import { localDateTime } from './local-time.js';
import type {
  AppointmentStatus,
  BookingConfirmation,
  BookingPayload,
  TokenActionResult
} from './api.js';

export interface PatientFormValues {
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
}

export interface ConfirmationView {
  start: string;
  duration: string;
  status: string;
}

export interface AppointmentStatusView {
  start: string;
  status: string;
}

export interface TokenActionView {
  message: string;
  status: string;
}

export function normalizeDni(value: string): string {
  return value.replace(/[\s.]/g, '');
}

export function buildBookingPayload(values: PatientFormValues, startAt: string): BookingPayload {
  return {
    nombre: values.nombre.trim(),
    apellido: values.apellido.trim(),
    dni: normalizeDni(values.dni),
    email: values.email.trim(),
    startAt: startAt.trim()
  };
}

export function formatStartAt(value: string): string {
  const trimmed = value.trim();

  if (trimmed.length === 0) {
    return '—';
  }

  return localDateTime(trimmed) ?? trimmed;
}

export function formatConfirmation(value: BookingConfirmation): ConfirmationView {
  return {
    start: formatStartAt(value.startAt),
    duration: `${value.durationMinutes} minutos`,
    status: value.status
  };
}

export function formatStatus(value: AppointmentStatus): AppointmentStatusView {
  return {
    start: formatStartAt(value.startAt),
    status: value.status
  };
}

export function formatTokenAction(value: TokenActionResult): TokenActionView {
  const message = value.message.trim();

  return {
    message: message.length > 0 ? message : 'Acción completada.',
    status: value.status
  };
}
