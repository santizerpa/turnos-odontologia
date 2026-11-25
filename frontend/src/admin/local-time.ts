// Presentation-only formatting in Buenos Aires local time, 24-hour clock; API values stay as ISO Instants.
// Same visible format as the public page (dd/MM/yy HH:mm) so both surfaces read alike.
const TIME_ZONE = 'America/Argentina/Buenos_Aires';

const PARTS = new Intl.DateTimeFormat('es-AR', {
  timeZone: TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23'
});

function parts(date: Date): Record<string, string> {
  // Built from parts so the output does not depend on the runtime's ICU separators.
  return Object.fromEntries(PARTS.formatToParts(date).map((part) => [part.type, part.value]));
}

export function localDateTime(instant: string): string {
  const date = new Date(instant);

  if (Number.isNaN(date.getTime())) {
    return instant;
  }

  const p = parts(date);

  return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}`;
}

export function localTimeRange(startInstant: string, durationMinutes: number): string {
  const start = new Date(startInstant);
  const end = new Date(start.getTime() + durationMinutes * 60_000);
  const from = parts(start);
  const to = parts(end);

  return `${from.hour}:${from.minute}–${to.hour}:${to.minute}`;
}
