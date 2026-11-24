const TOKEN_PATTERN = /^[A-Za-z0-9_-]{43}$/;

export function extractToken(input: string): string | null {
  const trimmed = input.trim();

  if (TOKEN_PATTERN.test(trimmed)) {
    return trimmed;
  }

  const withoutFragment = trimmed.split('#')[0];
  const path = withoutFragment.split('?')[0].replace(/\/+$/, '');
  const segments = path.split('/').filter((segment) => segment.length > 0);

  const appointmentsIndex = segments.indexOf('appointments');

  if (appointmentsIndex >= 0 && appointmentsIndex + 1 < segments.length) {
    const candidate = segments[appointmentsIndex + 1];

    if (TOKEN_PATTERN.test(candidate)) {
      return candidate;
    }
  }

  for (let index = segments.length - 1; index >= 0; index -= 1) {
    const candidate = segments[index];

    if (TOKEN_PATTERN.test(candidate)) {
      return candidate;
    }
  }

  return null;
}
