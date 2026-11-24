import type { Slot } from './api.js';
import type { TokenAction } from './endpoints.js';
import type { VisibleError } from './errors.js';
import type {
  AppointmentStatusView,
  ConfirmationView,
  TokenActionView
} from './payload.js';

export type SlotsState = 'idle' | 'loading' | 'loaded';
export type FeedbackTone = 'error' | 'info' | 'success';

export interface Feedback {
  tone: FeedbackTone;
  title: string;
  message: string;
  hint: string;
}

function timePart(value: string): string {
  const match = /T(\d{2}:\d{2})/.exec(value);

  return match ? match[1] : value.trim();
}

export function slotLabel(slot: Slot): string {
  return `${timePart(slot.start)} - ${timePart(slot.end)}`;
}

export function slotsStatusText(state: SlotsState, count: number): string {
  switch (state) {
    case 'idle':
      return 'Elija una fecha dentro de la ventana permitida.';
    case 'loading':
      return 'Cargando horarios...';
    default:
      return count === 0
        ? 'No hay horarios libres para esa fecha.'
        : `Seleccione uno de los ${count} horarios libres.`;
  }
}

function defaultErrorTitle(kind: VisibleError['kind']): string {
  switch (kind) {
    case 'conflict':
      return 'No se pudo reservar el turno';
    case 'validation':
      return 'Revise los datos ingresados';
    case 'invalid-token':
    case 'expired-token':
      return 'No se pudo consultar el enlace';
    default:
      return 'No se pudo completar la solicitud';
  }
}

export function errorFeedback(visible: VisibleError, title?: string): Feedback {
  return {
    tone: 'error',
    title: title ?? defaultErrorTitle(visible.kind),
    message: visible.message,
    hint: visible.hint
  };
}

export function tokenStatusFeedback(view: AppointmentStatusView): Feedback {
  return {
    tone: 'info',
    title: 'Estado del turno',
    message: `Inicio: ${view.start} · Estado: ${view.status}`,
    hint: ''
  };
}

export function tokenErrorFeedback(visible: VisibleError): Feedback {
  return {
    tone: 'error',
    title: 'No se pudo completar la acción.',
    message: visible.message,
    hint: visible.hint
  };
}

export function tokenActionFeedback(view: TokenActionView, action: TokenAction): Feedback {
  return {
    tone: 'success',
    title: action === 'confirm' ? 'Turno confirmado' : 'Turno cancelado',
    message: `${view.message} · Estado: ${view.status}`,
    hint: ''
  };
}

export function bookingConfirmationFeedback(view: ConfirmationView): Feedback {
  return {
    tone: 'success',
    title: 'Turno reservado',
    message: `Inicio: ${view.start} · Duración: ${view.duration} · Estado: ${view.status}`,
    hint: ''
  };
}
