import type { PatientPage, PatientQuery, PatientSummary } from './agenda.js';
import { visibleError } from './errors.js';

export type PatientPageResponse = PatientPage;
export type PatientRecord = PatientSummary;

export type PatientSearchCriterion = 'dni' | 'nombre';

export interface PatientQueryInput {
  /** Chosen criterion; when present, `term` is sent only under that parameter. */
  criterion?: PatientSearchCriterion;
  term?: string;
  dni?: string;
  page?: string | number;
  size?: string | number;
}

export interface PatientPageView {
  rows: PatientRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  empty: boolean;
  filter: string;
}

function numeric(value: string | number | undefined, fallback: number): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string' && /^\d+$/.test(value.trim())) {
    return Number(value.trim());
  }

  return fallback;
}

export function patientQuery(input: PatientQueryInput): PatientQuery {
  const criterion: PatientSearchCriterion = input.criterion ?? 'dni';
  const raw = input.criterion === undefined ? input.dni : input.term;
  const term = typeof raw === 'string' ? raw.trim() : '';
  const query: PatientQuery = {
    page: numeric(input.page, 0),
    size: numeric(input.size, 10)
  };

  // dni and nombre are mutually exclusive on the API: only the chosen one is sent.
  if (term.length > 0) {
    query[criterion] = term;
  }

  return query;
}

export function patientDetail(patient: PatientRecord): PatientRecord {
  return {
    id: patient.id,
    dni: patient.dni ?? '',
    nombre: patient.nombre,
    apellido: patient.apellido,
    email: patient.email ?? ''
  };
}

export function viewPatientPage(response: PatientPage, query: PatientQuery): PatientPageView {
  const content = Array.isArray(response.content) ? response.content : [];

  return {
    rows: content.map(patientDetail),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
    empty: content.length === 0,
    filter: query.dni ?? query.nombre ?? ''
  };
}

export function patientsErrorMessage(status: number, body: unknown): string {
  return visibleError(status, body);
}
