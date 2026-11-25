import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  anonymousSession,
  authenticatedSession,
  isSessionExpired,
  loginFailureMessage,
  loggedOutSession,
  sessionNotice,
  sessionScreen,
  sessionUnavailable,
  type DentistProfile
} from './session.js';

const profile: DentistProfile = {
  id: 'dentist-1',
  email: 'odontologo@example.invalid',
  firstName: 'Demo',
  lastName: 'Ficticio'
};

test('session starts anonymous and login keeps only the profile returned by the API', () => {
  assert.deepEqual(anonymousSession(), { kind: 'anonymous' });
  assert.deepEqual(authenticatedSession(profile), { kind: 'authenticated', profile });
  assert.deepEqual(
    authenticatedSession({ ...profile, passwordHash: 'must-not-be-shown', password: 'secret' }),
    { kind: 'authenticated', profile }
  );
});

test('a 401 No autenticado discards the action without redirecting or retrying', () => {
  const before = authenticatedSession(profile);
  const transition = sessionUnavailable(before, 401, { error: 'No autenticado' });

  assert.deepEqual(transition.state, { kind: 'session-unavailable' });
  assert.equal(transition.discardedAction, true);
  assert.equal(transition.redirect, false);
  assert.equal(transition.retry, false);
  assert.deepEqual(before, authenticatedSession(profile));
});

test('a login 401 is not a session expiry and reports only the generic invalid credentials message', () => {
  const before = authenticatedSession(profile);
  const transition = sessionUnavailable(before, 401, { error: 'Credenciales inválidas' });

  assert.equal(isSessionExpired(401, { error: 'Credenciales inválidas' }), false);
  assert.deepEqual(transition.state, before);
  assert.equal(transition.discardedAction, false);
  assert.equal(loginFailureMessage(401, { error: 'Credenciales inválidas' }), 'Credenciales inválidas');
  assert.equal(loginFailureMessage(401, { error: 'No autenticado' }), null);
});

test('non-401 failures do not discard the action or change the session', () => {
  for (const status of [400, 404, 409, 422, 500]) {
    const transition = sessionUnavailable(authenticatedSession(profile), status, { error: 'otro' });

    assert.deepEqual(transition.state, authenticatedSession(profile));
    assert.equal(transition.discardedAction, false);
    assert.equal(transition.redirect, false);
    assert.equal(transition.retry, false);
  }
});

test('logout 204 returns to the initial state and a later authenticated 401 shows login again', () => {
  const afterLogout = loggedOutSession();

  assert.deepEqual(afterLogout, anonymousSession());
  assert.deepEqual(sessionScreen(afterLogout), 'login');

  const transition = sessionUnavailable(afterLogout, 401, { error: 'No autenticado' });

  assert.deepEqual(sessionScreen(transition.state), 'login');
  assert.equal(transition.discardedAction, true);
  assert.equal(sessionScreen(authenticatedSession(profile)), 'console');
});

test('session-unavailable shows the same login document with a notice until a new login', () => {
  const unavailable = sessionUnavailable(anonymousSession(), 401, { error: 'No autenticado' }).state;

  assert.deepEqual(sessionScreen(unavailable), 'login');
  assert.equal(sessionNotice(unavailable), 'Su sesión no está disponible. Ingrese nuevamente.');
  assert.equal(sessionNotice(anonymousSession()), null);
  assert.deepEqual(
    sessionUnavailable(unavailable, 401, { error: 'No autenticado' }).state,
    unavailable
  );
  assert.deepEqual(sessionScreen(authenticatedSession(profile)), 'console');
});
