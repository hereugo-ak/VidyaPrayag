#!/usr/bin/env node
/* eslint-disable no-console */
/**
 * ensure-deps.js — bulletproof dependency guard for the Enroll+ website.
 *
 * WHY THIS EXISTS
 * ---------------
 * `node_modules` is intentionally git-ignored, so after every fresh clone /
 * branch switch / pull the dependencies must be installed before `next dev`
 * or `next build` can run. Forgetting that single step produced the recurring
 *
 *     Module not found: Can't resolve 'recharts'
 *
 * crash on /admin/dashboard. This script runs automatically via the `predev`,
 * `prebuild` and `prestart` npm hooks and *guarantees* the dependencies that
 * the code actually imports are present — installing them automatically if a
 * single one is missing. No human has to remember `npm install` ever again.
 *
 * It is intentionally dependency-free (pure Node core) so it can run before
 * anything is installed.
 */

const { existsSync, readFileSync } = require("node:fs");
const { join, dirname } = require("node:path");
const { spawnSync } = require("node:child_process");

const ROOT = dirname(__dirname); // website/
const NODE_MODULES = join(ROOT, "node_modules");

function readPackageJson() {
  try {
    return JSON.parse(readFileSync(join(ROOT, "package.json"), "utf8"));
  } catch (err) {
    console.error("[ensure-deps] Could not read package.json:", err.message);
    process.exit(1);
  }
}

/** A module is "present" if its package.json resolves inside node_modules. */
function isInstalled(pkg) {
  // Scoped + sub-path safe: just look for the package's own package.json.
  return existsSync(join(NODE_MODULES, pkg, "package.json"));
}

/**
 * Choose the install command.
 *  - "full"  : node_modules is entirely missing -> reproducible install.
 *              Use `npm ci` when a lockfile exists, else `npm install`.
 *  - "patch" : node_modules exists but a few packages are missing ->
 *              additive `npm install` (fast, memory-light, won't wipe the
 *              tree). `npm ci` would delete & reinstall everything, which is
 *              slow and can be OOM-killed in constrained environments.
 */
function detectInstallCommand(mode) {
  const hasLock = existsSync(join(ROOT, "package-lock.json"));
  if (mode === "full" && hasLock) {
    return { cmd: "npm", args: ["ci"] };
  }
  return { cmd: "npm", args: ["install"] };
}

function install(reason, mode) {
  const { cmd, args } = detectInstallCommand(mode);
  console.log(`\n[ensure-deps] ${reason}`);
  console.log(`[ensure-deps] Running: ${cmd} ${args.join(" ")} (this happens automatically)\n`);
  // Use a single command string on Windows to avoid DEP0190 deprecation warning
  // (passing args with shell:true is deprecated because they are concatenated, not escaped).
  const isWin = process.platform === "win32";
  const res = isWin
    ? spawnSync(`${cmd} ${args.join(" ")}`, {
        cwd: ROOT,
        stdio: "inherit",
        shell: true,
      })
    : spawnSync(cmd, args, {
        cwd: ROOT,
        stdio: "inherit",
      });
  if (res.status !== 0) {
    console.error(
      "\n[ensure-deps] Dependency install FAILED. Run it manually:\n" +
        `    cd website && ${cmd} ${args.join(" ")}\n`
    );
    process.exit(res.status || 1);
  }
}

function main() {
  const pkg = readPackageJson();
  const declared = Object.keys(pkg.dependencies || {});

  // 1) No node_modules at all -> install everything.
  if (!existsSync(NODE_MODULES)) {
    install("node_modules/ is missing (fresh clone or after a pull).", "full");
  }

  // 2) node_modules exists but is stale/partial -> reinstall.
  //    We check EVERY production dependency, not just recharts, so a partial
  //    install can never silently slip through again.
  const missing = declared.filter((dep) => !isInstalled(dep));
  if (missing.length > 0) {
    install(
      `Missing dependencies detected: ${missing.join(", ")} ` +
        "(node_modules is stale/incomplete).",
      "patch"
    );

    // Final verification — fail loudly instead of letting Next.js crash later.
    const stillMissing = declared.filter((dep) => !isInstalled(dep));
    if (stillMissing.length > 0) {
      console.error(
        `\n[ensure-deps] Still missing after install: ${stillMissing.join(", ")}.\n` +
          "    Try: cd website && rm -rf node_modules package-lock.json && npm install\n"
      );
      process.exit(1);
    }
  }

  console.log("[ensure-deps] OK — all dependencies present.");
}

main();
