export interface DateWindow {
  min: string;
  max: string;
}

const HOURS_24_MS = 24 * 60 * 60 * 1000;
const DAYS_30_MS = 30 * HOURS_24_MS;

export function formatLocalDate(date: Date): string {
  const year = date.getFullYear().toString().padStart(4, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');

  return `${year}-${month}-${day}`;
}

export function dateWindow(now: Date): DateWindow {
  return {
    min: formatLocalDate(new Date(now.getTime() + HOURS_24_MS)),
    max: formatLocalDate(new Date(now.getTime() + DAYS_30_MS))
  };
}

export function isDateInWindow(value: string, now: Date): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const parts = value.split('-').map(Number);
  const year = parts[0];
  const month = parts[1];
  const day = parts[2];
  const parsed = new Date(year, month - 1, day);

  if (parsed.getFullYear() !== year || parsed.getMonth() !== month - 1 || parsed.getDate() !== day) {
    return false;
  }

  const window = dateWindow(now);

  return value >= window.min && value <= window.max;
}
