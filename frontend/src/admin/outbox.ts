import { visibleError } from './errors.js';

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
}

export interface TokenLink {
  action: TokenAction;
  token: string;
  href: string;
}

export interface OutboxView {
  rows: OutboxMessage[];
  links: Array<TokenLink & { messageId: string }>;
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
    sentAt: message.sentAt ?? null
  }));
  const links = messages.flatMap((message) =>
    extractTokenLinks(message.body).map((link) => ({ ...link, messageId: message.id }))
  );

  return {
    rows,
    links,
    empty: rows.length === 0,
    message: rows.length === 0 ? NO_MESSAGES_MESSAGE : null
  };
}

export function tokenInfoText(response: { startAt: string; status: string }): string {
  return `Inicio: ${response.startAt} · Estado: ${response.status}`;
}

export function tokenActionText(response: { message: string; status: string }): string {
  return `${response.message} · Estado: ${response.status}`;
}

export function outboxErrorMessage(status: number, body: unknown): string {
  return visibleError(status, body);
}
