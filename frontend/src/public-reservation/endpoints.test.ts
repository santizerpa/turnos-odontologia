import { test } from 'node:test';
import assert from 'node:assert/strict';
import { appointmentsPath, slotsPath, tokenActionPath } from './endpoints.js';

const TOKEN = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ';
const OTHER_TOKEN = 'ZYXWVUTSRQPONMLKJIHGFEDCBAzyxwvutsrqponmlkj';

test('slotsPath uses the public slots endpoint with an encoded date', () => {
  assert.equal(slotsPath('2026-12-01'), '/api/public/slots?date=2026-12-01');
  assert.equal(slotsPath('2026-12-01T00:00'), '/api/public/slots?date=2026-12-01T00%3A00');
});

test('appointmentsPath targets the existing public appointments endpoint', () => {
  assert.equal(appointmentsPath(), '/api/public/appointments');
});

test('tokenActionPath targets only confirm or cancel on the public token endpoint', () => {
  assert.equal(tokenActionPath(TOKEN, 'confirm'), `/api/public/appointments/${TOKEN}/confirm`);
  assert.equal(tokenActionPath(OTHER_TOKEN, 'cancel'), `/api/public/appointments/${OTHER_TOKEN}/cancel`);
});
