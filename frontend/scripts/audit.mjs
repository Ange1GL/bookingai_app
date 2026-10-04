#!/usr/bin/env node
// Fails when npm audit reports vulnerabilities at or above the configured level.
import { spawnSync } from 'child_process'
import { dirname, resolve } from 'path'
import { fileURLToPath } from 'url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const AUDIT_LEVEL = process.env.AUDIT_LEVEL ?? 'high'
const NPM = process.platform === 'win32' ? 'npm.cmd' : 'npm'

const result = spawnSync(NPM, ['audit', `--audit-level=${AUDIT_LEVEL}`], {
  cwd: ROOT,
  stdio: 'inherit',
  shell: process.platform === 'win32',
})

process.exit(result.status ?? 1)
