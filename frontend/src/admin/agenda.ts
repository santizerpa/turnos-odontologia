import { isSessionExpired } from './session.js';

export const NO_APPOINTMENTS_MESSAGE = 'Sin turnos en el rango seleccionado.';
export const INVALID_RANGE_MESSAGE = 'Rango de agenda inválido';
export const MISSING_RANGE_MESSAGE = 'Parámetros from y to inválidos o faltantes';

export interface AgendaRange {
  from: string;
  to: string;
}

export interface Appointment {
  id: string;
  patientId: string;
  startAt: string;
  durationMinutes: number;
  status: string;
  activeSlot: string | null;
  dentistId?: string;
}

export interface PatientSummary {
  id: string;
  dni?: string;
  nombre: string;
  apellido: string;
  email?: string;
}

export interface PatientPage {
  content: PatientSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PatientQuery {
  dni?: string;
  nombre?: string;
  page: number;
  size: number;
}

export interface AgendaApi {
  appointments(from: string, to: string): Promise<Appointment[]>;
  patients(query: PatientQuery): Promise<PatientPage>;
}

export interface AgendaEntry {
  id: string;
  patientId: string;
  patientName: string;
  startAt: string;
  durationMinutes: number;
  status: string;
  activeSlot: string | null;
}

export interface AgendaView {
  entries: AgendaEntry[];
  empty: boolean;
  message: string | null;
}

export interface AgendaResult extends AgendaView {
  error: string | null;
}

const BUENOS_AIRES_OFFSET = '-03:00';

// Calendar arithmetic runs on UTC fields only to add days; the result is written as a
// Buenos Aires local midnight (`-03:00`), never through toISOString(), which would emit `Z`.
function localMidnight(day: Date): string {
  return `${day.toISOString().slice(0, 10)}T00:00:00${BUENOS_AIRES_OFFSET}`;
}

function localRange(date: string, days: number): AgendaRange {
  const from = startOfDay(date);
  const to = new Date(from);
  to.setUTCDate(to.getUTCDate() + days);

  return { from: localMidnight(from), to: localMidnight(to) };
}

function startOfDay(date: string): Date {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    throw new Error(MISSING_RANGE_MESSAGE);
  }

  const parsed = new Date(`${date}T00:00:00Z`);

  if (Number.isNaN(parsed.getTime()) || !parsed.toISOString().startsWith(date)) {
    throw new Error(MISSING_RANGE_MESSAGE);
  }

  return parsed;
}

export function dailyRange(date: string): AgendaRange {
  return localRange(date, 1);
}

export function weeklyRange(date: string): AgendaRange {
  return localRange(date, 7);
}

function isInstant(value: string | undefined): boolean {
  if (typeof value !== 'string') {
    return false;
  }

  const shape = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/;

  return shape.test(value) && !Number.isNaN(Date.parse(value));
}

export function validateAgendaRange(from: string | undefined, to: string | undefined): string | null {
  if (!isInstant(from) || !isInstant(to)) {
    return MISSING_RANGE_MESSAGE;
  }

  return Date.parse(from as string) >= Date.parse(to as string)
    ? INVALID_RANGE_MESSAGE
    : null;
}

export function buildPatientNameMap(patients: PatientSummary[]): Map<string, string> {
  const names = new Map<string, string>();

  for (const patient of patients) {
    const name = `${patient.nombre ?? ''} ${patient.apellido ?? ''}`.trim();

    if (typeof patient.id === 'string' && patient.id.length > 0 && name.length > 0) {
      names.set(patient.id, name);
    }
  }

  return names;
}

export function patientName(names: Map<string, string>, patientId: string): string {
  return names.get(patientId) ?? patientId;
}

export function viewAgenda(appointments: Appointment[], names: Map<string, string>): AgendaView {
  const entries = appointments
    .filter((appointment) => appointment.status !== 'CANCELLED')
    .sort((left, right) => left.startAt.localeCompare(right.startAt))
    .map((appointment) => ({
      id: appointment.id,
      patientId: appointment.patientId,
      patientName: patientName(names, appointment.patientId),
      startAt: appointment.startAt,
      durationMinutes: appointment.durationMinutes,
      status: appointment.status,
      activeSlot: appointment.activeSlot
    }));

  return {
    entries,
    empty: entries.length === 0,
    message: entries.length === 0 ? NO_APPOINTMENTS_MESSAGE : null
  };
}

function isUnauthenticated(error: unknown): boolean {
  if (typeof error !== 'object' || error === null) {
    return false;
  }

  const candidate = error as { status?: unknown; body?: unknown };

  return isSessionExpired(candidate.status as number, candidate.body);
}

export async function loadAgenda(api: AgendaApi, range: AgendaRange): Promise<AgendaResult> {
  const appointments = await api.appointments(range.from, range.to);
  let names = new Map<string, string>();

  try {
    const page = await api.patients({ page: 0, size: 100 });
    names = buildPatientNameMap(page.content);
  } catch (error) {
    if (isUnauthenticated(error)) {
      throw error;
    }
  }

  return { ...viewAgenda(appointments, names), error: null };
}
