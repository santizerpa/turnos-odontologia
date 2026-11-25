import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  NO_ENTRIES_MESSAGE,
  waitingListErrorMessage,
  waitingListView,
  type WaitingEntry
} from './waiting-list.js';

const first: WaitingEntry = {
  id: 'entry-z',
  nombre: 'Espera',
  apellido: 'Demo',
  email: 'espera@example.invalid',
  requestedDateFrom: '2026-12-01',
  requestedDateTo: '2026-12-05',
  status: 'NOTIFIED'
};

const second: WaitingEntry = {
  id: 'entry-a',
  nombre: 'Otra',
  apellido: 'Ficticia',
  email: 'otra@example.invalid',
  requestedDateFrom: '2026-12-06',
  requestedDateTo: '2026-12-08',
  status: 'PENDING'
};

test('waiting list preserves server FIFO order, shows preferences and hides DNI', () => {
  const raw = [
    { ...first, dni: '87654321', assignedAppointmentId: 'invented' },
    { ...second, dni: '11111111' }
  ] as unknown as WaitingEntry[];
  const view = waitingListView(raw);

  assert.deepEqual(view.rows, [first, second]);
  assert.deepEqual(view.rows.map((entry) => entry.id), ['entry-z', 'entry-a']);
  assert.equal(JSON.stringify(view).includes('87654321'), false);
  assert.equal(JSON.stringify(view).includes('assignedAppointmentId'), false);
  assert.equal(view.empty, false);
});

test('an empty waiting list has a visible state without assignment actions', () => {
  const view = waitingListView([]);

  assert.deepEqual(view.rows, []);
  assert.equal(view.empty, true);
  assert.equal(view.message, NO_ENTRIES_MESSAGE);
  assert.equal(view.hasAssignmentActions, false);
});

test('waiting list errors keep the exact backend message', () => {
  assert.equal(waitingListErrorMessage(401, { error: 'No autenticado' }), 'No autenticado');
  assert.equal(waitingListErrorMessage(500, { error: 'Fallo de bandeja' }), 'Fallo de bandeja');
  assert.equal(waitingListErrorMessage(500, {}), 'No se pudo completar la solicitud. Intente nuevamente más tarde.');
});
