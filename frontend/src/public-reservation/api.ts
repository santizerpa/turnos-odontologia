import { appointmentsPath, inboxPath, slotsPath, tokenActionPath, type TokenAction } from './endpoints.js';
import type { InboxMessage } from './inbox.js';

export interface BookingPayload {
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  startAt: string;
}

export interface Slot {
  start: string;
  end: string;
}

export interface SlotsResponse {
  slots: Slot[];
}

export interface BookingConfirmation {
  startAt: string;
  durationMinutes: number;
  status: string;
}

export interface AppointmentStatus {
  startAt: string;
  status: string;
}

export interface TokenActionResult {
  message: string;
  status: string;
}

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly body: unknown) {
    super(`HTTP ${status}`);
  }
}

async function readBody(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...init
  });
  const body = await readBody(response);

  if (!response.ok) {
    throw new ApiError(response.status, body);
  }

  return body as T;
}

export function fetchSlots(date: string): Promise<SlotsResponse> {
  return request<SlotsResponse>(slotsPath(date));
}

export function createAppointment(payload: BookingPayload): Promise<BookingConfirmation> {
  return request<BookingConfirmation>(appointmentsPath(), {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function getTokenStatus(token: string, action: TokenAction): Promise<AppointmentStatus> {
  return request<AppointmentStatus>(tokenActionPath(token, action));
}

export function postTokenAction(token: string, action: TokenAction): Promise<TokenActionResult> {
  return request<TokenActionResult>(tokenActionPath(token, action), { method: 'POST' });
}

export function fetchInbox(email: string): Promise<InboxMessage[]> {
  return request<InboxMessage[]>(inboxPath(email));
}
