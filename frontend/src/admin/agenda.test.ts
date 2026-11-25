import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildPatientNameMap,
  dailyRange,
  loadAgenda,
  NO_APPOINTMENTS_MESSAGE,
  patientName,
  validateAgendaRange,
  viewAgenda,
  weeklyRange,
  type AgendaApi,
  type Appointment,
  type PatientPage,
  type PatientSummary
} from './agenda.js';

const appointment: Appointment = {
  id: 'appointment-1',
  patientId: 'patient-1',
  startAt: '2026-11-24T10:00:00Z',
  durationMinutes: 30,
  status: 'CONFIRMED',
  activeSlot: '2026-11-24T10:00:00Z'
};

const patients: PatientSummary[] = [
  { id: 'patient-1', dni: '12345678', nombre: 'Paciente', apellido: 'Demo', email: 'paciente@example.invalid' },
  { id: 'patient-2', dni: '87654321', nombre: 'Otro', apellido: 'Ficticio', email: 'otro@example.invalid' }
];

function fakeApi(options: {
  appointments?: Appointment[];
  patients?: PatientPage;
  patientsError?: { status: number; body: unknown };
}): { api: AgendaApi; calls: Array<{ method: string; args: unknown[] }> } {
  const calls: Array<{ method: string; args: unknown[] }> = [];

  const api: AgendaApi = {
    async appointments(from: string, to: string) {
      calls.push({ method: 'appointments', args: [from, to] });
      return options.appointments ?? [];
    },
    async patients(query: { dni?: string; page: number; size: number }) {
      calls.push({ method: 'patients', args: [query] });
      if (options.patientsError) {
        const error = new Error('HTTP') as Error & { status: number; body: unknown };
        error.status = options.patientsError.status;
        error.body = options.patientsError.body;
        throw error;
      }
      return options.patients ?? { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 };
    }
  };

  return { api, calls };
}

test('daily and weekly ranges are valid Instants with from before to', () => {
  assert.deepEqual(dailyRange('2026-11-24'), {
    from: '2026-11-24T00:00:00Z',
    to: '2026-11-25T00:00:00Z'
  });
  assert.deepEqual(weeklyRange('2026-11-24'), {
    from: '2026-11-24T00:00:00Z',
    to: '2026-12-01T00:00:00Z'
  });
  assert.deepEqual(dailyRange('2026-12-31'), {
    from: '2026-12-31T00:00:00Z',
    to: '2027-01-01T00:00:00Z'
  });
});

test('range validation rejects from >= to, missing and unparseable parameters', () => {
  assert.equal(validateAgendaRange('2026-11-24T10:00:00Z', '2026-11-24T10:00:00Z'), 'Rango de agenda inválido');
  assert.equal(validateAgendaRange('2026-11-25T10:00:00Z', '2026-11-24T10:00:00Z'), 'Rango de agenda inválido');
  assert.equal(validateAgendaRange('', '2026-11-25T10:00:00Z'), 'Parámetros from y to inválidos o faltantes');
  assert.equal(validateAgendaRange('ayer', '2026-11-25T10:00:00Z'), 'Parámetros from y to inválidos o faltantes');
  assert.equal(validateAgendaRange('2026-11-24T10:00:00Z', '2026-11-25T10:00:00Z'), null);
});

test('name map uses nombre and apellido and falls back to the patientId without detail lookups', () => {
  const names = buildPatientNameMap(patients);

  assert.equal(patientName(names, 'patient-1'), 'Paciente Demo');
  assert.equal(patientName(names, 'patient-2'), 'Otro Ficticio');
  assert.equal(patientName(names, 'patient-outside-map'), 'patient-outside-map');
  assert.equal(names.size, 2);
});

test('agenda view keeps only server fields ordered by startAt and hides cancelled appointments', () => {
  const later: Appointment = { ...appointment, id: 'appointment-2', patientId: 'patient-2', startAt: '2026-11-24T11:00:00Z' };
  const cancelled: Appointment = { ...appointment, id: 'appointment-3', status: 'CANCELLED', activeSlot: null };
  const names = buildPatientNameMap(patients);
  const view = viewAgenda([later, cancelled, appointment], names);

  assert.deepEqual(view.entries.map((entry) => entry.id), ['appointment-1', 'appointment-2']);
  assert.deepEqual(view.entries[0], {
    id: 'appointment-1',
    patientId: 'patient-1',
    patientName: 'Paciente Demo',
    startAt: '2026-11-24T10:00:00Z',
    durationMinutes: 30,
    status: 'CONFIRMED',
    activeSlot: '2026-11-24T10:00:00Z'
  });
  assert.equal(view.empty, false);
  assert.equal(view.entries.some((entry) => entry.status === 'CANCELLED'), false);
});

test('an empty appointment array is an empty state, not an error', () => {
  const view = viewAgenda([], buildPatientNameMap([]));

  assert.deepEqual(view.entries, []);
  assert.equal(view.empty, true);
  assert.equal(view.message, NO_APPOINTMENTS_MESSAGE);
});

test('agenda load performs exactly one patient list call with page 0 and size 100', async () => {
  const { api: baseApi, calls } = fakeApi({ appointments: [appointment], patients: { content: patients, page: 0, size: 100, totalElements: 2, totalPages: 1 } });
  const cachedPatientsView = { content: [{ id: 'patient-1', nombre: 'Cache', apellido: 'Vieja' }] };
  const api = Object.assign(baseApi, { cachedPatientsView });

  const result = await loadAgenda(api, dailyRange('2026-11-24'));

  assert.deepEqual(calls, [
    { method: 'appointments', args: ['2026-11-24T00:00:00Z', '2026-11-25T00:00:00Z'] },
    { method: 'patients', args: [{ page: 0, size: 100 }] }
  ]);
  assert.deepEqual(Object.keys(calls[1].args[0] as object), ['page', 'size']);
  assert.equal(result.entries[0].patientName, 'Paciente Demo');
  assert.equal(result.error, null);
  assert.equal(cachedPatientsView.content[0].nombre, 'Cache');
});

test('a patient outside the first page uses the patientId without extra pages or detail routes', async () => {
  const outside: Appointment = { ...appointment, patientId: 'patient-outside-map' };
  const detailCalls: string[] = [];
  const apiWithDetail: AgendaApi = {
    ...fakeApi({ appointments: [outside], patients: { content: patients, page: 0, size: 100, totalElements: 2, totalPages: 1 } }).api,
    patientsById: async (id: string) => {
      detailCalls.push(id);
      return null;
    }
  } as AgendaApi;

  const result = await loadAgenda(apiWithDetail, dailyRange('2026-11-24'));

  assert.equal(result.entries[0].patientName, 'patient-outside-map');
  assert.deepEqual(detailCalls, []);
});

test('a non-401 patient list failure leaves the agenda visible with patientId and no agenda error', async () => {
  const { api } = fakeApi({
    appointments: [appointment],
    patientsError: { status: 500, body: { error: 'No se pudo completar la solicitud.' } }
  });

  const result = await loadAgenda(api, weeklyRange('2026-11-24'));

  assert.equal(result.entries[0].patientName, 'patient-1');
  assert.equal(result.error, null);
  assert.equal(result.empty, false);
});

test('a 401 from the patient list is delegated to session handling without a redirect', async () => {
  const { api } = fakeApi({ appointments: [appointment], patientsError: { status: 401, body: { error: 'No autenticado' } } });

  await assert.rejects(
    loadAgenda(api, dailyRange('2026-11-24')),
    (error: unknown) => (error as { status?: number }).status === 401
  );
});
