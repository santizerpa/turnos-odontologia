import { localDateTime } from './local-time.js';
import { extractToken } from './token.js';

export const INBOX_EMPTY_MESSAGE = 'No hay mensajes para este email.';

/** Entry of the demo-only simulated patient inbox (GET /api/public/inbox). */
export interface InboxMessage {
  id: string;
  subject: string;
  recipientEmail: string;
  patientDni: string | null;
  appointmentStartAt: string | null;
  appointmentDurationMinutes: number | null;
  createdAt: string;
  body: string;
}

export interface InboxMessageView {
  header: string;
  appointment: string | null;
  token: string | null;
}

const INBOX_OWN_ERRORS = new Set(['Bandeja simulada deshabilitada', 'Email inválido']);
const INBOX_UNAVAILABLE = 'La bandeja simulada no está disponible en este servidor.';
const INBOX_FALLBACK = 'No se pudo abrir la bandeja. Intente nuevamente más tarde.';

/**
 * Inbox-specific messages: the patient only types an e-mail, so errors never talk about links or tokens.
 * A generic 404 (for example a backend started before the inbox route existed) reads as "not available".
 */
export function inboxErrorMessage(status: number, body: unknown): string {
  const error = typeof body === 'object' && body !== null ? (body as { error?: unknown }).error : undefined;

  if (typeof error === 'string' && INBOX_OWN_ERRORS.has(error)) {
    return error;
  }

  return status === 404 ? INBOX_UNAVAILABLE : INBOX_FALLBACK;
}

const LINK = /\/api\/public\/appointments\/[A-Za-z0-9_-]+\/(?:confirm|cancel)/;

export function inboxMessageView(message: InboxMessage): InboxMessageView {
  const header = [message.subject, message.recipientEmail, message.patientDni, localDateTime(message.createdAt)]
    .filter((part): part is string => typeof part === 'string' && part.length > 0)
    .join(' · ');
  const start = message.appointmentStartAt === null ? null : localDateTime(message.appointmentStartAt);
  const appointment =
    start === null
      ? null
      : `Turno: ${start}${message.appointmentDurationMinutes === null ? '' : ` · ${message.appointmentDurationMinutes} minutos`}`;
  const link = LINK.exec(message.body);

  return { header, appointment, token: link === null ? null : extractToken(link[0]) };
}
