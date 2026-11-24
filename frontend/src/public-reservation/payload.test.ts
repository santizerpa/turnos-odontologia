import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildBookingPayload,
  formatConfirmation,
  formatStartAt,
  formatStatus,
  formatTokenAction,
  normalizeDni
} from './payload.js';

test('buildBookingPayload trims fictitious patient data and keeps the exact backend keys', () => {
  const payload = buildBookingPayload(
    {
      nombre: '  Ana Ficticia  ',
      apellido: ' Pérez Prueba ',
      dni: '12.345.678',
      email: 'ana.ficticia@example.invalid'
    },
    '2026-12-01T10:00:00Z'
  );

  assert.deepEqual(payload, {
    nombre: 'Ana Ficticia',
    apellido: 'Pérez Prueba',
    dni: '12345678',
    email: 'ana.ficticia@example.invalid',
    startAt: '2026-12-01T10:00:00Z'
  });
});

test('buildBookingPayload handles different fictitious values without dropping fields', () => {
  const payload = buildBookingPayload(
    {
      nombre: 'Bruno Demo',
      apellido: 'Gómez Ejemplo',
      dni: ' 98 765 432 ',
      email: 'bruno.demo@example.invalid'
    },
    '2026-12-02T15:30:00Z'
  );

  assert.equal(payload.dni, '98765432');
  assert.equal(payload.startAt, '2026-12-02T15:30:00Z');
  assert.equal(payload.nombre, 'Bruno Demo');
  assert.equal(payload.email, 'bruno.demo@example.invalid');
});

test('normalizeDni removes separators and formatStartAt trims or marks missing values', () => {
  assert.equal(normalizeDni('12.345.678'), '12345678');
  assert.equal(normalizeDni(' 98 765 432 '), '98765432');
  assert.equal(formatStartAt('  2026-12-01T10:00:00Z  '), '2026-12-01T10:00:00Z');
  assert.equal(formatStartAt(''), '—');
});

test('formatConfirmation reads the 201 response fields without inventing a token', () => {
  assert.deepEqual(
    formatConfirmation({ startAt: '2026-12-01T10:00:00Z', durationMinutes: 30, status: 'PENDING' }),
    { start: '2026-12-01T10:00:00Z', duration: '30 minutos', status: 'PENDING' }
  );
  assert.deepEqual(
    formatConfirmation({ startAt: '', durationMinutes: 30, status: 'PENDING' }),
    { start: '—', duration: '30 minutos', status: 'PENDING' }
  );
});

test('formatStatus and formatTokenAction surface the JSON returned by GET and POST', () => {
  assert.deepEqual(formatStatus({ startAt: '2026-12-01T10:00:00Z', status: 'CONFIRMED' }), {
    start: '2026-12-01T10:00:00Z',
    status: 'CONFIRMED'
  });
  assert.deepEqual(formatTokenAction({ message: 'confirmación exitosa', status: 'CONFIRMED' }), {
    message: 'confirmación exitosa',
    status: 'CONFIRMED'
  });
  assert.deepEqual(formatTokenAction({ message: '', status: 'CANCELLED' }), {
    message: 'Acción completada.',
    status: 'CANCELLED'
  });
});
