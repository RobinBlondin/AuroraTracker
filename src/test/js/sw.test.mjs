import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync('src/main/resources/static/sw.js', 'utf8');
function worker() {
  const handlers = new Map();
  const shown = [];
  const opened = [];
  const clients = { claim() {}, openWindow(url) { opened.push(url); return Promise.resolve(); } };
  const context = vm.createContext({
    URL, clients, importScripts() {},
    self: {
      location: { origin: 'https://auroratracker.se' }, clients,
      addEventListener: (name, handler) => handlers.set(name, handler),
      registration: { showNotification: async (title, options) => shown.push({ title, ...options }) },
      skipWaiting() {}, APP_CONFIG: {}
    },
    firebase: { initializeApp() {}, messaging: () => ({ onBackgroundMessage() {} }) }
  });
  vm.runInContext(source, context);
  return { handlers, shown, opened };
}

for (const provider of ['webpush', 'firebase']) {
  test(`${provider} recovery pushes show the management notification and link`, async () => {
    const w = worker();
    const payload = { title: 'Aurora Tracker', body: 'Tap to manage', type: 'management', url: 'https://auroratracker.se/manage-alerts#token=secret' };
    const waits = [];
    w.handlers.get('push')({
      data: { json: () => provider === 'firebase' ? { data: payload } : payload },
      waitUntil: task => waits.push(task)
    });
    await Promise.all(waits);
    assert.equal(w.shown[0].body, 'Tap to manage');
    assert.equal(w.shown[0].tag, 'aurora_management');
    w.handlers.get('notificationclick')({
      notification: { data: w.shown[0].data, close() {} }, waitUntil() {}
    });
    assert.equal(w.opened[0], payload.url);
  });
}

test('notification clicks cannot navigate outside Aurora Tracker', () => {
  const w = worker();
  w.handlers.get('notificationclick')({
    notification: { data: 'https://other.example/manage', close() {} }, waitUntil() {}
  });
  assert.equal(w.opened.length, 0);
});
