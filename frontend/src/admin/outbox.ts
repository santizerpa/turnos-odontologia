import { visibleError } from './errors.js';
import { localDateTime } from './local-time.js';

export const NO_MESSAGES_MESSAGE = 'Sin mensajes en la bandeja.';

export type TokenAction = 'confirm' | 'cancel';

export interface OutboxMessage {
  id: string;
  appointmentId: string | null;
  recipientEmail: string;
  subject: string;
  body: string;
  status: string;
  type: string;
  createdAt: string;
  sentAt: string | null;
  patientName: string | null;
  patientDni: string | null;
}

export interface TokenLink {
  action: TokenAction;
  token: string;
  href: string;
}

export interface OutboxView {
  rows: OutboxMessage[];
  links: Array<TokenLink & { messageId: string }>;
  /** One informative status lookup per message (the outbox is a read-only history). */
  statusLookups: Array<{ messageId: string; token: string }>;
  empty: boolean;
  message: string | null;
}

export function extractTokenLinks(body: string): TokenLink[] {
  const links: TokenLink[] = [];
  const pattern = /(^|[^\w/])(\/api\/public\/appointments\/([A-Za-z0-9_-]+)\/(confirm|cancel))(?![A-Za-z0-9_-])/g;

  for (const match of body.matchAll(pattern)) {
    links.push({
      action: match[4] as TokenAction,
      token: match[3],
      href: match[2]
    });
  }

  return links;
}

export function outboxView(messages: OutboxMessage[]): OutboxView {
  const rows = messages.map((message) => ({
    id: message.id,
    appointmentId: message.appointmentId ?? null,
    recipientEmail: message.recipientEmail,
    subject: message.subject,
    body: message.body,
    status: message.status,
    type: message.type,
    createdAt: message.createdAt,
    sentAt: message.sentAt ?? null,
    patientName: message.patientName ?? null,
    patientDni: message.patientDni ?? null
  }));
  const links = messages.flatMap((message) =>
    extractTokenLinks(message.body).map((link) => ({ ...link, messageId: message.id }))
  );

  const statusLookups = messages.flatMap((message) => {
    const first = extractTokenLinks(message.body)[0];

    return first === undefined ? [] : [{ messageId: message.id, token: first.token }];
  });

  return {
    rows,
    links,
    statusLookups,
    empty: rows.length === 0,
    message: rows.length === 0 ? NO_MESSAGES_MESSAGE : null
  };
}

export function tokenInfoText(response: { startAt: string; status: string }): string {
  return `Inicio: ${localDateTime(response.startAt)} · Estado: ${response.status}`;
}

export function outboxErrorMessage(status: number, body: unknown): string {
  return visibleError(status, body);
}
