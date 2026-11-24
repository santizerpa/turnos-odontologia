import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateBookingForm } from './validation.js';

const VALID = {
  nombre: 'Ana Ficticia',
  apellido: 'Pérez Prueba',
  dni: '12345678',
  email: 'ana.ficticia@example.invalid'
};

test('validateBookingForm accepts complete fictitious data with a selected slot', () => {
  assert.equal(validateBookingForm(VALID, '2026-12-01T10:00:00Z'), null);
});

test('validateBookingForm reports a missing slot and each missing patient field', () => {
  assert.match(String(validateBookingForm(VALID, null)), /horario/i);
  assert.match(String(validateBookingForm({ ...VALID, nombre: '  ' }, '2026-12-01T10:00:00Z')), /nombre/i);
  assert.match(
    String(validateBookingForm({ ...VALID, email: '' }, '2026-12-01T10:00:00Z')),
    /email/i
  );
});
