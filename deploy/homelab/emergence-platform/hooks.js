/**
 * n8n External Hooks for OIDC Authentication
 *
 * This file implements OIDC authentication support for n8n using only built-in Node.js modules.
 * It provides:
 * - OIDC discovery endpoint support
 * - Authorization code flow
 * - User provisioning (JIT - Just In Time)
 * - Frontend customization to show OIDC login button
 *
 * Environment Variables Required:
 * - OIDC_ISSUER_URL: The OIDC provider's issuer URL (e.g., https://auth.example.com)
 * - OIDC_CLIENT_ID: OAuth2 client ID
 * - OIDC_CLIENT_SECRET: OAuth2 client secret
 * - OIDC_REDIRECT_URI: The callback URL (e.g., https://n8n.example.com/auth/oidc/callback)
 *
 * Optional:
 * - OIDC_SCOPES: Space-separated list of scopes (default: "openid email profile")
 * - OIDC_BROWSER_ORIGIN: Public IdP origin for browser redirects (e.g. http://localhost:8180)
 *   when OIDC_ISSUER_URL is an internal Docker hostname (e.g. http://keycloak:8080/realms/osb)
 */

const https = require('https');
const http = require('http');
const crypto = require('crypto');
const { URL, URLSearchParams } = require('url');

// Configuration from environment
const config = {
  issuerUrl: process.env.OIDC_ISSUER_URL,
  clientId: process.env.OIDC_CLIENT_ID,
  clientSecret: process.env.OIDC_CLIENT_SECRET,
  redirectUri: process.env.OIDC_REDIRECT_URI,
  scopes: process.env.OIDC_SCOPES || 'openid email profile',
  browserOrigin: process.env.OIDC_BROWSER_ORIGIN || '',
  bridgeSecret: process.env.OSB_N8N_BRIDGE_SECRET || '',
};

/**
 * Verify HMAC ticket issued by OSB BFF (session propagation into iframe).
 * Format: base64url(email|first|last|exp).base64url(hmacSha256)
 */
function verifyBridgeTicket(ticket, secret) {
  if (!ticket || !secret || !ticket.includes('.')) {
    throw new Error('invalid bridge ticket');
  }
  const [bodyB64, signature] = ticket.split('.');
  const expected = crypto
    .createHmac('sha256', secret)
    .update(bodyB64)
    .digest('base64url');
  const a = Buffer.from(expected);
  const b = Buffer.from(signature);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
    throw new Error('invalid bridge ticket signature');
  }
  const body = Buffer.from(bodyB64, 'base64url').toString('utf8');
  const parts = body.split('|');
  if (parts.length !== 4) {
    throw new Error('invalid bridge ticket payload');
  }
  const exp = Number(parts[3]);
  if (!Number.isFinite(exp) || exp < Math.floor(Date.now() / 1000)) {
    throw new Error('bridge ticket expired');
  }
  if (!parts[0] || !parts[0].includes('@')) {
    throw new Error('invalid email in bridge ticket');
  }
  return {
    email: parts[0],
    given_name: parts[1] || 'OSB',
    family_name: parts[2] || 'User',
  };
}

/**
 * Safe post-login redirect path (same-origin relative path only).
 */
function sanitizeReturnTo(value) {
  if (!value || typeof value !== 'string') {
    return '/';
  }
  const trimmed = value.trim();
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.includes('://')) {
    return '/';
  }
  return trimmed;
}

/**
 * Build authorization URL; rewrite host for browser when IdP is reached via Docker DNS.
 */
function buildAuthorizationUrl(discovery) {
  const authUrl = new URL(discovery.authorization_endpoint);
  if (config.browserOrigin) {
    const browser = new URL(config.browserOrigin);
    authUrl.protocol = browser.protocol;
    authUrl.host = browser.host;
  }
  return authUrl;
}

// Validate configuration
function validateConfig() {
  const missing = [];
  if (!config.issuerUrl) missing.push('OIDC_ISSUER_URL');
  if (!config.clientId) missing.push('OIDC_CLIENT_ID');
  if (!config.clientSecret) missing.push('OIDC_CLIENT_SECRET');
  if (!config.redirectUri) missing.push('OIDC_REDIRECT_URI');
  return missing;
}

