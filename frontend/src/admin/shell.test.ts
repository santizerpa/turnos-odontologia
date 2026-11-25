import { test } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

test('admin shell loads /js/admin/main.js and the public shell loads /js/main.js', () => {
  const source = readFileSync('admin.html', 'utf8');
  const built = readFileSync('dist/admin.html', 'utf8');

  assert.match(source, /src="\/js\/admin\/main\.js"/);
  assert.equal(source.includes('src="/main.js"'), false);
  assert.match(built, /src="\/js\/admin\/main\.js"/);
  assert.equal(built.includes('src="/main.js"'), false);

  const publicShell = readFileSync('dist/index.html', 'utf8');

  assert.match(publicShell, /src="\/js\/main\.js"/);
  assert.equal(existsSync('dist/js/main.js'), true);
});

test('admin shell exposes a mount point and never references the public entry point', () => {
  const admin = readFileSync('admin.html', 'utf8');
  const publicShell = readFileSync('index.html', 'utf8');

  assert.match(admin, /id="app"/);
  assert.equal(admin.includes('src="/main.js"'), false);
  assert.match(publicShell, /src="\/js\/main\.js"/);
  assert.equal(publicShell.includes('/js/admin/main.js'), false);
});
