import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  agendaEmptyText,
  agendaEntryText,
  agendaRowAttributes,
  loginFeedback,
  outboxRowText,
  patientPageText,
  patientRowText,
  profileSummary,
  screenSections,
  sessionUnavailableNotice,
  statusActionLabels,
  statusOutcomeText,
  tokenDetailsText,
  tokenUiLabels
} from './ui.js';
import { NO_APPOINTMENTS_MESSAGE, type AgendaEntry, type PatientSummary } from './agenda.js';
import { NO_MESSAGES_MESSAGE, type OutboxMessage } from './outbox.js';
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

const agendaEntry: AgendaEntry = {
  id: 'appointment-1',
  patientId: 'patient-1',
  patientName: 'Paciente Demo',
  startAt: '2026-11-24T10:00:00Z',
  durationMinutes: 30,
  status: 'CONFIRMED',
  activeSlot: '2026-11-24T10:00:00Z'
};

test('agenda rows show local start, patient name without UUID, duration and kept active slot', () => {
  const text = agendaEntryText(agendaEntry);
  const localStart = '24/11/26 07:00';

  assert.equal(text, `${localStart} · Paciente Demo · 30 min · CONFIRMED · franja: 07:00–07:30`);
  assert.equal(text.includes('(patient-1)'), false);
  assert.equal(text.startsWith('2026-11-24T10:00:00Z'), false);
});

test('non-null active slot is shown as a Buenos Aires local range, never as a UTC Instant', () => {
  const text = agendaEntryText({ ...agendaEntry, startAt: '2026-11-26T17:30:00Z', activeSlot: '2026-11-26T17:30:00Z' });

  assert.equal(text.endsWith('franja: 14:30–15:00'), true);
  assert.equal(text.includes('2026-11-26T17:30:00Z'), false);
  assert.equal(text.includes('17:30'), false);
});

test('agenda start is rendered in Buenos Aires local time, not UTC', () => {
  const text = agendaEntryText({ ...agendaEntry, startAt: '2026-11-25T02:30:00Z' });

  // 02:30Z on the 25th is 23:30 on the 24th in Buenos Aires, always in 24-hour format.
  assert.equal(text.startsWith('24/11/26 23:30 · '), true);
  assert.equal(text.includes('25/11'), false);
  assert.equal(text.includes('p. m.'), false);
});

test('agenda rows use patientId as single fallback and show null active slot as franja libre', () => {
  const withoutName = agendaEntryText({
    ...agendaEntry,
    patientId: 'patient-outside-map',
    patientName: 'patient-outside-map',
    activeSlot: null
  });
  const localStart = '24/11/26 07:00';

  assert.equal(withoutName, `${localStart} · patient-outside-map · 30 min · CONFIRMED · franja: franja libre`);
  assert.equal(withoutName.split('patient-outside-map').length - 1, 1);
});

test('agenda row attributes keep the appointment id and expose the exact received status', () => {
  for (const status of ['PENDING', 'CONFIRMED', 'ATTENDED', 'ABSENT']) {
    assert.deepEqual(agendaRowAttributes({ ...agendaEntry, id: `appointment-${status}`, status }), {
      'data-appointment-id': `appointment-${status}`,
      'data-status': status
    });
  }
});

test('outbox rows show patient name and DNI instead of the message status, never the token', () => {
  const message: OutboxMessage = {
    id: 'message-1',
    appointmentId: 'appointment-1',
    recipientEmail: 'paciente@demo.invalid',
    subject: 'Confirmación de turno',
    body: 'Enlace /api/public/appointments/abcdefghijklmnopqrstuvwxyz1234/confirm',
    status: 'PENDING',
    type: 'CONFIRMATION',
    createdAt: '2026-11-24T03:00:00Z',
    sentAt: '2026-11-24T03:01:00Z',
    patientName: 'Paciente Demo',
    patientDni: '30111222'
  };
  const text = outboxRowText(message);

  assert.equal(text, 'Confirmación de turno · paciente@demo.invalid · Paciente Demo · 30111222 · 24/11/26 00:00');
  assert.equal(text.includes('PENDING'), false);
  assert.equal(text.includes('abcdefghijklmnopqrstuvwxyz1234'), false);
  assert.equal(NO_MESSAGES_MESSAGE, 'Sin mensajes en la bandeja.');
});

test('outbox rows without a patient omit name and DNI without placeholders', () => {
  const text = outboxRowText({
    id: 'message-2',
    appointmentId: null,
    recipientEmail: 'aviso@demo.invalid',
    subject: 'Aviso',
    body: 'Sin enlace.',
    status: 'PENDING',
    type: 'CONFIRMATION',
    createdAt: '2026-11-24T03:00:00Z',
    sentAt: null,
    patientName: null,
    patientDni: null
  });

  assert.equal(text, 'Aviso · aviso@demo.invalid · 24/11/26 00:00');
});

test('outbox history offers only the informative status lookup in local time', () => {
  assert.deepEqual(tokenUiLabels(), { info: 'Ver estado del turno' });
  assert.equal(
    tokenDetailsText({ startAt: '2026-11-26T17:30:00Z', status: 'PENDING' }),
    'Inicio: 26/11/26 14:30 · Estado: PENDING'
  );
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

test('dentist status actions exclude confirmation and surface backend messages', () => {
  assert.deepEqual(statusActionLabels(), [
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
  assert.deepEqual(screenSections('console'), ['agenda', 'patients', 'outbox']);
  assert.equal(sessionUnavailableNotice(unavailable), 'Su sesión no está disponible. Ingrese nuevamente.');
  assert.equal(sessionUnavailableNotice(anonymousSession()), null);
  assert.deepEqual(authenticatedSession(profile), { kind: 'authenticated', profile });
});
