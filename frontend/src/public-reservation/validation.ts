import type { PatientFormValues } from './payload.js';

export function validateBookingForm(
  values: PatientFormValues,
  selectedStart: string | null
): string | null {
  if (selectedStart === null || selectedStart.trim() === '') {
    return 'Seleccione un horario libre antes de reservar.';
  }
  if (values.nombre.trim() === '') {
    return 'Ingrese el nombre del paciente.';
  }
  if (values.apellido.trim() === '') {
    return 'Ingrese el apellido del paciente.';
  }
  if (values.dni.trim() === '') {
    return 'Ingrese el DNI del paciente.';
  }
  if (values.email.trim() === '') {
    return 'Ingrese el email del paciente.';
  }

  return null;
}
