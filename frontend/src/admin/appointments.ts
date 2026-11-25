import type { AgendaEntry } from './agenda.js';
import { visibleError } from './errors.js';

export type AgendaAppointment = AgendaEntry;
export type AppointmentStatus = 'CONFIRMED' | 'ABSENT' | 'ATTENDED' | 'CANCELLED';

export interface StatusRequest {
  status: AppointmentStatus;
  reason: string | null;
}

export type StatusOutcome =
  | { kind: 'updated'; status: string; message: string }
  | { kind: 'cancelled'; message: string }
  | { kind: 'already-cancelled'; message: string };

export function statusRequest(status: AppointmentStatus, reason?: string | null): StatusRequest {
  const trimmed = typeof reason === 'string' ? reason.trim() : '';

  return { status, reason: trimmed.length > 0 ? trimmed : null };
}

export function readStatusResponse(body: unknown): StatusOutcome | null {
  if (typeof body !== 'object' || body === null) {
    return null;
  }

  const response = body as { status?: unknown; cancelled?: unknown; message?: unknown };
  const message = typeof response.message === 'string' ? response.message : '';

  if (typeof response.status === 'string' && message === 'estado actualizado') {
    return { kind: 'updated', status: response.status, message };
  }

  if (response.cancelled === true && message === 'cancelado') {
    return { kind: 'cancelled', message };
  }

  if (response.cancelled === false && message === 'ya cancelado') {
    return { kind: 'already-cancelled', message };
  }

  return null;
}

export function statusErrorMessage(status: number, body: unknown): string {
  return visibleError(status, body);
}

export function applyOutcome(
  appointment: AgendaAppointment,
  outcome: StatusOutcome | null
): AgendaAppointment | null {
  if (outcome === null) {
    return appointment;
  }

  if (outcome.kind === 'updated') {
    return { ...appointment, status: outcome.status };
  }

  return null;
}
