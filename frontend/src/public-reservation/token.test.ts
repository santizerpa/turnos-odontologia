import { test } from 'node:test';
import assert from 'node:assert/strict';
import { extractToken } from './token.js';

const TOKEN = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ';
const OTHER_TOKEN = 'ZYXWVUTSRQPONMLKJIHGFEDCBAzyxwvutsrqponmlkj';

test('extractToken accepts a raw 43-character base64url token', () => {
  assert.equal(extractToken(TOKEN), TOKEN);
  assert.equal(extractToken(`  ${OTHER_TOKEN}  `), OTHER_TOKEN);
});

test('extractToken pulls the token out of a confirmation or cancellation link', () => {
  assert.equal(
    extractToken(`https://consultorio.example/api/public/appointments/${TOKEN}/confirm`),
    TOKEN
  );
  assert.equal(
    extractToken(`/api/public/appointments/${OTHER_TOKEN}/cancel/?utm=email#frag`),
    OTHER_TOKEN
  );
});

test('extractToken rejects empty, short or tokenless input', () => {
  assert.equal(extractToken(''), null);
  assert.equal(extractToken('   '), null);
  assert.equal(extractToken('abc123'), null);
  assert.equal(extractToken('https://consultorio.example/api/public/appointments/confirm'), null);
});
