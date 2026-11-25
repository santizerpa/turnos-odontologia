export type TokenAction = 'confirm' | 'cancel';

export function slotsPath(date: string): string {
  return `/api/public/slots?date=${encodeURIComponent(date)}`;
}

export function appointmentsPath(): string {
  return '/api/public/appointments';
}

export function tokenActionPath(token: string, action: TokenAction): string {
  return `${appointmentsPath()}/${encodeURIComponent(token)}/${action}`;
}

export function inboxPath(email: string): string {
  return `/api/public/inbox?email=${encodeURIComponent(email.trim())}`;
}
