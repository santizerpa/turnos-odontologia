import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ApiError, createAdminApi, type FetchLike } from './api.js';

interface RecordedCall {
  url: string;
  init?: { method?: string; credentials?: string; headers?: Record<string, string>; body?: string };
}

function fakeFetch(responses: Array<{ status: number; body?: unknown }>): {
  fetch: FetchLike;
  calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  let index = 0;

  const fetchImpl: FetchLike = async (url, init) => {
    calls.push({ url, init: init as RecordedCall['init'] });
    const response = responses[index++] ?? { status: 200, body: {} };

    return {
      ok: response.status >= 200 && response.status < 300,
      status: response.status,
      json: async () => response.body ?? null
    };
  };

  return { fetch: fetchImpl, calls };
}

test('login posts JSON with same-origin credentials and does not treat invalid credentials as expiry', async () => {
  const { fetch, calls } = fakeFetch([{ status: 401, body: { error: 'Credenciales inválidas' } }]);
  let expiredCalls = 0;
  const api = createAdminApi({ fetch, onSessionExpired: () => { expiredCalls += 1; } });

  const pending = api.login('odontologo@example.invalid', 'DemoPasswordOnly');

  assert.equal(pending instanceof Promise, true);

  let captured: unknown = null;

  try {
    await pending;
  } catch (error) {
    captured = error;
  }

  assert.equal(captured instanceof ApiError, true);
  assert.equal((captured as ApiError).status, 401);
  assert.deepEqual((captured as ApiError).body, { error: 'Credenciales inválidas' });
  assert.equal(expiredCalls, 0);
  assert.deepEqual(calls[0], {
    url: '/api/auth/login',
    init: {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'odontologo@example.invalid', password: 'DemoPasswordOnly' })
    }
  });
});

test('authenticated GETs use only existing routes with exact query parameters', async () => {
  const { fetch, calls } = fakeFetch([
    { status: 200, body: [] },
    { status: 200, body: { content: [] } },
    { status: 200, body: { content: [] } },
    { status: 200, body: [] },
    { status: 200, body: { id: 'dentist-1' } }
  ]);
  const api = createAdminApi({ fetch });

  await api.appointments('2026-11-24T00:00:00Z', '2026-11-25T00:00:00Z');
  await api.patients({ page: 0, size: 100 });
  await api.patients({ dni: '12.345.678', page: 2, size: 10 });
  await api.patients({ nombre: 'ana demo', page: 0, size: 10 });
  await api.outbox();
  await api.me();

  assert.deepEqual(calls.map((call) => call.url), [
    '/api/appointments?from=2026-11-24T00%3A00%3A00Z&to=2026-11-25T00%3A00%3A00Z',
    '/api/patients?page=0&size=100',
    '/api/patients?dni=12.345.678&page=2&size=10',
    '/api/patients?nombre=ana%20demo&page=0&size=10',
    '/api/outbox/messages',
    '/api/auth/me'
  ]);
  assert.equal(calls.every((call) => call.init?.credentials === 'same-origin'), true);
  assert.equal(calls.every((call) => call.init?.body === undefined), true);
});

test('status PATCH and public token calls send the exact method and body', async () => {
  const { fetch, calls } = fakeFetch([
    { status: 200, body: { status: 'CONFIRMED', message: 'estado actualizado' } },
    { status: 200, body: { startAt: '2026-11-24T10:00:00Z', status: 'PENDING' } },
    { status: 200, body: { message: 'ya cancelado', status: 'CANCELLED' } }
  ]);
  const api = createAdminApi({ fetch });

  await api.changeStatus('appointment-1', 'CANCELLED', '  Motivo ficticio  ');
  await api.tokenInfo('tokenFicticio123', 'confirm');
  await api.tokenAction('tokenFicticio123', 'cancel');

  assert.deepEqual(calls[0], {
    url: '/api/appointments/appointment-1/status',
    init: {
      method: 'PATCH',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'CANCELLED', reason: 'Motivo ficticio' })
    }
  });
  assert.equal(calls[1].url, '/api/public/appointments/tokenFicticio123/confirm');
  assert.equal(calls[1].init?.method, undefined);
  assert.equal(calls[2].url, '/api/public/appointments/tokenFicticio123/cancel');
  assert.equal(calls[2].init?.method, 'POST');
});

test('authenticated 401 notifies session handling once and never retries or redirects', async () => {
  const { fetch, calls } = fakeFetch([
    { status: 401, body: { error: 'No autenticado' } },
    { status: 200, body: [] }
  ]);
  const expired: Array<{ status: number; body: unknown }> = [];
  const api = createAdminApi({ fetch, onSessionExpired: (status, body) => expired.push({ status, body }) });

  await assert.rejects(
    api.outbox(),
    (error: unknown) => error instanceof ApiError && error.status === 401
  );

  assert.deepEqual(expired, [{ status: 401, body: { error: 'No autenticado' } }]);
  assert.equal(calls.length, 1);
  assert.deepEqual(calls[0].init, { credentials: 'same-origin' });
});

test('logout 204 resolves without body and public token errors keep their payload', async () => {
  const { fetch } = fakeFetch([
    { status: 204 },
    { status: 410, body: { error: 'Token vencido' } },
    { status: 404, body: { error: 'Token inexistente o inválido' } }
  ]);
  let expiredCalls = 0;
  const api = createAdminApi({ fetch, onSessionExpired: () => { expiredCalls += 1; } });

  assert.equal(await api.logout(), undefined);

  await assert.rejects(
    api.tokenAction('tokenFicticio123', 'cancel'),
    (error: unknown) => error instanceof ApiError && error.status === 410 && (error as ApiError).body !== null
  );
  await assert.rejects(
    api.tokenInfo('tokenInexistente', 'confirm'),
    (error: unknown) => error instanceof ApiError && error.status === 404
  );
  assert.equal(expiredCalls, 0);
});
