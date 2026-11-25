import {
  ApiError,
  createAppointment,
  fetchInbox,
  fetchSlots,
  getTokenStatus,
  postTokenAction,
  type BookingConfirmation,
  type Slot
} from './api.js';
import { dateWindow, isDateInWindow } from './dates.js';
import type { TokenAction } from './endpoints.js';
import { mapApiError, type VisibleError } from './errors.js';
import {
  buildBookingPayload,
  formatConfirmation,
  formatStatus,
  formatTokenAction,
  type PatientFormValues
} from './payload.js';
import { INBOX_EMPTY_MESSAGE, inboxErrorMessage, inboxMessageView, type InboxMessage } from './inbox.js';
import { validateBookingForm } from './validation.js';
import {
  bookingConfirmationFeedback,
  errorFeedback,
  slotLabel,
  slotsStatusText,
  tokenActionFeedback,
  tokenErrorFeedback,
  tokenStatusFeedback,
  type Feedback
} from './views.js';

function find<T extends HTMLElement>(root: ParentNode, selector: string): T {
  const element = root.querySelector<T>(selector);

  if (!element) {
    throw new Error(`No se encontró el elemento ${selector}`);
  }

  return element;
}

function clear(container: HTMLElement): void {
  container.replaceChildren();
}

function renderFeedback(container: HTMLElement, feedback: Feedback): void {
  const box = document.createElement('div');
  const heading = document.createElement('strong');
  const message = document.createElement('p');

  box.className = `feedback feedback-${feedback.tone}`;
  heading.textContent = feedback.title;
  message.textContent = feedback.message;
  box.append(heading, message);

  if (feedback.hint.length > 0) {
    const hint = document.createElement('p');

    hint.textContent = feedback.hint;
    box.append(hint);
  }

  container.replaceChildren(box);
}

function renderVisibleError(container: HTMLElement, visible: VisibleError, title?: string): void {
  renderFeedback(container, errorFeedback(visible, title));
}

function visibleFrom(error: unknown): VisibleError {
  if (error instanceof ApiError) {
    return mapApiError(error.status, error.body);
  }

  return mapApiError(0, null);
}

