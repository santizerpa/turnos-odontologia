import { test } from 'node:test';
import assert from 'node:assert/strict';
import { GENERIC_FALLBACK, mapApiError, readBackendError } from './errors.js';

test('409 shows the backend message and offers another slot or the office', () => {
  const visible = mapApiError(409, { error: 'La franja solicitada ya está ocupada' });

  assert.equal(visible.kind, 'conflict');
  assert.equal(visible.message, 'La franja solicitada ya está ocupada');
  assert.match(visible.hint, /otra franja/i);
  assert.match(visible.hint, /consultorio/i);
});

test('409 without a readable error uses the generic fallback but keeps recovery guidance', () => {
  const visible = mapApiError(409, {});

  assert.equal(visible.kind, 'conflict');
  assert.equal(visible.message, GENERIC_FALLBACK);
  assert.match(visible.hint, /consultorio/i);
});

test('422 shows the backend message for invalid data and for an out-of-window date', () => {
  const invalidData = mapApiError(422, { error: 'Datos de reserva inválidos' });
  const outOfWindow = mapApiError(422, {
    error: 'La fecha solicitada está fuera de la ventana pública'
  });
  const unavailableSlot = mapApiError(422, { error: 'La franja solicitada no está disponible' });

  assert.equal(invalidData.kind, 'validation');
  assert.equal(invalidData.message, 'Datos de reserva inválidos');
  assert.match(invalidData.hint, /Corrija/i);
  assert.equal(outOfWindow.message, 'La fecha solicitada está fuera de la ventana pública');
  assert.equal(unavailableSlot.kind, 'validation');
  assert.match(unavailableSlot.hint, /otra/i);
});

test('404 and 410 map to invalid and expired link messages', () => {
  const invalid = mapApiError(404, { error: 'Token inexistente o inválido' });
  const expired = mapApiError(410, { error: 'Token vencido' });

  assert.equal(invalid.kind, 'invalid-token');
  assert.equal(invalid.message, 'Token inexistente o inválido');
  assert.equal(expired.kind, 'expired-token');
  assert.equal(expired.message, 'Token vencido');
  assert.match(expired.hint, /solicite uno nuevo/i);
});

test('503 and 500 prefer the exact backend message and fall back when it is missing', () => {
  assert.equal(
    mapApiError(503, { error: 'Servicio temporalmente no disponible' }).message,
    'Servicio temporalmente no disponible'
  );
  assert.equal(mapApiError(503, null).message, GENERIC_FALLBACK);
  assert.equal(
    mapApiError(500, { error: 'No se pudo procesar la reserva' }).message,
    'No se pudo procesar la reserva'
  );
  assert.equal(mapApiError(500, undefined).message, GENERIC_FALLBACK);
});

test('unrecognized statuses and unreadable bodies use the generic fallback', () => {
  assert.equal(mapApiError(418, {}).message, GENERIC_FALLBACK);
  assert.equal(mapApiError(418, {}).kind, 'unknown');
  assert.equal(mapApiError(200, 'texto').message, GENERIC_FALLBACK);
  assert.equal(mapApiError(0, []).message, GENERIC_FALLBACK);
  assert.equal(readBackendError({ error: '' }), null);
  assert.equal(readBackendError({ error: '   ' }), null);
  assert.equal(readBackendError({ error: 42 }), null);
  assert.equal(readBackendError(null), null);
  assert.equal(readBackendError({ error: '  Error de prueba  ' }), 'Error de prueba');
});
