import { test } from 'node:test';
import assert from 'node:assert/strict';
import { inboxPath } from './endpoints.js';
import { INBOX_EMPTY_MESSAGE, inboxErrorMessage, inboxMessageView, type InboxMessage } from './inbox.js';

const TOKEN = 'A'.repeat(43);

const message: InboxMessage = {
  id: 'message-1',
  subject: 'Confirma tu turno',
  recipientEmail: 'paciente@demo.invalid',
  patientDni: '30111222',
  appointmentStartAt: '2026-11-26T17:30:00Z',
  appointmentDurationMinutes: 30,
  createdAt: '2026-11-25T15:10:00Z',
  body: `Para confirmar tu turno, visita /api/public/appointments/${TOKEN}/confirm. Para cancelarlo, visita /api/public/appointments/${TOKEN}/cancel.`
};

test('inbox path encodes the email as the only query parameter', () => {
  assert.equal(inboxPath(' paciente+demo@demo.invalid '), '/api/public/inbox?email=paciente%2Bdemo%40demo.invalid');
});

test('inbox message shows subject, email, dni and local send date, then the local appointment start', () => {
  const view = inboxMessageView(message);

  assert.equal(view.header, 'Confirma tu turno · paciente@demo.invalid · 30111222 · 25/11/26 12:10');
  assert.equal(view.appointment, 'Turno: 26/11/26 14:30 · 30 minutos');
  assert.equal(view.token, TOKEN);
  assert.equal(`${view.header} ${view.appointment}`.includes('Z'), false);
});

test('one token per message even when the body has confirm and cancel links', () => {
  assert.equal(inboxMessageView(message).token, TOKEN);
});

test('message without appointment or link has no appointment line and no token', () => {
  const view = inboxMessageView({
    ...message,
    patientDni: null,
    appointmentStartAt: null,
    appointmentDurationMinutes: null,
    body: 'Aviso ficticio sin enlace.'
  });

  assert.equal(view.header, 'Confirma tu turno · paciente@demo.invalid · 25/11/26 12:10');
  assert.equal(view.appointment, null);
  assert.equal(view.token, null);
  assert.equal(INBOX_EMPTY_MESSAGE, 'No hay mensajes para este email.');
});

test('inbox errors never mention links and explain a server without the inbox route', () => {
  assert.equal(inboxErrorMessage(404, { error: 'Bandeja simulada deshabilitada' }), 'Bandeja simulada deshabilitada');
  assert.equal(
    inboxErrorMessage(404, { timestamp: 'x', status: 404, error: 'Not Found', path: '/api/public/inbox' }),
    'La bandeja simulada no está disponible en este servidor.'
  );
  assert.equal(inboxErrorMessage(422, { error: 'Email inválido' }), 'Email inválido');
  assert.equal(inboxErrorMessage(0, null), 'No se pudo abrir la bandeja. Intente nuevamente más tarde.');
  assert.equal(inboxErrorMessage(500, { error: 'Not Found' }), 'No se pudo abrir la bandeja. Intente nuevamente más tarde.');
});
