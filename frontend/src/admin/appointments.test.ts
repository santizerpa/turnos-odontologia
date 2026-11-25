import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  applyOutcome,
  readStatusResponse,
  statusErrorMessage,
  statusRequest,
  type AgendaAppointment,
  type AppointmentStatus
} from './appointments.js';

const entry: AgendaAppointment = {
  id: 'appointment-1',
  patientId: 'patient-1',
  patientName: 'Paciente Demo',
  startAt: '2026-11-24T10:00:00Z',
  durationMinutes: 30,
  status: 'PENDING',
  activeSlot: '2026-11-24T10:00:00Z'
};

test('PATCH bodies use the four existing statuses with an explicit reason field', () => {
  const statuses: AppointmentStatus[] = ['CONFIRMED', 'ABSENT', 'ATTENDED', 'CANCELLED'];

  for (const status of statuses) {
    assert.deepEqual(statusRequest(status, '  Motivo ficticio  '), {
      status,
      reason: 'Motivo ficticio'
    });
  }

  assert.deepEqual(statusRequest('CONFIRMED'), { status: 'CONFIRMED', reason: null });
  assert.deepEqual(statusRequest('CANCELLED', '   '), { status: 'CANCELLED', reason: null });
});

test('successful responses are read as updated, cancelled or already cancelled', () => {
  assert.deepEqual(readStatusResponse({ status: 'CONFIRMED', message: 'estado actualizado' }), {
    kind: 'updated',
    status: 'CONFIRMED',
    message: 'estado actualizado'
  });
  assert.deepEqual(readStatusResponse({ status: 'ATTENDED', message: 'estado actualizado' }), {
    kind: 'updated',
    status: 'ATTENDED',
    message: 'estado actualizado'
  });
  assert.deepEqual(readStatusResponse({ cancelled: true, message: 'cancelado' }), {
    kind: 'cancelled',
    message: 'cancelado'
  });
  assert.deepEqual(readStatusResponse({ cancelled: false, message: 'ya cancelado' }), {
    kind: 'already-cancelled',
    message: 'ya cancelado'
  });
  assert.equal(readStatusResponse({ message: 'otro' }), null);
});

test('business errors keep the exact backend message for 422, 409, 404 and 401', () => {
  assert.equal(statusErrorMessage(422, { error: 'Transición de estado inválida' }), 'Transición de estado inválida');
  assert.equal(statusErrorMessage(409, { error: 'La franja solicitada ya está ocupada' }), 'La franja solicitada ya está ocupada');
  assert.equal(statusErrorMessage(404, { error: 'Turno no encontrado: 123e4567-e89b-12d3-a456-426614174000' }), 'Turno no encontrado: 123e4567-e89b-12d3-a456-426614174000');
  assert.equal(statusErrorMessage(401, { error: 'No autenticado' }), 'No autenticado');
  assert.equal(statusErrorMessage(500, {}), 'No se pudo completar la solicitud. Intente nuevamente más tarde.');
});

test('updated outcomes change only the local status and cancellations remove the active entry', () => {
  const updated = applyOutcome(entry, { kind: 'updated', status: 'ABSENT', message: 'estado actualizado' });

  assert.deepEqual(updated, { ...entry, status: 'ABSENT' });
  assert.equal(entry.status, 'PENDING');
  assert.equal(applyOutcome(entry, { kind: 'cancelled', message: 'cancelado' }), null);
  assert.equal(applyOutcome(entry, { kind: 'already-cancelled', message: 'ya cancelado' }), null);
});

test('a failed action leaves the visible state untouched and is not reported as success', () => {
  const unchanged = applyOutcome(entry, null);

  assert.equal(unchanged, entry);
  assert.equal(unchanged?.status, 'PENDING');
  assert.equal(readStatusResponse({ error: 'Transición de estado inválida' }), null);
});
