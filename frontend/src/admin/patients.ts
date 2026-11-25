import type { PatientPage, PatientQuery, PatientSummary } from './agenda.js';
import { visibleError } from './errors.js';

export type PatientPageResponse = PatientPage;
export type PatientRecord = PatientSummary;

export interface PatientQueryInput {
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
  const dni = typeof input.dni === 'string' ? input.dni.trim() : '';
  const query: PatientQuery = {
    page: numeric(input.page, 0),
    size: numeric(input.size, 10)
  };

  if (dni.length > 0) {
    query.dni = dni;
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
    filter: query.dni ?? ''
  };
}

export function patientsErrorMessage(status: number, body: unknown): string {
  return visibleError(status, body);
}
