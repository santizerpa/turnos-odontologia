import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  extractTokenLinks,
  NO_MESSAGES_MESSAGE,
  outboxErrorMessage,
  outboxView,
  tokenInfoText,
  type OutboxMessage
} from './outbox.js';

const message: OutboxMessage = {
  id: 'message-1',
  appointmentId: null,
  recipientEmail: 'paciente@example.invalid',
  subject: 'Confirmación de turno',
  body: 'Confirme en /api/public/appointments/tokenFicticio123/confirm o cancele en /api/public/appointments/tokenFicticio123/cancel',
  status: 'PENDING',
  type: 'CONFIRMATION',
  createdAt: '2026-11-24T09:00:00Z',
  sentAt: null,
  patientName: null,
  patientDni: null
};

test('only relative confirm and cancel links are extracted from body text', () => {
  const links = extractTokenLinks(message.body);

  assert.deepEqual(links, [
    { action: 'confirm', token: 'tokenFicticio123', href: '/api/public/appointments/tokenFicticio123/confirm' },
    { action: 'cancel', token: 'tokenFicticio123', href: '/api/public/appointments/tokenFicticio123/cancel' }
  ]);
});

test('absolute URLs and unrelated paths are ignored while several relative links are kept', () => {
  const body = [
    'https://ejemplo.invalid/api/public/appointments/absoluteToken/confirm',
    '/api/patients?page=0&size=100',
    '/api/public/appointments/otroToken456/cancel',
    'texto /api/public/appointments/otroToken456/confirm final'
  ].join(' ');

  assert.deepEqual(extractTokenLinks(body), [
    { action: 'cancel', token: 'otroToken456', href: '/api/public/appointments/otroToken456/cancel' },
    { action: 'confirm', token: 'otroToken456', href: '/api/public/appointments/otroToken456/confirm' }
  ]);
  assert.deepEqual(extractTokenLinks('sin enlaces'), []);
});

test('outbox rows keep server fields including nulls and never expose DNI or hashes', () => {
  const withDni = { ...message, dni: '12345678', passwordHash: 'hidden' } as unknown as OutboxMessage;
  const view = outboxView([withDni]);

  assert.deepEqual(view.rows, [message]);
  assert.equal(view.empty, false);
  assert.equal(JSON.stringify(view).includes('12345678'), false);
  assert.equal(JSON.stringify(view).includes('hidden'), false);
});

test('an empty outbox has a visible state and no test links', () => {
  const view = outboxView([]);

  assert.deepEqual(view.rows, []);
  assert.equal(view.empty, true);
  assert.equal(view.message, NO_MESSAGES_MESSAGE);
  assert.deepEqual(view.links, []);
});

test('token GET is informative and shows the start in Buenos Aires local time', () => {
  const text = tokenInfoText({ startAt: '2026-11-26T17:30:00Z', status: 'PENDING' });

  assert.match(text, /^Inicio: 26\/11\/(20)?26/);
  assert.equal(text.includes('2026-11-26T17:30:00Z'), false);
  assert.equal(text.endsWith(' · Estado: PENDING'), true);
});

test('outbox view offers one status lookup token per message even with confirm and cancel links', () => {
  const view = outboxView([
    {
      id: 'message-1',
      appointmentId: 'appointment-1',
      recipientEmail: 'paciente@demo.invalid',
      subject: 'Confirma tu turno',
      body: 'Confirmar: /api/public/appointments/tok_123/confirm Cancelar: /api/public/appointments/tok_123/cancel',
      status: 'PENDING',
      type: 'CONFIRMATION',
      createdAt: '2026-11-24T03:00:00Z',
      sentAt: null,
      patientName: 'Paciente Demo',
      patientDni: '30111222'
    }
  ]);

  assert.deepEqual(view.statusLookups, [{ messageId: 'message-1', token: 'tok_123' }]);
});

test('token errors keep the exact 404, 410 and four 409 messages', () => {
  const errors: Array<[number, string]> = [
    [404, 'Token inexistente o inválido'],
    [410, 'Token vencido'],
    [409, 'El turno no puede confirmarse'],
    [409, 'No se pudo confirmar el turno'],
    [409, 'Debe contactarse al consultorio para cancelar el turno'],
    [409, 'No se pudo cancelar el turno']
  ];

  for (const [status, text] of errors) {
    assert.equal(outboxErrorMessage(status, { error: text }), text);
  }
});

test('outbox 500 ProblemDetail shows only detail without instance, title or traces', () => {
  const body = {
    type: 'about:blank',
    title: 'Error interno',
    status: 500,
    detail: 'Error interno al procesar la bandeja',
    exception: 'at com.turnosodontologia.OutboxController'
  };
  const visible = outboxErrorMessage(500, body);

  assert.equal(visible, 'Error interno al procesar la bandeja');
  assert.equal('instance' in body, false);
  assert.equal(visible.includes('about:blank'), false);
  assert.equal(visible.includes('at com.'), false);
  assert.equal(outboxErrorMessage(500, null), 'No se pudo completar la solicitud. Intente nuevamente más tarde.');
});
