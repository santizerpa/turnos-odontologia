export const GENERIC_ERROR_MESSAGE = 'No se pudo completar la solicitud. Intente nuevamente más tarde.';
export const NO_AUTHENTICATED_MESSAGE = 'No autenticado';
export const INVALID_CREDENTIALS_MESSAGE = 'Credenciales inválidas';

function readableMessage(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null;
  }

  const trimmed = value.trim();

  return trimmed.length > 0 ? trimmed : null;
}

export function backendMessage(body: unknown): string | null {
  if (typeof body !== 'object' || body === null) {
    return null;
  }

  const candidate = body as { error?: unknown; detail?: unknown };

  return readableMessage(candidate.error) ?? readableMessage(candidate.detail);
}

export function visibleError(status: number, body: unknown): string {
  return backendMessage(body) ?? GENERIC_ERROR_MESSAGE;
}
