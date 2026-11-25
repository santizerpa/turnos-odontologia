import type { AdminApi } from './api.js';
import { ApiError } from './api.js';
import {
  dailyRange,
  loadAgenda,
  MISSING_RANGE_MESSAGE,
  NO_APPOINTMENTS_MESSAGE,
  validateAgendaRange,
  weeklyRange,
  type AgendaEntry,
  type AgendaRange,
  type PatientSummary
} from './agenda.js';
import {
  readStatusResponse,
  statusErrorMessage,
  type AppointmentStatus,
  type StatusOutcome
} from './appointments.js';
import { visibleError, INVALID_CREDENTIALS_MESSAGE } from './errors.js';
import { localDateTime, localTimeRange } from './local-time.js';
import { outboxErrorMessage, outboxView, tokenInfoText, type OutboxMessage } from './outbox.js';
import { patientQuery, patientsErrorMessage, viewPatientPage } from './patients.js';
import {
  anonymousSession,
  authenticatedSession,
  isSessionExpired,
  loggedOutSession,
  loginFailureMessage,
  sessionNotice,
  sessionScreen,
  sessionUnavailable,
  type DentistProfile,
  type SessionState
} from './session.js';

export interface LoginFeedback {
  visible: boolean;
  message: string | null;
  hint: string | null;
}

export interface AdminController {
  handleSessionExpired(status: number, body: unknown): void;
}

export function loginFeedback(message: string | null): LoginFeedback {
  if (message === null) {
    return { visible: false, message: null, hint: null };
  }

  return {
    visible: true,
    message,
    hint: 'Verifique sus datos e intente nuevamente.'
  };
}

export function profileSummary(profile: DentistProfile): string {
  return `${profile.firstName} ${profile.lastName} · ${profile.email}`;
}

export function screenSections(screen: 'login' | 'console'): string[] {
  return screen === 'console' ? ['agenda', 'patients', 'outbox'] : [];
}

export function sessionUnavailableNotice(state: SessionState): string | null {
  return sessionNotice(state);
}

export function agendaEntryText(entry: AgendaEntry): string {
  const start = localDateTime(entry.startAt);
  const slot = entry.activeSlot === null ? 'franja libre' : localTimeRange(entry.activeSlot, entry.durationMinutes);

  return `${start} · ${entry.patientName} · ${entry.durationMinutes} min · ${entry.status} · franja: ${slot}`;
}

export function agendaRowAttributes(entry: AgendaEntry): Record<'data-appointment-id' | 'data-status', string> {
  return { 'data-appointment-id': entry.id, 'data-status': entry.status };
}

export function agendaEmptyText(): string {
  return NO_APPOINTMENTS_MESSAGE;
}

export function statusActionLabels(): Array<{ status: AppointmentStatus; label: string }> {
  // Confirmation belongs to the patient (public token); the dentist only records outcomes or cancels.
  return [
    { status: 'ABSENT', label: 'Marcar ausente' },
    { status: 'ATTENDED', label: 'Marcar atendido' },
    { status: 'CANCELLED', label: 'Cancelar' }
  ];
}

export function statusOutcomeText(outcome: StatusOutcome | null): string | null {
  return outcome?.message ?? null;
}

export function patientRowText(patient: PatientSummary): string {
  const parts = [`${patient.nombre} ${patient.apellido}`];

  if (patient.dni !== undefined && patient.dni !== '') {
    parts.push(patient.dni);
  }

  if (patient.email !== undefined && patient.email !== '') {
    parts.push(patient.email);
  }

  return parts.join(' · ');
}

export function patientPageText(page: { page: number; size: number; totalElements: number; totalPages: number }): string {
  return `Página ${page.page + 1} de ${page.totalPages} · ${page.totalElements} pacientes`;
}

// The message status is not shown: it never changes after sending and misled as the appointment status,
// which is looked up with "Ver estado del turno".
export function outboxRowText(message: OutboxMessage): string {
  return [message.subject, message.recipientEmail, message.patientName, message.patientDni, localDateTime(message.createdAt)]
    .filter((value): value is string => typeof value === 'string' && value !== '')
    .join(' · ');
}