// Cache for OIDC discovery document
let discoveryCache = null;
let discoveryCacheTime = 0;
const DISCOVERY_CACHE_TTL = 3600000; // 1 hour

/**
 * Make an HTTP/HTTPS request
 * @param {string} url - The URL to request
 * @param {object} options - Request options
 * @returns {Promise<{statusCode: number, headers: object, body: string}>}
 */
function makeRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url);
    const protocol = parsedUrl.protocol === 'https:' ? https : http;

    const reqOptions = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80),
      path: parsedUrl.pathname + parsedUrl.search,
      method: options.method || 'GET',
      headers: options.headers || {},
    };

    const req = protocol.request(reqOptions, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body,
        });
      });
    });

    req.on('error', reject);

    if (options.body) {
      req.write(options.body);
    }

    req.end();
  });
}

/**
 * Fetch OIDC discovery document
 * @returns {Promise<object>}
 */
async function fetchDiscoveryDocument() {
  const now = Date.now();
  if (discoveryCache && now - discoveryCacheTime < DISCOVERY_CACHE_TTL) {
    return discoveryCache;
  }

  const discoveryUrl = config.issuerUrl.replace(/\/$/, '') + '/.well-known/openid-configuration';
  const response = await makeRequest(discoveryUrl);

  if (response.statusCode !== 200) {
    throw new Error(`Failed to fetch OIDC discovery document: ${response.statusCode}`);
  }

  discoveryCache = JSON.parse(response.body);
  discoveryCacheTime = now;
  return discoveryCache;
}

/**
 * Generate a random string for state/nonce
 * @param {number} length
 * @returns {string}
 */
function generateRandomString(length = 32) {
  return crypto.randomBytes(length).toString('hex');
}

/**
 * Base64URL encode
 * @param {Buffer|string} input
 * @returns {string}
 */
function base64UrlEncode(input) {
  const base64 = Buffer.isBuffer(input) ? input.toString('base64') : Buffer.from(input).toString('base64');
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

/**
 * Base64URL decode
 * @param {string} input
 * @returns {Buffer}
 */
function base64UrlDecode(input) {
  let base64 = input.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4) {
    base64 += '=';
  }
  return Buffer.from(base64, 'base64');
}

/**
 * Decode JWT without verification (for extracting claims)
 * @param {string} token
 * @returns {object}
 */
function decodeJwt(token) {
  const parts = token.split('.');
  if (parts.length !== 3) {
    throw new Error('Invalid JWT format');
  }

  const payload = JSON.parse(base64UrlDecode(parts[1]).toString('utf8'));
  return payload;
}

/**
 * Exchange authorization code for tokens
 * @param {string} code
 * @param {object} discovery
 * @returns {Promise<object>}
 */
async function exchangeCodeForTokens(code, discovery) {
  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: config.redirectUri,
    client_id: config.clientId,
    client_secret: config.clientSecret,
  });

  const response = await makeRequest(discovery.token_endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
  });

  if (response.statusCode !== 200) {
    console.error('Token exchange failed:', response.body);
    throw new Error(`Token exchange failed: ${response.statusCode}`);
  }

  return JSON.parse(response.body);
}

/**
 * Fetch user info from OIDC provider
 * @param {string} accessToken
 * @param {object} discovery
 * @returns {Promise<object>}
 */
