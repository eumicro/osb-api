'use strict';

function apiBaseUrl() {
  return (process.env.OSB_API_BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
}

function apiToken() {
  return process.env.OSB_N8N_CLIENT_TOKEN || 'osb-n8n-client-dev-secret';
}

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    'X-OSB-Workflow-Token': apiToken(),
  };
}

async function listTemplates(ctx, kind) {
  const query = kind ? `?kind=${encodeURIComponent(kind)}` : '';
  const items = await ctx.helpers.httpRequest({
    method: 'GET',
    url: `${apiBaseUrl()}/api/internal/templates${query}`,
    headers: authHeaders(),
    json: true,
  });
  return Array.isArray(items) ? items : [];
}

async function listClientInstances(ctx, clientType) {
  const items = await ctx.helpers.httpRequest({
    method: 'GET',
    url: `${apiBaseUrl()}/api/internal/workflow-clients/${clientType}/instances`,
    headers: authHeaders(),
    json: true,
  });
  return Array.isArray(items) ? items : [];
}

function pickOsbPayload(input) {
  return input.body && typeof input.body === 'object' && !Array.isArray(input.body)
    ? input.body
    : input;
}

function firstId(list) {
  return Array.isArray(list) && list.length > 0 ? list[0] : '';
}

function templateOptions(templates, emptyLabel) {
  const seen = new Set();
  const options = [{ name: emptyLabel, value: '' }];
  for (const template of templates) {
    if (!template || !template.id || seen.has(template.id)) continue;
    seen.add(template.id);
    options.push({
      name: `${template.name} (${template.id})`,
      value: template.id,
    });
  }
  return options;
}

function clientOptions(instances) {
  return [
    { name: '— from workflow —', value: '' },
    ...instances.map((instance) => ({
      name: `${instance.name} (${instance.id})`,
      value: instance.id,
    })),
  ];
}

/**
 * Invoke OSB client API. Context (command, operationId, …) comes from the webhook item.
 *
 * @param {object} ctx n8n node this
 * @param {string} clientType GIT | KUBERNETES | HTTP
 * @param {number} itemIndex
 * @param {{ action: string, clientId?: string, templateId?: string }} params
 */
async function invokeOsbClient(ctx, clientType, itemIndex, params) {
  const action = params.action;
  const input = ctx.getInputData()[itemIndex]?.json || {};
  const osbPayload = pickOsbPayload(input);
  const operationId = osbPayload.operationId || input.operationId || null;
  const command = osbPayload.command || input.command || {};
  const clients = osbPayload.clients || input.clients || [];
  const httpClientIds = osbPayload.httpClientIds || input.httpClientIds || [];
  const kubernetesClientIds =
    osbPayload.kubernetesClientIds || input.kubernetesClientIds || [];
  const gitClientIds = osbPayload.gitClientIds || input.gitClientIds || [];
  const templateIds = osbPayload.templateIds || input.templateIds || [];
  const templates = osbPayload.templates || input.templates || [];
  const kind = osbPayload.kind || input.kind || null;
  const workflowId = osbPayload.workflowId || input.workflowId || null;

  const invokePayload = {
    operationId,
    command,
    clientId: params.clientId || '',
  };
  if (params.templateId) {
    invokePayload.templateId = params.templateId;
  }
  if (params.namespace) {
    invokePayload.namespace = params.namespace;
  }
  if (command && typeof command === 'object') {
    for (const [key, value] of Object.entries(command)) {
      if (invokePayload[key] === undefined) {
        invokePayload[key] = value;
      }
    }
  }

  let response;
  let fallback = false;
  try {
    response = await ctx.helpers.httpRequest({
      method: 'POST',
      url: `${apiBaseUrl()}/api/internal/workflow-clients/${clientType}`,
      headers: authHeaders(),
      body: {
        action,
        payload: invokePayload,
        operationId,
      },
      json: true,
    });
  } catch (err) {
    // Opt-in only — default is fail-loud (Compose realtests must not silently noop).
    const allowFallback = process.env.OSB_CLIENT_ALLOW_LOCAL_FALLBACK === 'true';
    if (!allowFallback) {
      throw err;
    }
    fallback = true;
    console.warn(
      `[OSB Client] ${clientType}/${action} API unreachable (${apiBaseUrl()}): ${err.message}. Using local noop fallback.`,
    );
    response = {
      ok: true,
      client: clientType,
      action,
      message: `${clientType.toLowerCase()} ${action} accepted (local noop fallback)`,
      detailsJson: JSON.stringify(invokePayload),
    };
  }

  if (response && response.ok === false) {
    const detail = response.message || response.detailsJson || 'OSB client returned ok=false';
    throw new Error(`${clientType}/${action} failed: ${detail}`);
  }

  return {
    json: {
      body: osbPayload,
      operationId,
      command,
      clients,
      httpClientIds,
      kubernetesClientIds,
      gitClientIds,
      templateIds,
      templates,
      kind,
      workflowId,
      clientResult: response,
      fallback,
      request: { clientType, action, payload: invokePayload },
    },
  };
}

module.exports = {
  invokeOsbClient,
  listTemplates,
  listClientInstances,
  templateOptions,
  clientOptions,
  pickOsbPayload,
  firstId,
  apiBaseUrl,
  apiToken,
};
