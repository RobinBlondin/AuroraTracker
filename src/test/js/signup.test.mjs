import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFileSync } from 'node:fs';

const source = readFileSync('src/main/resources/static/script/signup.js', 'utf8')
  .replace('import { subscribePush } from "./push.js";', '');

async function submit({ method = 'email', responseStatus = 202 } = {}) {
  const elements = new Map();
  for (const id of ['signup-form', 'email', 'get-location', 'location-status', 'signup-status', 'submit-button', 'lat', 'lon']) {
    elements.set(id, { value: '', disabled: false, textContent: '', listeners: {},
      addEventListener(type, handler) { this.listeners[type] = handler; } });
  }
  elements.get('email').value = 'user@example.com';
  elements.get('lat').value = '60';
  elements.get('lon').value = '18';
  const requests = [];
  vm.runInContext(source, vm.createContext({
    document: {
      getElementById: id => elements.get(id),
      querySelector: selector => ({ value: selector.includes('method') ? method : 'CLEAR' }),
      querySelectorAll: () => []
    },
    navigator: {}, console: { error() {} },
    subscribePush: async () => ({ firebaseToken: 'fcm-token' }),
    fetch: async (url, options) => {
      requests.push({ url, ...options });
      return { ok: responseStatus >= 200 && responseStatus < 300, status: responseStatus };
    }
  }));
  await elements.get('signup-form').listeners.submit({ preventDefault() {} });
  return { elements, requests };
}

test('email signup uses the public endpoint without a secret or client identity', async () => {
  const ui = await submit();
  assert.equal(ui.requests[0].headers['X-Subscription-Request'], '1');
  assert.equal(ui.requests[0].headers['X-Request-ID'], undefined);
  assert.equal(JSON.parse(ui.requests[0].body).userId, undefined);
  assert.match(ui.elements.get('signup-status').textContent, /Check your email/);
  assert.equal(ui.elements.get('submit-button').textContent, 'Check your email');
});

test('push signup submits delivery information and shows activation', async () => {
  const ui = await submit({ method: 'push', responseStatus: 201 });
  const body = JSON.parse(ui.requests[0].body);
  assert.equal(body.email, null);
  assert.equal(body.firebaseToken, 'fcm-token');
  assert.equal(ui.elements.get('signup-status').textContent, 'Alerts successfully enabled!');
});

test('rate limited signup offers retry and re-enables the button', async () => {
  const ui = await submit({ responseStatus: 429 });
  assert.match(ui.elements.get('signup-status').textContent, /Too many requests/);
  assert.equal(ui.elements.get('submit-button').disabled, false);
});

test('already registered push browsers are directed to management', async () => {
  const ui = await submit({ method: 'push', responseStatus: 409 });
  assert.match(ui.elements.get('signup-status').textContent, /Manage alerts/);
});
