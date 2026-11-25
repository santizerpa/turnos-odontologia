import { test } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

function readJson(path: string): Record<string, any> {
  return JSON.parse(readFileSync(path, 'utf8'));
}

test('build emits the public bundle under /js and the admin bundle under /js/admin', () => {
  const pkg = readJson('package.json');
  const publicConfig = readJson('tsconfig.json');
  const adminConfig = readJson('tsconfig.admin.json');

  assert.match(pkg.scripts.build, /tsc -p tsconfig\.admin\.json/);
  assert.match(pkg.scripts.test, /node --test/);
  assert.equal(publicConfig.compilerOptions.outDir, 'dist/js');
  assert.equal(adminConfig.compilerOptions.rootDir, 'src/admin');
  assert.equal(adminConfig.compilerOptions.outDir, 'dist/js/admin');
  assert.equal(existsSync('dist/js/admin/main.js'), true);
  assert.equal(existsSync('dist/js/main.js'), true);
});

test('test script compiles both projects and keeps the public test suite in the run', () => {
  const pkg = readJson('package.json');

  assert.match(pkg.scripts.test, /tsc -p tsconfig\.json/);
  assert.match(pkg.scripts.test, /tsc -p tsconfig\.admin\.json/);
  assert.match(pkg.scripts.test, /node --test/);
  assert.equal(existsSync('dist/js/public-reservation/errors.test.js'), true);
});
