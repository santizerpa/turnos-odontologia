import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  agendaEmptyText,
  agendaEntryText,
  loginFeedback,
  outboxRowText,
  patientPageText,
  patientRowText,
  profileSummary,
  screenSections,
  sessionUnavailableNotice,
  statusActionLabels,
  statusOutcomeText,
  tokenActionFeedbackText,
  tokenDetailsText,
  tokenUiLabels,
  waitingRowText,
  waitingUiSummary
} from './ui.js';
import { NO_APPOINTMENTS_MESSAGE, type AgendaEntry, type PatientSummary } from './agenda.js';
import { NO_MESSAGES_MESSAGE, type OutboxMessage } from './outbox.js';
import { NO_ENTRIES_MESSAGE, type WaitingEntry } from './waiting-list.js';
import { anonymousSession, authenticatedSession, sessionUnavailable, type DentistProfile } from './session.js';

const profile: DentistProfile = {
  id: 'dentist-1',
  email: 'odontologo@example.invalid',
  firstName: 'Demo',
  lastName: 'Ficticio'
};

test('login feedback is generic and never reveals which credential failed', () => {
  const feedback = loginFeedback('Credenciales inválidas');

  assert.deepEqual(feedback, {
    visible: true,
    message: 'Credenciales inválidas',
    hint: 'Verifique sus datos e intente nuevamente.'
  });
  assert.equal(/email|contraseña|password/i.test((feedback.message ?? '') + (feedback.hint ?? '')), false);
  assert.deepEqual(loginFeedback(null), { visible: false, message: null, hint: null });
});

test('profile summary uses only the four fields returned by login', () => {
  const withSecret = { ...profile, passwordHash: 'must-not-be-shown' } as unknown as DentistProfile;
  const summary = profileSummary(withSecret);

  assert.equal(summary, 'Demo Ficticio · odontologo@example.invalid');
  assert.equal(summary.includes('must-not-be-shown'), false);
});

test('agenda rows expose only the fields returned by the API', () => {
  const entry: AgendaEntry = {
    id: 'appointment-1',
    patientId: 'patient-1',
    patientName: 'Paciente Demo',
    startAt: '2026-11-24T10:00:00Z',
    durationMinutes: 30,
    status: 'CONFIRMED',
    activeSlot: '2026-11-24T10:00:00Z'
  };
  const text = agendaEntryText(entry);

  assert.equal(text, '2026-11-24T10:00:00Z · Paciente Demo (patient-1) · 30 min · CONFIRMED · franja: 2026-11-24T10:00:00Z');

  const withoutName = agendaEntryText({ ...entry, patientId: 'patient-outside-map', patientName: 'patient-outside-map', activeSlot: null });

  assert.equal(withoutName, '2026-11-24T10:00:00Z · patient-outside-map (patient-outside-map) · 30 min · CONFIRMED · franja: sin franja activa');
});

test('waiting list rows keep server order and preferences without DNI or assignment actions', () => {
  const first: WaitingEntry = {
    id: 'entry-1',
    nombre: 'Paciente',
    apellido: 'Uno',
    email: 'uno@demo.invalid',
    requestedDateFrom: '2026-11-25T09:00:00Z',
    requestedDateTo: '2026-11-25T10:00:00Z',
    status: 'WAITING'
  };
  const second: WaitingEntry = { ...first, id: 'entry-2', nombre: 'Paciente', apellido: 'Dos', email: 'dos@demo.invalid' };

  assert.equal(waitingRowText(first), 'Paciente Uno · uno@demo.invalid · 2026-11-25T09:00:00Z a 2026-11-25T10:00:00Z · WAITING');
  assert.equal(waitingRowText(first).includes('DNI'), false);
  assert.equal(waitingRowText(first).includes('Asignar'), false);
  assert.equal(waitingUiSummary([first, second]), '2 entradas · sin asignación');
  assert.equal(waitingUiSummary([]), NO_ENTRIES_MESSAGE);
});

