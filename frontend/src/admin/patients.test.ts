import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  patientDetail,
  patientQuery,
  patientsErrorMessage,
  viewPatientPage,
  type PatientPageResponse,
  type PatientRecord
} from './patients.js';

const record = {
  id: 'patient-1',
  dni: '12345678',
  nombre: 'Paciente',
  apellido: 'Demo',
  email: 'paciente@example.invalid'
};

const page: PatientPageResponse = {
  content: [record],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1
};

test('patient query builds the exact dni, page and size parameters', () => {
  assert.deepEqual(patientQuery({ dni: '  12.345.678  ', page: '2', size: '25' }), {
    dni: '12.345.678',
    page: 2,
    size: 25
  });
  assert.deepEqual(patientQuery({}), { page: 0, size: 10 });
  assert.deepEqual(patientQuery({ dni: '   ', page: 0, size: 0 }), { page: 0, size: 0 });
});

test('patient query sends only the chosen criterion, dni or nombre, never both', () => {
  assert.deepEqual(patientQuery({ criterion: 'nombre', term: '  ana  ', page: '0', size: '10' }), {
    nombre: 'ana',
    page: 0,
    size: 10
  });
  assert.deepEqual(patientQuery({ criterion: 'dni', term: '12.345.678' }), { dni: '12.345.678', page: 0, size: 10 });
  assert.deepEqual(patientQuery({ criterion: 'nombre', term: '   ' }), { page: 0, size: 10 });
  assert.equal('dni' in patientQuery({ criterion: 'nombre', term: 'ana', dni: '12345678' }), false);
});

test('patient page view keeps the visible filter for either criterion', () => {
  const query = patientQuery({ criterion: 'nombre', term: 'ana' });

  assert.equal(viewPatientPage(page, query).filter, 'ana');
});

test('detail keeps only the five fields returned by the patient list', () => {
  const extra = { ...record, passwordHash: 'hidden', extra: 'hidden' } as unknown as PatientRecord;

  assert.deepEqual(patientDetail(extra), record);
  assert.deepEqual(Object.keys(patientDetail(record)), ['id', 'dni', 'nombre', 'apellido', 'email']);
});

test('page view shows content and pagination metadata without inventing data', () => {
  const view = viewPatientPage(page, patientQuery({}));

  assert.deepEqual(view.rows, [record]);
  assert.equal(view.empty, false);
  assert.deepEqual(
    { page: view.page, size: view.size, totalElements: view.totalElements, totalPages: view.totalPages },
    { page: 0, size: 10, totalElements: 1, totalPages: 1 }
  );
});

test('empty page keeps the visible filter and page', () => {
  const query = patientQuery({ dni: '99999999', page: '3', size: '10' });
  const view = viewPatientPage({ ...page, content: [], page: 3, totalElements: 0, totalPages: 0 }, query);

  assert.deepEqual(view.rows, []);
  assert.equal(view.empty, true);
  assert.equal(view.filter, '99999999');
  assert.equal(view.page, 3);
  assert.equal(view.size, 10);
});

test('400 ProblemDetail is read from detail without an instance field', () => {
  const body = { type: 'about:blank', title: 'Bad Request', status: 400, detail: 'Datos de paciente inválidos.' };

  assert.equal(patientsErrorMessage(400, body), 'Datos de paciente inválidos.');
  assert.equal('instance' in body, false);
  assert.equal(patientsErrorMessage(400, { error: 'otro' }), 'otro');
  assert.equal(patientsErrorMessage(401, { error: 'No autenticado' }), 'No autenticado');
  assert.equal(patientsErrorMessage(500, {}), 'No se pudo completar la solicitud. Intente nuevamente más tarde.');
});
