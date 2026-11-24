import { test } from 'node:test';
import assert from 'node:assert/strict';
import { dateWindow, formatLocalDate, isDateInWindow } from './dates.js';

test('dateWindow limits selection from 24 hours to 30 days since now', () => {
  const now = new Date(2026, 5, 15, 12, 0, 0);

  assert.deepEqual(dateWindow(now), { min: '2026-06-16', max: '2026-07-15' });
});

test('formatLocalDate pads month and day', () => {
  assert.equal(formatLocalDate(new Date(2026, 0, 5, 9, 0, 0)), '2026-01-05');
});

test('isDateInWindow accepts both boundaries and rejects dates outside', () => {
  const now = new Date(2026, 5, 15, 12, 0, 0);

  assert.equal(isDateInWindow('2026-06-16', now), true);
  assert.equal(isDateInWindow('2026-07-15', now), true);
  assert.equal(isDateInWindow('2026-06-15', now), false);
  assert.equal(isDateInWindow('2026-07-16', now), false);
});