test('outbox rows show only message fields and never DNI or token hashes', () => {
  const message: OutboxMessage = {
    id: 'message-1',
    appointmentId: 'appointment-1',
    recipientEmail: 'paciente@demo.invalid',
    subject: 'Confirmación de turno',
    body: 'DNI 12345678Z /api/public/appointments/abcdefghijklmnopqrstuvwxyz1234/confirm',
    status: 'SENT',
    type: 'CONFIRMATION',
    createdAt: '2026-11-24T03:00:00Z',
    sentAt: '2026-11-24T03:01:00Z'
  };
  const text = outboxRowText(message);

  assert.equal(text, 'Confirmación de turno · paciente@demo.invalid · SENT · CONFIRMATION · 2026-11-24T03:00:00Z');
  assert.equal(text.includes('12345678Z'), false);
  assert.equal(text.includes('abcdefghijklmnopqrstuvwxyz1234'), false);
  assert.equal(NO_MESSAGES_MESSAGE, 'Sin mensajes en la bandeja.');
});

test('token controls separate informative GET from explicit POST feedback', () => {
  assert.deepEqual(tokenUiLabels(), {
    info: 'Ver estado del enlace',
    confirm: 'Confirmar con token',
    cancel: 'Cancelar con token'
  });
  assert.equal(tokenDetailsText({ startAt: '2026-11-24T10:00:00Z', status: 'PENDING' }), 'Inicio: 2026-11-24T10:00:00Z · Estado: PENDING');
  assert.equal(tokenActionFeedbackText({ message: 'Turno confirmado', status: 'CONFIRMED' }), 'Turno confirmado · Estado: CONFIRMED');
});

test('patient rows expose only the five fields returned by the list', () => {
  const complete: PatientSummary = {
    id: 'patient-1',
    dni: '12345678Z',
    nombre: 'Paciente',
    apellido: 'Demo',
    email: 'paciente@demo.invalid'
  };
  const minimal: PatientSummary = { id: 'patient-2', nombre: 'Sin', apellido: 'Datos' };

  assert.equal(patientRowText(complete), 'Paciente Demo · 12345678Z · paciente@demo.invalid');
  assert.equal(patientRowText(minimal), 'Sin Datos');
  assert.equal(patientRowText(complete).includes('patient-1'), false);
});

test('patient page view reports only server pagination metadata', () => {
  assert.equal(patientPageText({ page: 0, size: 20, totalElements: 21, totalPages: 2 }), 'Página 1 de 2 · 21 pacientes');
  assert.equal(patientPageText({ page: 3, size: 10, totalElements: 0, totalPages: 0 }), 'Página 4 de 0 · 0 pacientes');
});

test('status actions cover the four existing statuses and surface backend messages', () => {
  assert.deepEqual(statusActionLabels(), [
    { status: 'CONFIRMED', label: 'Confirmar' },
    { status: 'ABSENT', label: 'Marcar ausente' },
    { status: 'ATTENDED', label: 'Marcar atendido' },
    { status: 'CANCELLED', label: 'Cancelar' }
  ]);
  assert.equal(statusOutcomeText({ kind: 'updated', status: 'CONFIRMED', message: 'estado actualizado' }), 'estado actualizado');
  assert.equal(statusOutcomeText({ kind: 'cancelled', message: 'cancelado' }), 'cancelado');
  assert.equal(statusOutcomeText({ kind: 'already-cancelled', message: 'ya cancelado' }), 'ya cancelado');
  assert.equal(statusOutcomeText(null), null);
});

test('agenda empty state is not an error and is distinct from a range error', () => {
  assert.equal(agendaEmptyText(), NO_APPOINTMENTS_MESSAGE);
  assert.equal(agendaEmptyText().includes('Sin turnos'), true);
  assert.equal(agendaEmptyText().includes('Rango'), false);
});

test('console sections appear only after login and expired session reuses the login document', () => {
  const unavailable = sessionUnavailable(anonymousSession(), 401, { error: 'No autenticado' }).state;

  assert.deepEqual(screenSections('login'), []);
  assert.deepEqual(screenSections('console'), ['agenda', 'patients', 'outbox', 'waiting-list']);
  assert.equal(sessionUnavailableNotice(unavailable), 'Su sesión no está disponible. Ingrese nuevamente.');
  assert.equal(sessionUnavailableNotice(anonymousSession()), null);
  assert.deepEqual(authenticatedSession(profile), { kind: 'authenticated', profile });
});