export function mountPublicBooking(root: HTMLElement): void {
  root.innerHTML = `
    <main>
      <h1>Reserva de turnos</h1>
      <section aria-labelledby="booking-title">
        <h2 id="booking-title">Reservar un turno</h2>
        <label>Fecha <input id="booking-date" type="date"></label>
        <button id="slots-load" type="button">Consultar horarios</button>
        <p id="slots-status" role="status"></p>
        <div id="slots-list"></div>
        <form id="booking-form">
          <label>Nombre <input id="patient-nombre" name="nombre" autocomplete="given-name" required></label>
          <label>Apellido <input id="patient-apellido" name="apellido" autocomplete="family-name" required></label>
          <label>DNI <input id="patient-dni" name="dni" required></label>
          <label>Email <input id="patient-email" name="email" type="email" autocomplete="email" required></label>
          <button id="booking-submit" type="submit">Reservar turno</button>
        </form>
        <div id="booking-result" aria-live="polite"></div>
        <div id="booking-error" role="alert"></div>
      </section>
      <section aria-labelledby="inbox-title">
        <h2 id="inbox-title">Bandeja de entrada simulada (correo del paciente)</h2>
        <p>Simulación de demo: muestra los correos que recibiría el paciente. No se envía correo real.</p>
        <label>Email del paciente <input id="inbox-email" type="email" autocomplete="email"></label>
        <button id="inbox-load" type="button">Ver mensajes</button>
        <p id="inbox-status" role="status"></p>
        <div id="inbox-list"></div>
        <div id="inbox-error" role="alert"></div>
      </section>
    </main>`;

  const dateInput = find<HTMLInputElement>(root, '#booking-date');
  const slotsLoad = find<HTMLButtonElement>(root, '#slots-load');
  const slotsStatus = find<HTMLElement>(root, '#slots-status');
  const slotsList = find<HTMLElement>(root, '#slots-list');
  const bookingForm = find<HTMLFormElement>(root, '#booking-form');
  const bookingResult = find<HTMLElement>(root, '#booking-result');
  const bookingError = find<HTMLElement>(root, '#booking-error');
  const inboxEmail = find<HTMLInputElement>(root, '#inbox-email');
  const inboxLoad = find<HTMLButtonElement>(root, '#inbox-load');
  const inboxStatus = find<HTMLElement>(root, '#inbox-status');
  const inboxList = find<HTMLElement>(root, '#inbox-list');
  const inboxError = find<HTMLElement>(root, '#inbox-error');
  const window = dateWindow(new Date());

  dateInput.min = window.min;
  dateInput.max = window.max;
  dateInput.value = window.min;
  slotsStatus.textContent = slotsStatusText('idle', 0);

  let selectedStart: string | null = null;

  function selectSlot(start: string): void {
    selectedStart = start;

    for (const button of slotsList.querySelectorAll<HTMLButtonElement>('button[data-start]')) {
      button.setAttribute('aria-pressed', String(button.dataset.start === start));
    }
  }

  slotsLoad.addEventListener('click', async () => {
    clear(slotsList);
    clear(bookingResult);
    clear(bookingError);
    selectedStart = null;

    if (!isDateInWindow(dateInput.value, new Date())) {
      slotsStatus.textContent = 'La fecha debe estar dentro de la ventana permitida.';
      return;
    }

    slotsStatus.textContent = slotsStatusText('loading', 0);

    try {
      const response = await fetchSlots(dateInput.value);
      const slots: Slot[] = Array.isArray(response.slots) ? response.slots : [];

      for (const slot of slots) {
        const button = document.createElement('button');

        button.type = 'button';
        button.textContent = slotLabel(slot);
        button.dataset.start = slot.start;
        button.setAttribute('aria-pressed', 'false');
        button.addEventListener('click', () => selectSlot(slot.start));
        slotsList.append(button);
      }

      slotsStatus.textContent = slotsStatusText('loaded', slots.length);
    } catch (error) {
      slotsStatus.textContent = '';
      renderVisibleError(bookingError, visibleFrom(error), 'No se pudieron cargar los horarios.');
    }
  });

  bookingForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clear(bookingResult);
    clear(bookingError);

    const values: PatientFormValues = {
      nombre: find<HTMLInputElement>(root, '#patient-nombre').value,
      apellido: find<HTMLInputElement>(root, '#patient-apellido').value,
      dni: find<HTMLInputElement>(root, '#patient-dni').value,
      email: find<HTMLInputElement>(root, '#patient-email').value
    };
    const validationMessage = validateBookingForm(values, selectedStart);

    if (validationMessage !== null) {
      renderVisibleError(bookingError, {
        kind: 'validation',
        message: validationMessage,
        hint: 'Corrija la selección o los datos e intente nuevamente.'
      });
      return;
    }

    try {
      const confirmation: BookingConfirmation = await createAppointment(
        buildBookingPayload(values, selectedStart ?? '')
      );

      renderFeedback(bookingResult, bookingConfirmationFeedback(formatConfirmation(confirmation)));
    } catch (error) {
      renderVisibleError(bookingError, visibleFrom(error));
    }
  });

  // Actions of each simulated-inbox message: GET only informs, POST acts.
  async function showStatus(token: string, status: HTMLElement, failure: HTMLElement): Promise<void> {
    try {
      renderFeedback(status, tokenStatusFeedback(formatStatus(await getTokenStatus(token, 'confirm'))));
    } catch (error) {
      renderVisibleError(failure, visibleFrom(error));
    }
  }

  async function runAction(token: string, action: TokenAction, result: HTMLElement, failure: HTMLElement): Promise<void> {
    try {
      renderFeedback(result, tokenActionFeedback(formatTokenAction(await postTokenAction(token, action)), action));
    } catch (error) {
      renderFeedback(failure, tokenErrorFeedback(visibleFrom(error)));
    }
  }

  function inboxEntry(message: InboxMessage): HTMLElement {
    const view = inboxMessageView(message);
    const entry = document.createElement('article');
    const header = document.createElement('strong');
    const outcome = document.createElement('div');
    const failure = document.createElement('div');

    header.textContent = view.header;
    entry.append(header);

    if (view.appointment !== null) {
      const appointment = document.createElement('p');

      appointment.textContent = view.appointment;
      entry.append(appointment);
    }

    if (view.token !== null) {
      const token = view.token;
      const actions = document.createElement('div');
      const buttons: Array<[string, () => void]> = [
        ['Ver estado del turno', () => void showStatus(token, outcome, failure)],
        ['Confirmar turno', () => void runAction(token, 'confirm', outcome, failure)],
        ['Cancelar turno', () => void runAction(token, 'cancel', outcome, failure)]
      ];

      for (const [label, handler] of buttons) {
        const button = document.createElement('button');

        button.type = 'button';
        button.textContent = label;
        button.addEventListener('click', () => {
          clear(outcome);
          clear(failure);
          handler();
        });
        actions.append(button);
      }

      entry.append(actions);
    }

    outcome.setAttribute('aria-live', 'polite');
    failure.setAttribute('role', 'alert');
    entry.append(outcome, failure);

    return entry;
  }

  inboxLoad.addEventListener('click', async () => {
    clear(inboxList);
    clear(inboxError);
    inboxStatus.textContent = 'Cargando mensajes...';

    try {
      const messages = await fetchInbox(inboxEmail.value);

      inboxStatus.textContent =
        messages.length === 0 ? INBOX_EMPTY_MESSAGE : `${messages.length} ${messages.length === 1 ? 'mensaje' : 'mensajes'}`;

      for (const message of messages) {
        inboxList.append(inboxEntry(message));
      }
    } catch (error) {
      inboxStatus.textContent = '';
      const message =
        error instanceof ApiError ? inboxErrorMessage(error.status, error.body) : inboxErrorMessage(0, null);

      renderFeedback(inboxError, { tone: 'error', title: 'No se pudo abrir la bandeja simulada', message, hint: '' });
    }
  });
}
