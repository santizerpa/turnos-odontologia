import { visibleError } from './errors.js';

export const NO_ENTRIES_MESSAGE = 'Sin entradas en la lista de espera.';

export interface WaitingEntry {
  id: string;
  nombre: string;
  apellido: string;
  email: string;
  requestedDateFrom: string;
  requestedDateTo: string;
  status: string;
}

export interface WaitingView {
  rows: WaitingEntry[];
  empty: boolean;
  message: string | null;
  hasAssignmentActions: false;
}

export function waitingListView(entries: WaitingEntry[]): WaitingView {
  const rows = entries.map((entry) => ({
    id: entry.id,
    nombre: entry.nombre,
    apellido: entry.apellido,
    email: entry.email,
    requestedDateFrom: entry.requestedDateFrom,
    requestedDateTo: entry.requestedDateTo,
    status: entry.status
  }));

  return {
    rows,
    empty: rows.length === 0,
    message: rows.length === 0 ? NO_ENTRIES_MESSAGE : null,
    hasAssignmentActions: false
  };
}

export function waitingListErrorMessage(status: number, body: unknown): string {
  return visibleError(status, body);
}
