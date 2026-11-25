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

  assert.match(pkg.scripts.test, /npm run build/);
  assert.match(pkg.scripts.test, /node --test/);
  assert.equal(pkg.engines.node, '>=22');
  assert.equal(existsSync('dist/js/public-reservation/errors.test.js'), true);
});

const STYLESHEET = '<link rel="stylesheet" href="/css/styles.css">';
const VIEWPORT = '<meta name="viewport" content="width=device-width, initial-scale=1">';

test('public and admin entries declare the viewport and link the local stylesheet', () => {
  for (const page of ['index.html', 'admin.html']) {
    const html = readFileSync(page, 'utf8');

    assert.equal(html.includes(STYLESHEET), true, `${page} links /css/styles.css`);
    assert.equal(html.includes(VIEWPORT), true, `${page} declares the viewport`);
  }
});

test('build publishes frontend/css into dist/css and compiled entries keep their links', () => {
  assert.equal(existsSync('css/styles.css'), true);
  assert.equal(existsSync('dist/css/styles.css'), true);
  assert.equal(readFileSync('dist/css/styles.css', 'utf8'), readFileSync('css/styles.css', 'utf8'));

  for (const page of ['dist/index.html', 'dist/admin.html']) {
    const html = readFileSync(page, 'utf8');

    assert.equal(html.includes(STYLESHEET), true, `${page} links /css/styles.css`);
    assert.equal(html.includes(VIEWPORT), true, `${page} declares the viewport`);
  }
});
