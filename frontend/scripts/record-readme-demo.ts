/**
 * README demo: calm tour of existing workspace Views.
 *
 * - Shows Views menu + Panels menu (read-only — never creates views/panels)
 * - Switches: Standard → Templates → Workflows → Instances
 * - Each view: list + one detail
 * - Then osb-platform provision / deprovision on Instances
 *
 *   cd frontend && npm run demo:gif
 */
import { createRequire } from "node:module";
import { mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { PNG } from "pngjs";
import { chromium, type Page } from "playwright";

const require = createRequire(import.meta.url);
const { default: GIFEncoder, applyPalette, quantize } = require("gifenc") as {
  default: () => ReturnType<typeof import("gifenc").default>;
  applyPalette: (data: Uint8Array, palette: number[][]) => Uint8Array;
  quantize: (data: Uint8Array, maxColors: number) => number[][];
};

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(frontendRoot, "..");
const outputGif = path.join(repoRoot, "docs", "demo.gif");
const framesDir = path.join(frontendRoot, "scripts", ".demo-frames");

const VIEWPORT = {
  width: Number(process.env.DEMO_WIDTH ?? 1600),
  height: Number(process.env.DEMO_HEIGHT ?? 960),
};
const BASE_URL = process.env.DEMO_BASE_URL ?? "http://localhost:8081";
const DEMO_USER = process.env.DEMO_USER ?? "alice";
const DEMO_PASSWORD = process.env.DEMO_PASSWORD ?? "alice";

const FRAME_DELAY_MS = 2600;
const PAUSE = {
  short: 1000,
  medium: 1800,
  long: 3000,
  settle: 4000,
} as const;

const PROVISION_TIMEOUT_MS = Number(process.env.DEMO_PROVISION_TIMEOUT_MS ?? 360_000);
const DEPROVISION_TIMEOUT_MS = Number(process.env.DEMO_DEPROVISION_TIMEOUT_MS ?? 180_000);

const VIEWS = ["Standard", "Templates", "Workflows", "Instances"] as const;

let frameCounter = 0;

async function pause(ms: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function snap(page: Page, label: string): Promise<void> {
  await pause(PAUSE.short);
  const framePath = path.join(framesDir, `${String(frameCounter++).padStart(3, "0")}-${label}.png`);
  await page.screenshot({ path: framePath, type: "png", animations: "disabled" });
  await pause(PAUSE.medium);
}

async function ensureEnglish(page: Page): Promise<void> {
  const langBtn = page.getByRole("button", { name: /UI language|UI-Sprache/i });
  if (!(await langBtn.isVisible().catch(() => false))) return;
  const flag = await langBtn.innerText().catch(() => "");
  if (flag.includes("🇬🇧")) return;
  await langBtn.click();
  await page.getByRole("menuitemradio", { name: /English/i }).click();
  await pause(PAUSE.medium);
}

async function login(page: Page): Promise<void> {
  await page.goto(BASE_URL, { waitUntil: "domcontentloaded" });
  await pause(PAUSE.medium);

  const username = page.locator("#username, input[name='username']").first();
  if (await username.isVisible().catch(() => false)) {
    await snap(page, "01-login");
    await username.fill(DEMO_USER);
    await page.locator("#password, input[name='password']").first().fill(DEMO_PASSWORD);
    await pause(PAUSE.short);
    await page.locator("#kc-login, button[type='submit']").first().click();
  }

  await page.waitForURL((url) => !url.pathname.includes("/realms/"), { timeout: 45_000 });
  await page.getByRole("button", { name: /^View:/i }).waitFor({ state: "visible", timeout: 45_000 });
  await pause(PAUSE.settle);
}

/* ---------- menus (show only, never create) ---------- */

async function closeViewMenu(page: Page): Promise<void> {
  if (await page.locator(".view-dropdown").isVisible().catch(() => false)) {
    await page.getByRole("button", { name: /^View:/i }).click({ force: true });
    await pause(PAUSE.short);
  }
}

async function showViewsMenu(page: Page, snapLabel: string): Promise<void> {
  await closeViewMenu(page);
  await closePanelsMenu(page);
  await page.getByRole("button", { name: /^View:/i }).click();
  await page.locator(".view-dropdown").waitFor({ state: "visible", timeout: 10_000 });
  await pause(PAUSE.long);
  await snap(page, snapLabel);
}

async function closePanelsMenu(page: Page): Promise<void> {
  for (let i = 0; i < 3; i++) {
    const open = await page.locator("header .dropdown-item").first().isVisible().catch(() => false);
    if (!open) return;
    // Panels dropdown shares dropdown-item class; only toggle if Panels menu is up.
    const panelsOpen = await page
      .getByRole("checkbox", { name: /^Instances$|^Catalogs$|^Workflows$/i })
      .first()
      .isVisible()
      .catch(() => false);
    if (!panelsOpen) return;
    await page.getByRole("button", { name: "Panels" }).click({ force: true });
    await pause(PAUSE.short);
  }
}

async function showPanelsMenu(page: Page, snapLabel: string): Promise<void> {
  await closeViewMenu(page);
  await closePanelsMenu(page);
  await page.getByRole("button", { name: "Panels" }).click();
  await page.getByRole("checkbox").first().waitFor({ state: "visible", timeout: 10_000 });
  await pause(PAUSE.long);
  await snap(page, snapLabel);
  // Close again — do not toggle any checkbox.
  await page.getByRole("button", { name: "Panels" }).click({ force: true });
  await pause(PAUSE.medium);
}

/**
 * Switch to an existing view. Menu labels are "○ Standard" / "● Instances".
 * Never creates a view.
 */
async function switchToExistingView(page: Page, viewName: string): Promise<void> {
  await closePanelsMenu(page);
  await closeViewMenu(page);
  await page.getByRole("button", { name: /^View:/i }).click();
  await page.locator(".view-dropdown").waitFor({ state: "visible", timeout: 10_000 });
  await pause(PAUSE.medium);

  const items = page.locator(".view-dropdown button.view-item").filter({
    hasText: new RegExp(`\\b${viewName}\\b`, "i"),
  });
  const count = await items.count();
  if (count === 0) {
    await closeViewMenu(page);
    throw new Error(
      `View "${viewName}" not found. Expected existing views: ${VIEWS.join(", ")}.`,
    );
  }
  // Prefer already-active entry if present among duplicates, else first.
  const active = items.filter({ hasText: "●" });
  if ((await active.count()) > 0) {
    await active.first().click();
  } else {
    await items.first().click();
  }
  await pause(PAUSE.settle);
}

async function focusTab(page: Page, name: RegExp): Promise<void> {
  await closeViewMenu(page);
  await closePanelsMenu(page);
  const tab = page.getByRole("tab", { name });
  if (await tab.isVisible().catch(() => false)) {
    await tab.click({ force: true });
    await pause(PAUSE.medium);
  }
}

async function selectInLabeledField(page: Page, label: string, optionPattern: RegExp): Promise<void> {
  const dialog = page.getByRole("dialog");
  const field = dialog.locator("label.form-field").filter({ hasText: new RegExp(`^\\s*${label}`, "i") }).first();
  const select = field.locator("select").first();
  await select.waitFor({ state: "visible", timeout: 20_000 });

  const deadline = Date.now() + 20_000;
  let value: string | null = null;
  while (Date.now() < deadline) {
    value = await select.locator("option").evaluateAll((options, patternSource) => {
      const re = new RegExp(patternSource);
      const match = options.find((opt) => re.test(opt.textContent ?? ""));
      return match ? (match as HTMLOptionElement).value : null;
    }, optionPattern.source);
    if (value) break;
    await pause(300);
  }
  if (!value) throw new Error(`No option matching ${optionPattern} in "${label}"`);
  await select.selectOption(value);
  await pause(PAUSE.long);
}

async function clickFirstMatching(page: Page, pattern: RegExp): Promise<boolean> {
  const target = page.getByText(pattern).first();
  if (!(await target.isVisible().catch(() => false))) return false;
  await target.click();
  await pause(PAUSE.settle);
  return true;
}

async function fillSearch(page: Page, query: string): Promise<void> {
  const search = page.getByRole("searchbox", { name: /Search/i });
  if (await search.isVisible().catch(() => false)) {
    await search.fill(query);
    await pause(PAUSE.long);
  }
}

/* ---------- view tours ---------- */

async function tourStandard(page: Page): Promise<void> {
  await switchToExistingView(page, "Standard");
  await snap(page, "04-standard");
  await focusTab(page, /^Catalogs$/i);
  await pause(PAUSE.long);
  await snap(page, "05-standard-list");
  if (await clickFirstMatching(page, /k8s-ops|Kubernetes Ops|default/i)) {
    await pause(PAUSE.long);
    await snap(page, "06-standard-detail");
  }
}

async function tourTemplates(page: Page): Promise<void> {
  await switchToExistingView(page, "Templates");
  await snap(page, "07-templates");
  await focusTab(page, /^Templates$/i);
  await fillSearch(page, "osb");
  await snap(page, "08-templates-list");
  const row = page
    .locator("tr, li, .collection-card")
    .filter({ hasText: /tpl-osb|OSB Platform/i })
    .first();
  if (await row.isVisible().catch(() => false)) {
    await row.click();
    await pause(PAUSE.settle);
    await focusTab(page, /^Template$/i);
    await pause(PAUSE.long);
    await snap(page, "09-template-detail");
  }
}

async function selectWorkflowRow(page: Page, pattern: RegExp): Promise<boolean> {
  await focusTab(page, /^Workflows$/i);
  const row = page.locator("tr, li, .collection-card").filter({ hasText: pattern }).first();
  if (!(await row.isVisible().catch(() => false))) {
    return false;
  }
  await row.click();
  await pause(PAUSE.medium);
  await focusTab(page, /^Workflow$/i);
  // Detail: n8n iframe and/or configuration block
  await Promise.race([
    page.locator('iframe[title="n8n"]').waitFor({ state: "attached", timeout: 25_000 }),
    page.getByText(/Configuration|n8n webhook|PROVISION|DEPROVISION/i).first().waitFor({
      state: "visible",
      timeout: 25_000,
    }),
  ]).catch(() => undefined);
  await pause(PAUSE.settle);
  return true;
}

async function tourWorkflows(page: Page): Promise<void> {
  await switchToExistingView(page, "Workflows");
  await snap(page, "10-workflows");
  await focusTab(page, /^Workflows$/i);
  await fillSearch(page, "osb");
  await pause(PAUSE.long);
  await snap(page, "11-workflows-list");

  for (const [label, pattern] of [
    ["12-workflow-provision", /OSB Platform Provision/i],
    ["13-workflow-last-op", /OSB Platform Instance Last Op|Instance Last Op/i],
    ["14-workflow-deprovision", /OSB Platform Deprovision/i],
  ] as const) {
    if (await selectWorkflowRow(page, pattern)) {
      await snap(page, label);
    } else {
      console.warn(`Workflow row not found for ${pattern}`);
    }
  }
}

async function tourInstances(page: Page, prefix: string): Promise<void> {
  await switchToExistingView(page, "Instances");
  await snap(page, `${prefix}-instances`);
  await focusTab(page, /^Instances$/i);
  await pause(PAUSE.long);
  await snap(page, `${prefix}-instances-list`);

  const row = page.getByText(/inst-/i).first();
  if (await row.isVisible().catch(() => false)) {
    await row.click();
    await pause(PAUSE.settle);
    await snap(page, `${prefix}-instance-detail`);
  }
}

async function waitForInstanceState(
  page: Page,
  instanceId: string,
  wanted: RegExp,
  timeoutMs: number,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const text = await page.locator("body").innerText().catch(() => "");
    if (text.includes(instanceId) && wanted.test(text)) return;
    await page.getByRole("button", { name: /Reload/i }).click().catch(() => undefined);
    await pause(2500);
  }
  throw new Error(`Timeout waiting for ${instanceId} → ${wanted}`);
}

async function recordProvisionAndDeprovision(page: Page): Promise<void> {
  await switchToExistingView(page, "Instances");
  await focusTab(page, /^Instances$/i);
  await snap(page, "17-instances-ready");

  await page.getByRole("button", { name: "New instance" }).click();
  await page.getByRole("dialog").waitFor({ state: "visible", timeout: 10_000 });
  await pause(PAUSE.long);
  await snap(page, "18-new-instance");

  await selectInLabeledField(page, "Catalog", /k8s-ops|Kubernetes Ops/i);
  await selectInLabeledField(page, "Service", /osb-platform/i);
  await selectInLabeledField(page, "Plan", /.+/);
  await pause(PAUSE.long);
  await snap(page, "19-osb-platform-form");

  // Dialog closes immediately; provision HTTP may still run — poll fast for "in progress".
  await page.getByRole("dialog").getByRole("button", { name: /^Create$/i }).click();
  await page.getByRole("dialog").waitFor({ state: "hidden", timeout: 15_000 }).catch(() => undefined);

  await focusTab(page, /^Instances$/i);
  let instanceId: string | undefined;
  let caughtInProgress = false;
  const appearDeadline = Date.now() + 180_000;
  while (Date.now() < appearDeadline) {
    await page.getByRole("button", { name: /Reload/i }).click().catch(() => undefined);
    await pause(800);
    const body = await page.locator("body").innerText().catch(() => "");
    const match = body.match(/inst-[a-f0-9]+/i);
    if (match) {
      instanceId = match[0];
      if (/in progress/i.test(body)) {
        await page.getByText(instanceId).first().click().catch(() => undefined);
        await pause(PAUSE.medium);
        await snap(page, "20-provision-in-progress");
        caughtInProgress = true;
        break;
      }
      // Instance visible but not yet labeled in progress — keep polling briefly.
      if (!caughtInProgress) {
        await page.getByText(instanceId).first().click().catch(() => undefined);
        await pause(PAUSE.short);
        await snap(page, "20-provision-started");
      }
      break;
    }
  }
  if (!instanceId) throw new Error("Could not read created instance id");

  if (!caughtInProgress) {
    try {
      await waitForInstanceState(page, instanceId, /In progress|in progress/i, 90_000);
      await page.getByText(instanceId).first().click().catch(() => undefined);
      await pause(PAUSE.medium);
      await snap(page, "21-provision-in-progress");
      caughtInProgress = true;
    } catch {
      console.warn("Could not catch in-progress state; continuing to succeeded.");
    }
  }

  await waitForInstanceState(page, instanceId, /Succeeded/i, PROVISION_TIMEOUT_MS);
  await page.getByText(instanceId).first().click().catch(() => undefined);
  await pause(PAUSE.settle);
  await snap(page, "22-provision-succeeded");

  await focusTab(page, /^Instances$/i);
  await pause(PAUSE.medium);
  await page
    .locator("tr, li, .collection-card")
    .filter({ hasText: instanceId })
    .getByRole("button", { name: /^Delete$/i })
    .first()
    .click();
  await page.getByRole("dialog").waitFor({ state: "visible", timeout: 10_000 });
  await pause(PAUSE.long);
  await snap(page, "23-deprovision-confirm");

  await page.getByRole("dialog").getByRole("button", { name: /^Delete$/i }).click();
  await pause(PAUSE.settle);
  await snap(page, "24-deprovision-started");

  const deadline = Date.now() + DEPROVISION_TIMEOUT_MS;
  while (Date.now() < deadline) {
    const stillThere = await page.getByText(instanceId).first().isVisible().catch(() => false);
    if (!stillThere) {
      await pause(PAUSE.settle);
      await snap(page, "25-deprovisioned");
      return;
    }
    await page.getByRole("button", { name: /Reload/i }).click().catch(() => undefined);
    await pause(2500);
  }
  throw new Error(`Timeout waiting for deprovision of ${instanceId}`);
}

async function runDemo(): Promise<string[]> {
  rmSync(framesDir, { recursive: true, force: true });
  mkdirSync(framesDir, { recursive: true });
  rmSync(outputGif, { force: true });
  frameCounter = 0;

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: VIEWPORT });

  try {
    await login(page);
    await ensureEnglish(page);
    await snap(page, "01b-workspace");

    // Show menus once (read-only).
    await showViewsMenu(page, "02-views-menu");
    await closeViewMenu(page);
    await showPanelsMenu(page, "03-panels-menu");

    // Existing views only — never create.
    await tourStandard(page);
    await tourTemplates(page);
    await tourWorkflows(page);
    await tourInstances(page, "15");

    try {
      await recordProvisionAndDeprovision(page);
    } catch (error) {
      console.warn("Provision/deprovision segment failed; keeping view-tour frames.", error);
      await snap(page, "99-lifecycle-error");
    }
  } finally {
    await browser.close();
  }

  return readdirSync(framesDir)
    .filter((name) => name.endsWith(".png"))
    .sort()
    .map((name) => path.join(framesDir, name));
}

function encodeGif(framePaths: string[]): void {
  const gif = GIFEncoder();
  framePaths.forEach((framePath, index) => {
    const png = PNG.sync.read(readFileSync(framePath));
    const palette = quantize(png.data, 128);
    const indices = applyPalette(png.data, palette);
    gif.writeFrame(indices, png.width, png.height, {
      palette,
      delay: FRAME_DELAY_MS,
      first: index === 0,
    });
  });
  gif.finish();
  writeFileSync(outputGif, Buffer.from(gif.bytes()));
}

async function main(): Promise<void> {
  console.log(`Recording calm OSB views demo against ${BASE_URL} …`);
  console.log("Menus: Views + Panels (show only). Views: Standard → Templates → Workflows → Instances");
  const frames = await runDemo();
  console.log(`${frames.length} frames → GIF (${FRAME_DELAY_MS}ms/frame) …`);
  mkdirSync(path.dirname(outputGif), { recursive: true });
  encodeGif(frames);
  rmSync(framesDir, { recursive: true, force: true });
  console.log(`Done: ${outputGif}`);
}

main().catch((error: unknown) => {
  console.error(error);
  process.exit(1);
});
