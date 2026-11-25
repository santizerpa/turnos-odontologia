import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  backendMessage,
  GENERIC_ERROR_MESSAGE,
  INVALID_CREDENTIALS_MESSAGE,
  NO_AUTHENTICATED_MESSAGE,
  visibleError
} from './errors.js';

test('error responses keep the exact backend message for 401, 404, 409, 410 and 422', () => {
  const cases: Array<[number, string]> = [
    [401, 'No autenticado'],
    [404, 'Token inexistente o inválido'],
    [409, 'La franja solicitada ya está ocupada'],
    [410, 'Token vencido'],
    [422, 'Transición de estado inválida']
  ];

  for (const [status, message] of cases) {
    assert.equal(visibleError(status, { error: message }), message);
  }
});

test('login 401 and session 401 remain distinguishable', () => {
  assert.equal(visibleError(401, { error: INVALID_CREDENTIALS_MESSAGE }), 'Credenciales inválidas');
  assert.equal(visibleError(401, { error: NO_AUTHENTICATED_MESSAGE }), 'No autenticado');
  assert.equal(backendMessage({ error: 'Credenciales inválidas' }), 'Credenciales inválidas');
});

test('ProblemDetail 400 shows detail even when instance is absent', () => {
  const body = {
    type: 'about:blank',
    title: 'Bad Request',
    status: 400,
    detail: 'Datos de paciente inválidos.'
  };

  assert.equal(visibleError(400, body), 'Datos de paciente inválidos.');
  assert.equal('instance' in body, false);
  assert.equal(backendMessage(body), 'Datos de paciente inválidos.');
});

test('ProblemDetail 500 shows detail without exposing title, type or traces', () => {
  const body = {
    type: 'about:blank',
    title: 'Error interno',
    status: 500,
    detail: 'Error interno al procesar la bandeja',
    trace: 'at com.turnosodontologia.OutboxController'
  };
  const message = visibleError(500, body);

  assert.equal(message, 'Error interno al procesar la bandeja');
  assert.equal(message.includes('Error interno al procesar la bandeja'), true);
  assert.equal(message.includes('at com.'), false);
  assert.equal(message.includes('about:blank'), false);
});

test('error takes precedence over detail when both formats are present', () => {
  assert.equal(backendMessage({ error: 'Rango de agenda inválido', detail: 'otro' }), 'Rango de agenda inválido');
  assert.equal(visibleError(422, { error: 'Rango de agenda inválido', detail: 'otro' }), 'Rango de agenda inválido');
});

test('unreadable bodies use the exact generic fallback', () => {
  const bodies: unknown[] = [null, undefined, 'texto', [], {}, { error: '' }, { error: '   ' }, { detail: 42 }];

  for (const body of bodies) {
    assert.equal(visibleError(500, body), GENERIC_ERROR_MESSAGE);
  }

  assert.equal(
    GENERIC_ERROR_MESSAGE,
    'No se pudo completar la solicitud. Intente nuevamente más tarde.'
  );
});

test('messages are trimmed and fields that are not messages are ignored', () => {
  assert.equal(backendMessage({ detail: '  Token vencido  ' }), 'Token vencido');
  assert.equal(backendMessage({ error: '  No autenticado\n' }), 'No autenticado');
  assert.equal(backendMessage({ title: 'Bad Request', type: 'about:blank', status: 400 }), null);
  assert.equal(visibleError(400, { title: 'Bad Request', type: 'about:blank', status: 400 }), GENERIC_ERROR_MESSAGE);
});
