import { test } from 'node:test';
import assert from 'node:assert/strict';
import { GENERIC_FALLBACK, mapApiError } from './errors.js';
import {
  bookingConfirmationFeedback,
  errorFeedback,
  slotLabel,
  slotsStatusText,
  tokenActionFeedback,
  tokenErrorFeedback,
  tokenStatusFeedback
} from './views.js';

test('slotLabel shows both extremes of the fictitious free slot', () => {
  assert.equal(
    slotLabel({ start: '2026-12-01T10:00:00Z', end: '2026-12-01T10:30:00Z' }),
    '10:00 - 10:30'
  );
});

test('slotsStatusText distinguishes idle, loading, empty and populated lists', () => {
  assert.equal(slotsStatusText('idle', 0), 'Elija una fecha dentro de la ventana permitida.');
  assert.equal(slotsStatusText('loading', 0), 'Cargando horarios...');
  assert.equal(slotsStatusText('loaded', 0), 'No hay horarios libres para esa fecha.');
  assert.equal(slotsStatusText('loaded', 2), 'Seleccione uno de los 2 horarios libres.');
});

test('errorFeedback makes a 409 conflict visible without claiming success', () => {
  const feedback = errorFeedback(
    mapApiError(409, { error: 'La franja solicitada ya está ocupada' })
  );

  assert.equal(feedback.tone, 'error');
  assert.equal(feedback.title, 'No se pudo reservar el turno');
  assert.equal(feedback.message, 'La franja solicitada ya está ocupada');
  assert.match(feedback.hint, /otra franja/i);
  assert.match(feedback.hint, /consultorio/i);
});

test('errorFeedback gives 422 a correction title and keeps the exact backend message', () => {
  const feedback = errorFeedback(mapApiError(422, { error: 'Datos de reserva inválidos' }));

  assert.equal(feedback.title, 'Revise los datos ingresados');
  assert.equal(feedback.message, 'Datos de reserva inválidos');
  assert.match(feedback.hint, /Corrija/i);

  const outOfWindow = errorFeedback(
    mapApiError(422, { error: 'La fecha solicitada está fuera de la ventana pública' })
  );

  assert.equal(outOfWindow.title, 'Revise los datos ingresados');
  assert.equal(outOfWindow.message, 'La fecha solicitada está fuera de la ventana pública');
});

test('errorFeedback keeps the exact 503 message and uses the fallback when it is missing', () => {
  const withBackendError = errorFeedback(
    mapApiError(503, { error: 'Servicio temporalmente no disponible' })
  );
  const withoutBackendError = errorFeedback(mapApiError(503, null));

  assert.equal(withBackendError.message, 'Servicio temporalmente no disponible');
  assert.equal(
    withoutBackendError.message,
    'No se pudo completar la solicitud. Intente nuevamente más tarde.'
  );
});

test('tokenStatusFeedback renders the JSON from the informative GET without mutating', () => {
  const feedback = tokenStatusFeedback({ start: '2026-12-01T10:00:00Z', status: 'CANCELLED' });

  assert.equal(feedback.tone, 'info');
  assert.equal(feedback.title, 'Estado del turno');
  assert.match(feedback.message, /2026-12-01T10:00:00Z/);
  assert.match(feedback.message, /CANCELLED/);
});

test('expired token feedback states that the link expired and asks for a new one', () => {
  const feedback = errorFeedback(mapApiError(410, { error: 'Token vencido' }));

  assert.equal(feedback.title, 'No se pudo consultar el enlace');
  assert.equal(feedback.message, 'Token vencido');
  assert.match(feedback.hint, /venció/i);
});

test('tokenActionFeedback uses the POST message and status for confirm and cancel', () => {
  const confirmed = tokenActionFeedback(
    { message: 'confirmación exitosa', status: 'CONFIRMED' },
    'confirm'
  );
  const cancelled = tokenActionFeedback(
    { message: 'ya cancelado', status: 'CANCELLED' },
    'cancel'
  );

  assert.equal(confirmed.tone, 'success');
  assert.equal(confirmed.title, 'Turno confirmado');
  assert.match(confirmed.message, /confirmación exitosa/);
  assert.match(confirmed.message, /CONFIRMED/);
  assert.equal(cancelled.title, 'Turno cancelado');
  assert.match(cancelled.message, /ya cancelado/);
  assert.match(cancelled.message, /CANCELLED/);
});

test('tokenErrorFeedback applies the generic fallback when a token response has no error field', () => {
  const serviceFailure = tokenErrorFeedback(mapApiError(503, {}));
  const unexpectedStatus = tokenErrorFeedback(mapApiError(418, {}));

  assert.equal(serviceFailure.title, 'No se pudo completar la acción.');
  assert.equal(serviceFailure.message, GENERIC_FALLBACK);
  assert.equal(unexpectedStatus.message, GENERIC_FALLBACK);
  assert.equal(unexpectedStatus.tone, 'error');
});

test('bookingConfirmationFeedback exposes start, duration and PENDING from the 201 response', () => {
  const feedback = bookingConfirmationFeedback({
    start: '2026-12-01T10:00:00Z',
    duration: '30 minutos',
    status: 'PENDING'
  });

  assert.equal(feedback.tone, 'success');
  assert.equal(feedback.title, 'Turno reservado');
  assert.match(feedback.message, /2026-12-01T10:00:00Z/);
  assert.match(feedback.message, /30 minutos/);
  assert.match(feedback.message, /PENDING/);
});
