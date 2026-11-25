declare module 'node:test' {
  export interface TestContext {}

  export function test(name: string, fn: (context: TestContext) => void | Promise<void>): void;
  export function describe(name: string, fn: () => void): void;
  export function it(name: string, fn: (context: TestContext) => void | Promise<void>): void;
}

declare module 'node:assert/strict' {
  interface AssertStrict {
    equal(actual: unknown, expected: unknown, message?: string): void;
    notEqual(actual: unknown, expected: unknown, message?: string): void;
    deepEqual(actual: unknown, expected: unknown, message?: string): void;
    ok(value: unknown, message?: string): void;
    match(value: string, regexp: RegExp, message?: string): void;
    throws(fn: () => unknown, expected?: unknown, message?: string): void;
    rejects(fn: Promise<unknown> | (() => unknown), expected?: unknown, message?: string): Promise<void>;
  }

  const assert: AssertStrict;

  export default assert;
}
