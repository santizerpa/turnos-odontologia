import { backendMessage, INVALID_CREDENTIALS_MESSAGE, NO_AUTHENTICATED_MESSAGE } from './errors.js';

export interface DentistProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export type SessionState =
  | { readonly kind: 'anonymous' }
  | { readonly kind: 'authenticated'; readonly profile: DentistProfile }
  | { readonly kind: 'session-unavailable' };

export interface SessionTransition {
  state: SessionState;
  discardedAction: boolean;
  redirect: false;
  retry: false;
}

export function anonymousSession(): SessionState {
  return { kind: 'anonymous' };
}

export function authenticatedSession(input: unknown): SessionState {
  if (typeof input !== 'object' || input === null) {
    return anonymousSession();
  }

  const candidate = input as Record<string, unknown>;
  const profile: DentistProfile = {
    id: typeof candidate.id === 'string' ? candidate.id : '',
    email: typeof candidate.email === 'string' ? candidate.email : '',
    firstName: typeof candidate.firstName === 'string' ? candidate.firstName : '',
    lastName: typeof candidate.lastName === 'string' ? candidate.lastName : ''
  };

  if (profile.id.length === 0 || profile.email.length === 0) {
    return anonymousSession();
  }

  return { kind: 'authenticated', profile };
}

export function isSessionExpired(status: number, body: unknown): boolean {
  return status === 401 && backendMessage(body) === NO_AUTHENTICATED_MESSAGE;
}

export function sessionUnavailable(previous: SessionState, status: number, body: unknown): SessionTransition {
  if (!isSessionExpired(status, body)) {
    return { state: previous, discardedAction: false, redirect: false, retry: false };
  }

  return { state: { kind: 'session-unavailable' }, discardedAction: true, redirect: false, retry: false };
}

export function loggedOutSession(): SessionState {
  return anonymousSession();
}

export function loginFailureMessage(status: number, body: unknown): string | null {
  if (status === 401 && backendMessage(body) === INVALID_CREDENTIALS_MESSAGE) {
    return INVALID_CREDENTIALS_MESSAGE;
  }

  return null;
}

export function sessionScreen(state: SessionState): 'login' | 'console' {
  return state.kind === 'authenticated' ? 'console' : 'login';
}

export function sessionNotice(state: SessionState): string | null {
  return state.kind === 'session-unavailable'
    ? 'Su sesión no está disponible. Ingrese nuevamente.'
    : null;
}