async function fetchUserInfo(accessToken, discovery) {
  const response = await makeRequest(discovery.userinfo_endpoint, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (response.statusCode !== 200) {
    console.error('UserInfo fetch failed:', response.body);
    throw new Error(`UserInfo fetch failed: ${response.statusCode}`);
  }

  return JSON.parse(response.body);
}

/**
 * Create a signed JWT for state/nonce storage
 * We use the JWT service from n8n when available, but for cookies we just use HMAC
 * @param {object} payload
 * @param {string} secret
 * @param {number} expiresInSeconds
 * @returns {string}
 */
function createSignedCookie(payload, secret, expiresInSeconds = 900) {
  const exp = Math.floor(Date.now() / 1000) + expiresInSeconds;
  const data = JSON.stringify({ ...payload, exp });
  const hmac = crypto.createHmac('sha256', secret);
  hmac.update(data);
  const signature = hmac.digest('hex');
  return base64UrlEncode(data) + '.' + signature;
}

/**
 * Verify and decode a signed cookie
 * @param {string} cookie
 * @param {string} secret
 * @returns {object|null}
 */
function verifySignedCookie(cookie, secret) {
  try {
    const [dataB64, signature] = cookie.split('.');
    const data = base64UrlDecode(dataB64).toString('utf8');

    const hmac = crypto.createHmac('sha256', secret);
    hmac.update(data);
    const expectedSignature = hmac.digest('hex');

    if (signature !== expectedSignature) {
      return null;
    }

    const payload = JSON.parse(data);
    if (payload.exp && payload.exp < Date.now() / 1000) {
      return null;
    }

    return payload;
  } catch {
    return null;
  }
}

/**
 * Get or create the cookie signing secret
 * We derive it from the n8n encryption key if available
 * @param {object} context
 * @returns {string}
 */
function getCookieSecret(context) {
  // Use a combination of environment variables to create a stable secret
  const baseKey = process.env.N8N_ENCRYPTION_KEY || process.env.OIDC_CLIENT_SECRET || 'n8n-oidc-hook-secret';
  const hash = crypto.createHash('sha256').update(baseKey + '-oidc-state').digest('hex');
  return hash;
}

/**
 * Create the n8n auth cookie using n8n's JwtService
 * @param {object} user
 * @param {object} jwtService - n8n's JwtService instance
 * @returns {string}
 */
function createAuthToken(user, jwtService) {
  // n8n's JWT contains: { id, hash, browserId?, usedMfa? }
  const payload = {
    id: user.id,
    hash: createUserHash(user),
    usedMfa: false,
  };

  return jwtService.sign(payload, { expiresIn: '7d' });
}

/**
 * Create user hash for JWT (mimics n8n's AuthService.createJWTHash)
 * @param {object} user
 * @returns {string}
 */
function createUserHash(user) {
  const payload = [user.email, user.password || ''];
  if (user.mfaEnabled && user.mfaSecret) {
    payload.push(user.mfaSecret.substring(0, 3));
  }
  return crypto.createHash('sha256').update(payload.join(':')).digest('base64').substring(0, 10);
}

/**
 * Check if email is valid
 * @param {string} email
 * @returns {boolean}
 */
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

// n8n module paths (specific to the Docker image)
const N8N_DI_PATH = '/usr/local/lib/node_modules/n8n/node_modules/@n8n/di';
const N8N_JWT_SERVICE_PATH = '/usr/local/lib/node_modules/n8n/dist/services/jwt.service.js';
const N8N_AUTH_SERVICE_PATH = '/usr/local/lib/node_modules/n8n/dist/auth/auth.service.js';

// Export the hooks
module.exports = {
  n8n: {
    /**
     * Called when n8n is ready
     * We use this to register custom routes for OIDC
     */
    ready: [
      async function (server, n8nConfig) {
        const missing = validateConfig();
        const bridgeEnabled = !!config.bridgeSecret;
        if (missing.length > 0 && !bridgeEnabled) {
          console.warn(`[OIDC Hook] Missing configuration: ${missing.join(', ')}. OIDC disabled.`);
          return;
        }

        console.log('[OIDC Hook] Initializing authentication hooks...');

        // Get n8n services from the DI container
        const { Container } = require(N8N_DI_PATH);
        const { JwtService } = require(N8N_JWT_SERVICE_PATH);
        const { AuthService } = require(N8N_AUTH_SERVICE_PATH);
        const jwtService = Container.get(JwtService);
        const authService = Container.get(AuthService);

        const { app } = server;
        const cookieSecret = getCookieSecret();

        // Cookie settings
        const cookieOptions = {
          httpOnly: true,
          secure: process.env.N8N_PROTOCOL === 'https',
          sameSite: 'lax',
          maxAge: 15 * 60 * 1000, // 15 minutes
        };

        async function loadUserWithSecrets(User, email) {
          // password/mfaSecret are select:false — required for a valid n8n-auth JWT hash
          return User.createQueryBuilder('user')
            .addSelect('user.password')
            .addSelect('user.mfaSecret')
            .leftJoinAndSelect('user.role', 'role')
            .where('user.email = :email', { email })
            .getOne();
        }

        async function markOwnerSetupComplete(Settings) {
          try {
            await Settings.update(
              { key: 'userManagement.isInstanceOwnerSetUp' },
              { value: 'true' },
            );
          } catch (e) {
            console.warn('[OIDC Hook] Failed to mark owner setup complete:', e.message);
          }
        }

        async function establishSession(res, userInfo) {
          const { User, Settings } = this.dbCollections;
          const firstName = userInfo.given_name || userInfo.name?.split(' ')[0] || 'User';
          const lastName = userInfo.family_name || userInfo.name?.split(' ').slice(1).join(' ') || '';

          let user = await loadUserWithSecrets(User, userInfo.email);

          if (!user) {
            // n8n may ship a placeholder global:owner with null email until setup completes.
            let placeholderOwner = null;
            try {
              placeholderOwner = await User.createQueryBuilder('user')
                .addSelect('user.password')
                .where('user.email IS NULL')
                .andWhere('user.roleSlug = :slug', { slug: 'global:owner' })
                .getOne();
            } catch {
              placeholderOwner = null;
            }

            if (placeholderOwner) {
              placeholderOwner.email = userInfo.email;
              placeholderOwner.firstName = firstName;
              placeholderOwner.lastName = lastName;
              if (!placeholderOwner.password) {
                placeholderOwner.password = crypto.randomBytes(32).toString('hex');
              }
              await User.save(placeholderOwner);
              user = await loadUserWithSecrets(User, userInfo.email);
              console.log(`[OIDC Hook] Claimed placeholder owner for: ${userInfo.email}`);
            } else {
              const userCount = await User.count();
              const userData = {
                email: userInfo.email,
                firstName,
                lastName,
                password: crypto.randomBytes(32).toString('hex'),
                role: { slug: userCount === 0 ? 'global:owner' : 'global:member' },
              };
              await User.createUserWithProject(userData);
              user = await loadUserWithSecrets(User, userInfo.email);
              console.log(`[OIDC Hook] Created ${userCount === 0 ? 'owner' : 'member'} user: ${userInfo.email}`);
            }
          }

          if (!user) {
            throw new Error('Failed to create or find user');
          }

          // Without this flag the UI stays on /setup even with a valid n8n-auth cookie.
          await markOwnerSetupComplete(Settings);

          // Prefer n8n AuthService so JWT hash matches validateToken().
          try {
            authService.issueCookie(res, user, false);
          } catch (e) {
            console.warn('[OIDC Hook] AuthService.issueCookie failed, falling back:', e.message);
            const authToken = createAuthToken(user, jwtService);
            res.cookie('n8n-auth', authToken, {
              httpOnly: true,
              secure: process.env.N8N_PROTOCOL === 'https',
              sameSite: 'lax',
              maxAge: 7 * 24 * 60 * 60 * 1000,
            });
          }
          return user;
        }

        /**
         * BFF session bridge: redeem short-lived ticket → n8n auth cookie → workflow deep-link.
         * Avoids Keycloak login inside a third-party iframe.
         */
        app.get('/auth/oidc/bridge', async (req, res) => {
          try {
            if (!config.bridgeSecret) {
              return res.status(503).send('Bridge not configured');
            }
            const userInfo = verifyBridgeTicket(req.query.ticket, config.bridgeSecret);
            await establishSession.call(this, res, userInfo);
            res.redirect(sanitizeReturnTo(req.query.returnTo));
          } catch (error) {
            console.error('[OIDC Hook] Bridge error:', error);
            res.redirect('/signin?error=' + encodeURIComponent('Bridge auth failed: ' + error.message));
          }
        });

        if (missing.length > 0) {
          console.warn(`[OIDC Hook] OIDC IdP login disabled (missing ${missing.join(', ')}); bridge enabled.`);
          console.log('[OIDC Hook] Routes registered: GET /auth/oidc/bridge');
          return;
        }

        /**
         * OIDC Login endpoint - redirects to the OIDC provider
         */
  app.get('/auth/oidc/login', async (req, res) => {
    try {
      const discovery = await fetchDiscoveryDocument();

      const state = generateRandomString();
      const nonce = generateRandomString();
      const returnTo = sanitizeReturnTo(req.query.returnTo);

      // Store state, nonce and post-login target in signed cookies
      const stateCookie = createSignedCookie({ state, returnTo }, cookieSecret);
      const nonceCookie = createSignedCookie({ nonce }, cookieSecret);

      res.cookie('n8n-oidc-state', stateCookie, cookieOptions);
      res.cookie('n8n-oidc-nonce', nonceCookie, cookieOptions);

      // Build authorization URL (browser-facing IdP origin when configured)
      const authUrl = buildAuthorizationUrl(discovery);
      authUrl.searchParams.set('client_id', config.clientId);
      authUrl.searchParams.set('redirect_uri', config.redirectUri);
      authUrl.searchParams.set('response_type', 'code');
      authUrl.searchParams.set('scope', config.scopes);
      authUrl.searchParams.set('state', state);
      authUrl.searchParams.set('nonce', nonce);

      res.redirect(authUrl.toString());
    } catch (error) {
      console.error('[OIDC Hook] Login error:', error);
      res.status(500).send('OIDC configuration error. Please check the logs.');
    }
  });

        /**
         * OIDC Callback endpoint - handles the authorization code
         */
        app.get('/auth/oidc/callback', async (req, res) => {
          try {
            const { code, state, error, error_description } = req.query;

            // Handle OIDC errors
            if (error) {
              console.error('[OIDC Hook] OIDC error:', error, error_description);
              return res.redirect('/signin?error=' + encodeURIComponent(error_description || error));
            }

            if (!code || !state) {
              return res.redirect('/signin?error=' + encodeURIComponent('Missing authorization code or state'));
            }

            // Verify state
            const stateCookie = req.cookies['n8n-oidc-state'];
            const nonceCookie = req.cookies['n8n-oidc-nonce'];

            if (!stateCookie || !nonceCookie) {
              return res.redirect('/signin?error=' + encodeURIComponent('Missing state cookies - session expired'));
            }

            const statePayload = verifySignedCookie(stateCookie, cookieSecret);
            const noncePayload = verifySignedCookie(nonceCookie, cookieSecret);

            if (!statePayload || statePayload.state !== state) {
              return res.redirect('/signin?error=' + encodeURIComponent('Invalid state - possible CSRF attack'));
            }

            // Clear state cookies
            res.clearCookie('n8n-oidc-state');
            res.clearCookie('n8n-oidc-nonce');

            // Exchange code for tokens
            const discovery = await fetchDiscoveryDocument();
            const tokens = await exchangeCodeForTokens(code, discovery);

            // Verify nonce in ID token if present
            if (tokens.id_token) {
              const idTokenClaims = decodeJwt(tokens.id_token);
              if (noncePayload && idTokenClaims.nonce !== noncePayload.nonce) {
                return res.redirect('/signin?error=' + encodeURIComponent('Invalid nonce - possible replay attack'));
              }
            }

            // Get user info
            let userInfo;
            try {
              userInfo = await fetchUserInfo(tokens.access_token, discovery);
            } catch (e) {
              // Fall back to ID token claims if userinfo endpoint fails
              if (tokens.id_token) {
                userInfo = decodeJwt(tokens.id_token);
              } else {
                throw e;
              }
            }

            // Validate we have an email
            if (!userInfo.email || !isValidEmail(userInfo.email)) {
              return res.redirect('/signin?error=' + encodeURIComponent('No valid email in OIDC response'));
            }

            await establishSession.call(this, res, userInfo);

            // Redirect to requested workflow/editor path (or home)
            const returnTo = sanitizeReturnTo(statePayload.returnTo);
            res.redirect(returnTo);
          } catch (error) {
            console.error('[OIDC Hook] Callback error:', error);
            res.redirect('/signin?error=' + encodeURIComponent('Authentication failed: ' + error.message));
          }
        });

        /**
         * Serve the frontend customization script
         * This script will replace the login form with an OIDC button
         *
         * NOTE: The route must be under /assets/ or another non-UI route prefix
         * to avoid being intercepted by n8n's history API handler which would
         * serve index.html instead of our JavaScript file.
         */
        app.get('/assets/oidc-frontend-hook.js', (req, res) => {
          // Use res.type() for proper MIME type handling with nosniff
          res.type('text/javascript; charset=utf-8');
          res.set('Cache-Control', 'public, max-age=3600');
          res.send(getFrontendScript());
        });

        console.log('[OIDC Hook] OIDC routes registered:');
        console.log('  - GET /auth/oidc/bridge');
        console.log('  - GET /auth/oidc/login');
        console.log('  - GET /auth/oidc/callback');
        console.log('  - GET /assets/oidc-frontend-hook.js');
      },
    ],
  },

  frontend: {
    /**
     * Modify frontend settings to configure SSO display
     */
    settings: [
      async function (frontendSettings) {
        const missing = validateConfig();
        if (missing.length > 0) {
          return; // OIDC not configured, don't modify settings
        }

        // Enable OIDC login button by setting these properties
        // This tells the frontend that OIDC is available
        frontendSettings.sso = frontendSettings.sso || {};
        frontendSettings.sso.oidc = {
          loginEnabled: true,
          loginUrl: '/auth/oidc/login',
          callbackUrl: config.redirectUri,
        };

        // Set authentication method to OIDC so the frontend knows SSO is primary
        frontendSettings.userManagement = frontendSettings.userManagement || {};
        frontendSettings.userManagement.authenticationMethod = 'oidc';

        // Enable enterprise OIDC feature flag so the SSO button shows
        frontendSettings.enterprise = frontendSettings.enterprise || {};
        frontendSettings.enterprise.oidc = true;

        console.log('[OIDC Hook] Frontend settings configured for OIDC');
      },
    ],
  },
};

/**
 * Generate the frontend customization script
 * This script runs in the browser and customizes the login page
 */
function getFrontendScript() {
  return `
/**
 * n8n OIDC Frontend Customization
 *
 * This script surgically modifies the login form to show an SSO button.
 * To access the normal login form, add ?showLogin=true to the URL.
 */
(function() {
	'use strict';

	function shouldShowNormalLogin() {
		return new URLSearchParams(window.location.search).get('showLogin') === 'true';
	}

	function isSigninPage() {
		return window.location.pathname === '/signin' || window.location.pathname === '/login';
	}

	function displayError(form) {
		var error = new URLSearchParams(window.location.search).get('error');
		if (!error || !form || form.querySelector('#oidc-error')) return;

		var errorDiv = document.createElement('div');
		errorDiv.id = 'oidc-error';
		errorDiv.style.cssText = 'background: var(--color-danger-tint-1, #fee); border: 1px solid var(--color-danger, #fcc); color: var(--color-danger, #c00); padding: 12px; border-radius: 4px; margin: 16px 0;';
		errorDiv.textContent = decodeURIComponent(error);

		var heading = form.querySelector('div[class*="_heading_"]');
		if (heading) heading.after(errorDiv);
		else form.prepend(errorDiv);
	}

	function injectSsoButton() {
		if (shouldShowNormalLogin()) return;
		if (!isSigninPage()) return;

		var form = document.querySelector('[data-test-id="auth-form"]');
		if (!form || form.querySelector('#oidc-sso-button')) return;

		// Find existing button to clone its classes
		var existingButton = form.querySelector('[data-test-id="form-submit-button"]');
		var buttonClasses = existingButton ? existingButton.className : '';

		// Hide the form elements (inputs, buttons, forgot password)
		form.querySelectorAll('div[class*="_inputsContainer_"], div[class*="_buttonsContainer_"], div[class*="_actionContainer_"]')
			.forEach(function(el) { el.style.display = 'none'; });

		// Create SSO button container
		var ssoContainer = document.createElement('div');
		ssoContainer.id = 'oidc-sso-container';
		ssoContainer.style.cssText = 'text-align: center;';

		// Create button - use cloned classes or fallback styles
		var button = document.createElement('button');
		button.id = 'oidc-sso-button';
		button.type = 'button';
		button.textContent = 'Sign in with SSO';
  button.onclick = function() {
    var returnTo = new URLSearchParams(window.location.search).get('redirect')
      || sessionStorage.getItem('osb-n8n-returnTo')
      || '/';
    window.location.href = '/auth/oidc/login?returnTo=' + encodeURIComponent(returnTo);
  };

		if (buttonClasses) {
			button.className = buttonClasses;
			button.style.width = '100%';
		} else {
			button.style.cssText = 'width: 100%; padding: 12px 24px; font-size: 14px; font-weight: 600; color: white; background: var(--color-primary, #ea4b30); border: none; border-radius: 4px; cursor: pointer;';
		}

		// Create admin link
		var adminLink = document.createElement('p');
		adminLink.style.cssText = 'margin-top: 16px; font-size: 12px; color: var(--color-text-light, #666);';
		adminLink.innerHTML = 'Admin? <a href="?showLogin=true" style="color: var(--color-primary, #ea4b30);">Sign in with email</a>';

		ssoContainer.appendChild(button);
		ssoContainer.appendChild(adminLink);

		// Insert after the heading
		var heading = form.querySelector('div[class*="_heading_"]');
		if (heading) heading.after(ssoContainer);
		else form.prepend(ssoContainer);

		displayError(form);
	}

	function observeAndInject() {
		if (shouldShowNormalLogin() || !isSigninPage()) return;

		injectSsoButton();

		var observer = new MutationObserver(function() {
			if (isSigninPage() && !shouldShowNormalLogin()) {
				var form = document.querySelector('[data-test-id="auth-form"]');
				if (form && !form.querySelector('#oidc-sso-button')) {
					injectSsoButton();
				}
			}
		});

		observer.observe(document.body, { childList: true, subtree: true });
		setTimeout(function() { observer.disconnect(); }, 10000);
	}

	function handleNavigation() {
		var origPush = history.pushState;
		var origReplace = history.replaceState;

		history.pushState = function() {
			origPush.apply(this, arguments);
			setTimeout(observeAndInject, 100);
		};

		history.replaceState = function() {
			origReplace.apply(this, arguments);
			setTimeout(observeAndInject, 100);
		};

		window.addEventListener('popstate', function() {
			setTimeout(observeAndInject, 100);
		});
	}

  function autoStartSsoIfRequested() {
    if (!isSigninPage() || shouldShowNormalLogin()) return;
    var params = new URLSearchParams(window.location.search);
    var returnTo = params.get('redirect') || params.get('returnTo');
    if (returnTo) {
      sessionStorage.setItem('osb-n8n-returnTo', returnTo);
    }
    if (params.get('autoSso') === 'true' || returnTo) {
      var target = returnTo || sessionStorage.getItem('osb-n8n-returnTo') || '/';
      window.location.replace('/auth/oidc/login?returnTo=' + encodeURIComponent(target));
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      autoStartSsoIfRequested();
      observeAndInject();
      handleNavigation();
    });
  } else {
    autoStartSsoIfRequested();
    observeAndInject();
    handleNavigation();
  }

  setTimeout(observeAndInject, 500);
  setTimeout(observeAndInject, 1000);

  console.log('[OIDC Hook] Frontend customization loaded');
})();
`;
}
