import test from "node:test";
import assert from "node:assert/strict";
import { clearSession, onSessionEnded, saveSessionUser } from "../src/utils/session.js";

const storage = () => {
  const values = new Map();
  return { setItem: (key, value) => values.set(key, String(value)), getItem: (key) => values.get(key) ?? null, removeItem: (key) => values.delete(key) };
};

test("logout clears legacy tokens and notifies same-tab and other-tab subscribers", (t) => {
  for (const [name, value] of Object.entries({ window: new EventTarget(), localStorage: storage(), sessionStorage: storage() })) {
    const previous = Object.getOwnPropertyDescriptor(globalThis, name);
    Object.defineProperty(globalThis, name, { configurable: true, value });
    t.after(() => previous ? Object.defineProperty(globalThis, name, previous) : delete globalThis[name]);
  }
  localStorage.setItem("token", "old-fixture");
  saveSessionUser({ _id: "fixture" });
  assert.equal(localStorage.getItem("token"), null);
  sessionStorage.setItem("pendingSignup", "fixture");
  const calls = [];
  const unsubscribe = onSessionEnded((remote) => calls.push(remote));
  clearSession();
  assert.deepEqual(calls, [false]);
  assert.equal(localStorage.getItem("userData"), null);
  assert.equal(sessionStorage.getItem("pendingSignup"), null);
  const event = new Event("storage");
  Object.assign(event, { key: "civicresolve:logout", newValue: "another-tab" });
  window.dispatchEvent(event);
  assert.deepEqual(calls, [false, true]);
  unsubscribe();
  window.dispatchEvent(event);
  assert.equal(calls.length, 2);
});