export function tokenUiLabels(): { info: string } {
  return { info: 'Ver estado del turno' };
}

export function tokenDetailsText(response: { startAt: string; status: string }): string {
  return tokenInfoText(response);
}

function node<K extends keyof HTMLElementTagNameMap>(tag: K, text?: string): HTMLElementTagNameMap[K] {
  const created = document.createElement(tag);

  if (text !== undefined) {
    created.textContent = text;
  }

  return created;
}

function feedbackBox(feedback: LoginFeedback): HTMLElement {
  const box = node('div');
  const message = node('strong', feedback.message ?? '');
  const hint = node('p', feedback.hint ?? '');

  box.setAttribute('role', 'alert');
  box.append(message, hint);

  return box;
}

function renderAgendaSection(section: HTMLElement, api: AdminApi, currentSession: () => SessionState): void {
  const heading = node('h2', 'Agenda');
  const dateLabel = node('label', 'Fecha ');
  const dateInput = node('input');
  const daily = node('button', 'Ver día');
  const weekly = node('button', 'Ver semana');
  const fromLabel = node('label', 'Desde (Instant) ');
  const fromInput = node('input');
  const toLabel = node('label', 'Hasta (Instant) ');
  const toInput = node('input');
  const rangeLoad = node('button', 'Cargar rango');
  const reasonLabel = node('label', 'Motivo de cancelación ');
  const reasonInput = node('input');
  const status = node('p');
  const list = node('div');
  const error = node('div');
  const actionStatus = node('p');
  const actionError = node('div');
  let lastRange: AgendaRange | null = null;

  dateInput.type = 'date';
  dateInput.value = new Date().toISOString().slice(0, 10);
  daily.type = 'button';
  weekly.type = 'button';
  rangeLoad.type = 'button';
  reasonLabel.append(reasonInput);
  status.setAttribute('role', 'status');
  error.setAttribute('role', 'alert');
  actionStatus.setAttribute('role', 'status');
  actionError.setAttribute('role', 'alert');
  dateLabel.append(dateInput);
  fromLabel.append(fromInput);
  toLabel.append(toInput);
  section.append(heading, dateLabel, daily, weekly, fromLabel, toLabel, rangeLoad, reasonLabel, status, list, error, actionStatus, actionError);

  async function load(range: AgendaRange): Promise<void> {
    lastRange = range;
    list.replaceChildren();
    error.replaceChildren();
    actionStatus.textContent = '';
    actionError.replaceChildren();

    if (validateAgendaRange(range.from, range.to) === MISSING_RANGE_MESSAGE) {
      status.textContent = MISSING_RANGE_MESSAGE;
      return;
    }

    status.textContent = 'Cargando agenda...';

    try {
      const view = await loadAgenda(api, range);

      if (currentSession().kind !== 'authenticated') {
        return;
      }

      if (view.empty) {
        status.textContent = view.message ?? agendaEmptyText();
        return;
      }

      status.textContent = '';

      for (const entry of view.entries) {
        const row = node('div');
        const actions = node('div');

        row.append(node('span', agendaEntryText(entry)), actions);
        for (const [name, value] of Object.entries(agendaRowAttributes(entry))) {
          row.setAttribute(name, value);
        }

        for (const action of statusActionLabels()) {
          const button = node('button', action.label);

          button.type = 'button';
          button.addEventListener('click', () => {
            void changeStatus(entry.id, action.status, action.status === 'CANCELLED' ? reasonInput.value : null);
          });
          actions.append(button);
        }

        list.append(row);
      }
    } catch (failure) {
      if (currentSession().kind !== 'authenticated') {
        return;
      }

      status.textContent = '';
      error.replaceChildren(
        node('p', failure instanceof ApiError ? statusErrorMessage(failure.status, failure.body) : visibleError(0, null))
      );
    }
  }

  async function changeStatus(id: string, status: AppointmentStatus, reason: string | null): Promise<void> {
    actionStatus.textContent = '';
    actionError.replaceChildren();

    try {
      const response = await api.changeStatus(id, status, reason);

      if (currentSession().kind !== 'authenticated') {
        return;
      }

      const message = statusOutcomeText(readStatusResponse(response));

      if (lastRange !== null) {
        await load(lastRange);
      }

      if (message !== null) {
        actionStatus.textContent = message;
      }
    } catch (failure) {
      if (currentSession().kind !== 'authenticated') {
        return;
      }

      actionError.replaceChildren(
        node('p', failure instanceof ApiError ? statusErrorMessage(failure.status, failure.body) : visibleError(0, null))
      );
    }
  }

  function setRange(range: AgendaRange): void {
    fromInput.value = range.from;
    toInput.value = range.to;
    void load(range);
  }

  daily.addEventListener('click', () => setRange(dailyRange(dateInput.value)));
  weekly.addEventListener('click', () => setRange(weeklyRange(dateInput.value)));
  rangeLoad.addEventListener('click', () => void load({ from: fromInput.value, to: toInput.value }));
}

