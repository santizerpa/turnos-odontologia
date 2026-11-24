export const GENERIC_FALLBACK = 'No se pudo completar la solicitud. Intente nuevamente más tarde.';

export type ErrorKind =
  | 'conflict'
  | 'validation'
  | 'invalid-token'
  | 'expired-token'
  | 'service-error'
  | 'unknown';

export interface VisibleError {
  kind: ErrorKind;
  message: string;
  hint: string;
}

export function readBackendError(body: unknown): string | null {
  if (typeof body !== 'object' || body === null) {
    return null;
  }

  const error = (body as { error?: unknown }).error;

  if (typeof error !== 'string' || error.trim().length === 0) {
    return null;
  }

  return error.trim();
}

export function mapApiError(status: number, body: unknown): VisibleError {
  const message = readBackendError(body) ?? GENERIC_FALLBACK;

  switch (status) {
    case 409:
      return {
        kind: 'conflict',
        message,
        hint: 'Elija otra franja o contacte al consultorio.'
      };
    case 422:
      return {
        kind: 'validation',
        message,
        hint: 'Corrija los datos o la fecha; si la franja ya no está disponible, elija otra.'
      };
    case 404:
      return {
        kind: 'invalid-token',
        message,
        hint: 'Verifique el enlace o solicite uno nuevo al consultorio.'
      };
    case 410:
      return {
        kind: 'expired-token',
        message,
        hint: 'El enlace venció: solicite uno nuevo al consultorio.'
      };
    case 503:
    case 500:
      return {
        kind: 'service-error',
        message,
        hint: 'Intente nuevamente más tarde.'
      };
    default:
      return {
        kind: 'unknown',
        message,
        hint: 'Intente nuevamente más tarde.'
      };
  }
}
