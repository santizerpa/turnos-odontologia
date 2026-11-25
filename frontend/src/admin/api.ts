import type { Appointment, PatientPage, PatientQuery } from './agenda.js';
import type { AppointmentStatus } from './appointments.js';
import { isSessionExpired } from './session.js';
import type { OutboxMessage, TokenAction } from './outbox.js';
import type { WaitingEntry } from './waiting-list.js';
import { statusRequest } from './appointments.js';

export interface ApiResponseLike {
  ok: boolean;
  status: number;
  json(): Promise<unknown>;
}

export type FetchLike = (input: string, init?: RequestInit) => Promise<ApiResponseLike>;

export interface ApiClientOptions {
  fetch?: FetchLike;
  onSessionExpired?: (status: number, body: unknown) => void;
}

export interface TokenStatusResponse {
  startAt: string;
  status: string;
}

export interface TokenActionResponse {
  message: string;
  status: string;
}

export interface AdminApi {
  login(email: string, password: string): Promise<unknown>;
  me(): Promise<unknown>;
  logout(): Promise<void>;
  appointments(from: string, to: string): Promise<Appointment[]>;
  patients(query: PatientQuery): Promise<PatientPage>;
  changeStatus(id: string, status: AppointmentStatus, reason?: string | null): Promise<unknown>;
  outbox(): Promise<OutboxMessage[]>;
  waitingList(): Promise<WaitingEntry[]>;
  tokenInfo(token: string, action: TokenAction): Promise<TokenStatusResponse>;
  tokenAction(token: string, action: TokenAction): Promise<TokenActionResponse>;
}

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly body: unknown) {
    super(`HTTP ${status}`);
  }
}

function buildInit(method?: string, payload?: unknown): RequestInit {
  const init: RequestInit = { credentials: 'same-origin' };

  if (method !== undefined) {
    init.method = method;
  }

  if (payload !== undefined) {
    init.headers = { 'Content-Type': 'application/json' };
    init.body = JSON.stringify(payload);
  }

  return init;
}

function queryString(parts: Array<[string, string | number | undefined]>): string {
  const query = parts
    .filter((part): part is [string, string | number] => part[1] !== undefined && part[1] !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&');

  return query.length > 0 ? `?${query}` : '';
}

export function createAdminApi(options: ApiClientOptions = {}): AdminApi {
  const fetchImpl: FetchLike = options.fetch ??
    ((input: string, init?: RequestInit) => globalThis.fetch(input, init));

  async function request<T>(path: string, init?: RequestInit, authenticated = true): Promise<T> {
    const response = await fetchImpl(path, init ?? buildInit());
    let body: unknown = null;

    try {
      body = await response.json();
    } catch {
      body = null;
    }

    if (!response.ok) {
      if (authenticated && isSessionExpired(response.status, body)) {
        options.onSessionExpired?.(response.status, body);
      }

      throw new ApiError(response.status, body);
    }

    return body as T;
  }

  return {
    login: (email, password) =>
      request<unknown>('/api/auth/login', buildInit('POST', { email, password }), false),
    me: () => request<unknown>('/api/auth/me'),
    logout: async () => {
      await request<unknown>('/api/auth/logout', buildInit('POST'));
    },
    appointments: (from, to) =>
      request<Appointment[]>(`/api/appointments${queryString([['from', from], ['to', to]])}`),
    patients: (query) =>
      request<PatientPage>(
        `/api/patients${queryString([['dni', query.dni], ['page', query.page], ['size', query.size]])}`
      ),
    changeStatus: (id, status, reason) =>
      request<unknown>(
        `/api/appointments/${encodeURIComponent(id)}/status`,
        buildInit('PATCH', statusRequest(status, reason))
      ),
    outbox: () => request<OutboxMessage[]>('/api/outbox/messages'),
    waitingList: () => request<WaitingEntry[]>('/api/waiting-list'),
    tokenInfo: (token, action) =>
      request<TokenStatusResponse>(
        `/api/public/appointments/${encodeURIComponent(token)}/${action}`,
        buildInit(),
        false
      ),
    tokenAction: (token, action) =>
      request<TokenActionResponse>(
        `/api/public/appointments/${encodeURIComponent(token)}/${action}`,
        buildInit('POST'),
        false
      )
  };
}
