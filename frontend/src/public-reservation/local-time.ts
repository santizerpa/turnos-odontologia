// Presentation-only formatting in Buenos Aires local time (dd/MM/yy HH:mm); API values stay as ISO Instants.
const PARTS = new Intl.DateTimeFormat('es-AR', {
  timeZone: 'America/Argentina/Buenos_Aires',
  day: '2-digit',
  month: '2-digit',
  year: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23'
});

export function localDateTime(instant: string): string | null {
  const date = new Date(instant);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  // Built from parts so the output does not depend on the runtime's ICU separators.
  const part = (type: Intl.DateTimeFormatPartTypes): string =>
    PARTS.formatToParts(date).find((candidate) => candidate.type === type)?.value ?? '';

  return `${part('day')}/${part('month')}/${part('year')} ${part('hour')}:${part('minute')}`;
}
