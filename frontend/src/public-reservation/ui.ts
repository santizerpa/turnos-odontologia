import {
  ApiError,
  createAppointment,
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
import { extractToken } from './token.js';
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
      <section aria-labelledby="token-title">
        <h2 id="token-title">Ya tengo un turno</h2>
        <label>Token o enlace <input id="token-input" type="text"></label>
        <label>Acción
          <select id="token-flow">
            <option value="confirm">Confirmar turno</option>
            <option value="cancel">Cancelar turno</option>
          </select>
        </label>
        <button id="token-load" type="button">Consultar estado</button>
        <div id="token-status" role="status"></div>
        <button id="token-submit" type="button" hidden>Ejecutar acción</button>
        <div id="token-result" aria-live="polite"></div>
        <div id="token-error" role="alert"></div>
      </section>
    </main>`;

  const dateInput = find<HTMLInputElement>(root, '#booking-date');
  const slotsLoad = find<HTMLButtonElement>(root, '#slots-load');
  const slotsStatus = find<HTMLElement>(root, '#slots-status');
  const slotsList = find<HTMLElement>(root, '#slots-list');
  const bookingForm = find<HTMLFormElement>(root, '#booking-form');
  const bookingResult = find<HTMLElement>(root, '#booking-result');
  const bookingError = find<HTMLElement>(root, '#booking-error');
  const tokenInput = find<HTMLInputElement>(root, '#token-input');
  const tokenFlow = find<HTMLSelectElement>(root, '#token-flow');
  const tokenLoad = find<HTMLButtonElement>(root, '#token-load');
  const tokenStatus = find<HTMLElement>(root, '#token-status');
  const tokenSubmit = find<HTMLButtonElement>(root, '#token-submit');
  const tokenResult = find<HTMLElement>(root, '#token-result');
  const tokenError = find<HTMLElement>(root, '#token-error');
  const window = dateWindow(new Date());

  dateInput.min = window.min;
  dateInput.max = window.max;
  dateInput.value = window.min;
  slotsStatus.textContent = slotsStatusText('idle', 0);

  let selectedStart: string | null = null;
  let activeToken: string | null = null;

  function selectedTokenAction(): TokenAction {
    return tokenFlow.value === 'cancel' ? 'cancel' : 'confirm';
  }

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

  tokenLoad.addEventListener('click', async () => {
    clear(tokenStatus);
    clear(tokenResult);
    clear(tokenError);
    activeToken = null;
    tokenSubmit.hidden = true;

    const token = extractToken(tokenInput.value);

    if (token === null) {
      renderVisibleError(tokenError, {
        kind: 'invalid-token',
        message: 'Ingrese un token o enlace válido.',
        hint: 'Verifique el enlace o solicite uno nuevo al consultorio.'
      });
      return;
    }

    try {
      const status = await getTokenStatus(token, selectedTokenAction());

      activeToken = token;
      renderFeedback(tokenStatus, tokenStatusFeedback(formatStatus(status)));
      tokenSubmit.hidden = false;
    } catch (error) {
      renderVisibleError(tokenError, visibleFrom(error));
    }
  });

  tokenSubmit.addEventListener('click', async () => {
    if (activeToken === null) {
      return;
    }

    clear(tokenResult);
    clear(tokenError);

    const action = selectedTokenAction();

    try {
      const result = await postTokenAction(activeToken, action);

      renderFeedback(tokenResult, tokenActionFeedback(formatTokenAction(result), action));
    } catch (error) {
      renderFeedback(tokenError, tokenErrorFeedback(visibleFrom(error)));
    }
  });
}