function renderPatientsSection(section: HTMLElement, api: AdminApi, currentSession: () => SessionState): void {
  const heading = node('h2', 'Pacientes');
  const criterionLabel = node('label', 'Buscar por ');
  const criterionSelect = node('select');
  const termLabel = node('label', 'Búsqueda ');
  const termInput = node('input');
  const pageLabel = node('label', 'Página ');
  const pageInput = node('input');
  const sizeLabel = node('label', 'Tamaño ');
  const sizeInput = node('input');
  const load = node('button', 'Buscar pacientes');
  const summary = node('p');
  const list = node('div');
  const error = node('div');

  pageInput.type = 'number';
  pageInput.value = '0';
  sizeInput.type = 'number';
  sizeInput.value = '20';
  load.type = 'button';
  summary.setAttribute('role', 'status');
  error.setAttribute('role', 'alert');
  for (const [value, text] of [['dni', 'DNI'], ['nombre', 'Nombre']] as const) {
    const option = node('option', text);

    option.value = value;
    criterionSelect.append(option);
  }

  criterionLabel.append(criterionSelect);
  termLabel.append(termInput);
  pageLabel.append(pageInput);
  sizeLabel.append(sizeInput);
  section.append(heading, criterionLabel, termLabel, pageLabel, sizeLabel, load, summary, list, error);

  load.addEventListener('click', async () => {
    summary.textContent = '';
    list.replaceChildren();
    error.replaceChildren();

    const query = patientQuery({
      criterion: criterionSelect.value === 'nombre' ? 'nombre' : 'dni',
      term: termInput.value,
      page: pageInput.value,
      size: sizeInput.value
    });

    try {
      const response = await api.patients(query);

      if (currentSession().kind !== 'authenticated') {
        return;
      }

      const view = viewPatientPage(response, query);

      summary.textContent = patientPageText(view);

      for (const row of view.rows) {
        list.append(node('div', patientRowText(row)));
      }

      if (view.empty) {
        list.append(node('p', 'Sin pacientes para los filtros seleccionados.'));
      }
    } catch (failure) {
      if (currentSession().kind !== 'authenticated') {
        return;
      }

      error.replaceChildren(
        node('p', failure instanceof ApiError ? patientsErrorMessage(failure.status, failure.body) : visibleError(0, null))
      );
    }
  });
}

function renderOutboxSection(section: HTMLElement, api: AdminApi, currentSession: () => SessionState): void {
  const heading = node('h2', 'Bandeja de salida');
  const load = node('button', 'Cargar bandeja');
  const summary = node('p');
  const list = node('div');
  const error = node('div');
  const feedback = node('p');
  const labels = tokenUiLabels();

  load.type = 'button';
  summary.setAttribute('role', 'status');
  error.setAttribute('role', 'alert');
  feedback.setAttribute('role', 'status');
  section.append(heading, load, summary, list, error, feedback);

  function reportFailure(failure: unknown): void {
    if (currentSession().kind !== 'authenticated') {
      return;
    }

    feedback.textContent = '';
    error.replaceChildren(
      node('p', failure instanceof ApiError ? outboxErrorMessage(failure.status, failure.body) : visibleError(0, null))
    );
  }

  // The outbox is a read-only history: only the informative GET, never confirm/cancel POSTs.
  function statusButton(token: string): HTMLButtonElement {
    const button = node('button', labels.info);

    button.type = 'button';
    button.addEventListener('click', async () => {
      error.replaceChildren();
      feedback.textContent = '';

      try {
        const response = await api.tokenInfo(token, 'confirm');

        if (currentSession().kind === 'authenticated') {
          feedback.textContent = tokenDetailsText(response);
        }
      } catch (failure) {
        reportFailure(failure);
      }
    });

    return button;
  }

  load.addEventListener('click', async () => {
    summary.textContent = '';
    list.replaceChildren();
    error.replaceChildren();
    feedback.textContent = '';

    try {
      const messages = await api.outbox();

      if (currentSession().kind !== 'authenticated') {
        return;
      }

      const view = outboxView(messages);

      summary.textContent = view.empty ? view.message ?? '' : `${view.rows.length} mensajes`;

      for (const message of view.rows) {
        const row = node('div');
        const actions = node('div');

        row.append(node('span', outboxRowText(message)), actions);

        const lookup = view.statusLookups.find((candidate) => candidate.messageId === message.id);

        if (lookup !== undefined) {
          actions.append(statusButton(lookup.token));
        }

        list.append(row);
      }
    } catch (failure) {
      reportFailure(failure);
    }
  });
}

export function mountAdminConsole(root: HTMLElement, api: AdminApi): AdminController {
  let session: SessionState = anonymousSession();

  function renderLogin(): void {
    const form = node('form');
    const emailLabel = node('label', 'Email ');
    const email = node('input');
    const passwordLabel = node('label', 'Contraseña ');
    const password = node('input');
    const submit = node('button', 'Ingresar');
    const error = node('div');

    email.type = 'email';
    email.name = 'email';
    password.type = 'password';
    password.name = 'password';
    submit.type = 'submit';
    emailLabel.append(email);
    passwordLabel.append(password);
    form.append(emailLabel, passwordLabel, submit);

    const notice = sessionUnavailableNotice(session);

    root.replaceChildren(node('h2', 'Acceso administrativo'));

    if (notice !== null) {
      root.append(node('p', notice));
    }

    root.append(form, error);

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      error.replaceChildren();

      try {
        const response = await api.login(email.value, password.value);
        session = authenticatedSession(response);
        render();
      } catch (failure) {
        const status = failure instanceof ApiError ? failure.status : 0;
        const body = failure instanceof ApiError ? failure.body : null;
        const message =
          loginFailureMessage(status, body) ??
          (status === 401 ? INVALID_CREDENTIALS_MESSAGE : visibleError(status, body));

        error.replaceChildren(feedbackBox(loginFeedback(message)));
      }
    });
  }

  function renderConsole(): void {
    const header = node('div');

    if (session.kind === 'authenticated') {
      header.append(node('strong', profileSummary(session.profile)));
    }

    const logout = node('button', 'Cerrar sesión');
    logout.type = 'button';
    logout.addEventListener('click', async () => {
      try {
        await api.logout();
        session = loggedOutSession();
        render();
      } catch (failure) {
        if (failure instanceof ApiError && isSessionExpired(failure.status, failure.body)) {
          session = sessionUnavailable(session, failure.status, failure.body).state;
          render();
        }
      }
    });
    header.append(logout);
    root.replaceChildren(header);

    for (const id of screenSections('console')) {
      const section = node('section');

      section.setAttribute('data-section', id);

      if (id === 'agenda') {
        renderAgendaSection(section, api, () => session);
      } else if (id === 'patients') {
        renderPatientsSection(section, api, () => session);
      } else if (id === 'outbox') {
        renderOutboxSection(section, api, () => session);
      }

      root.append(section);
    }
  }

  function render(): void {
    if (sessionScreen(session) === 'login') {
      renderLogin();
    } else {
      renderConsole();
    }
  }

  render();

  return {
    handleSessionExpired(status: number, body: unknown): void {
      session = sessionUnavailable(session, status, body).state;
      render();
    }
  };
}
