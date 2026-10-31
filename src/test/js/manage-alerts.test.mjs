import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const pageSource = readFileSync('src/main/resources/static/script/manage-alerts.js', 'utf8')
  .replace("import { getExistingPushIdentity } from './push.js';", '');
const flush = () => new Promise(resolve => setImmediate(resolve));

function page({ hash = '', replies = [], identity = { endpoint: 'browser-endpoint' } } = {}) {
  const elements = new Map();
  const ids = ['manage-form', 'manage-status', 'location-status', 'lat', 'lon', 'save-button',
    'unsubscribe-button', 'get-location', 'enabled', 'recovery-options', 'verify-link',
    'verify-link-button', 'verify-push-button', 'verify-email-button', 'recovery-email-form', 'recovery-email'];
  for (const id of ids) elements.set(id, {
    hidden: ['manage-form', 'recovery-options', 'verify-link'].includes(id),
    disabled: false, value: '', textContent: '', listeners: {},
    addEventListener(type, listener) { this.listeners[type] = listener; }
  });
  const requests = [];
  const history = [];
  const levels = [{ value: 'ALL' }, { value: 'CLEAR' }];
  const context = vm.createContext({
    URLSearchParams, Number, JSON,
    window: {
      location: { hash, pathname: '/manage-alerts', search: '' },
      history: { replaceState: (...args) => history.push(args) }, confirm: () => true
    },
    document: {
      getElementById: id => elements.get(id),
      querySelectorAll: () => levels,
      querySelector: () => levels.find(level => level.checked)
    },
    navigator: {},
    getExistingPushIdentity: async () => {
      if (identity instanceof Error) throw identity;
      return identity;
    },
    fetch: async (url, options) => {
      requests.push({ url, ...options });
      const reply = replies.shift();
      assert.ok(reply, `Unexpected request to ${url}`);
      return {
        status: reply.status, ok: reply.status >= 200 && reply.status < 300,
        json: async () => reply.body
      };
    }
  });
  vm.runInContext(pageSource, context);
  return {
    elements, requests, history,
    fire: async (id, type = 'click') => {
      await elements.get(id).listeners[type]({ preventDefault() {} });
      await flush();
    }
  };
}

const preferences = { lat: 59.3, lon: 18, notificationLevel: 'CLEAR', enabled: true, csrfToken: 'csrf' };

test('expired sessions show both recovery options and hide the preferences form', async () => {
  const ui = page({ replies: [{ status: 401 }] });
  await flush();
  assert.equal(ui.elements.get('manage-form').hidden, true);
  assert.equal(ui.elements.get('recovery-options').hidden, false);
});

test('email recovery submits the address and shows a generic confirmation', async () => {
  const ui = page({ replies: [{ status: 401 }, { status: 202 }] });
  await flush();
  ui.elements.get('recovery-email').value = ' user@example.com ';
  await ui.fire('recovery-email-form', 'submit');
  assert.equal(ui.requests[1].url, '/api/subscriptions/manage/email');
  assert.deepEqual(JSON.parse(ui.requests[1].body), { email: 'user@example.com' });
  assert.equal(ui.requests[1].headers['X-Management-Request'], '1');
  assert.match(ui.elements.get('manage-status').textContent, /If a subscription exists/);
  assert.equal(ui.elements.get('verify-email-button').disabled, false);
});

test('link previews do not redeem tokens and confirmation loads the subscription', async () => {
  const ui = page({ hash: '#token=secret-token', replies: [{ status: 204 }, { status: 200, body: preferences }] });
  await flush();
  assert.equal(ui.requests.length, 0);
  assert.equal(ui.history[0][2], '/manage-alerts');
  assert.equal(ui.elements.get('verify-link').hidden, false);
  await ui.fire('verify-link-button');
  assert.equal(ui.requests[0].url, '/api/subscriptions/manage/verify');
  assert.deepEqual(JSON.parse(ui.requests[0].body), { token: 'secret-token' });
  assert.equal(ui.elements.get('manage-form').hidden, false);
  assert.equal(ui.elements.get('verify-link').hidden, true);
  assert.equal(ui.elements.get('lat').value, '59.300');
});

test('expired links offer recovery instead of showing subscription data', async () => {
  const ui = page({ hash: '#token=expired', replies: [{ status: 401 }] });
  await ui.fire('verify-link-button');
  assert.equal(ui.elements.get('manage-form').hidden, true);
  assert.equal(ui.elements.get('recovery-options').hidden, false);
});

test('push recovery requests a challenge but does not create a session from the endpoint', async () => {
  const ui = page({ replies: [{ status: 401 }, { status: 202 }] });
  await flush();
  await ui.fire('verify-push-button');
  assert.equal(ui.requests[1].url, '/api/subscriptions/manage/challenge');
  assert.deepEqual(JSON.parse(ui.requests[1].body), { endpoint: 'browser-endpoint' });
  assert.equal(ui.elements.get('manage-form').hidden, true);
  assert.match(ui.elements.get('manage-status').textContent, /Tap it to continue/);
});

test('missing push subscriptions leave email recovery available', async () => {
  const ui = page({ replies: [{ status: 401 }], identity: new Error('Try email') });
  await flush();
  await ui.fire('verify-push-button');
  assert.equal(ui.requests.length, 1);
  assert.equal(ui.elements.get('manage-status').textContent, 'Try email');
  assert.equal(ui.elements.get('verify-email-button').disabled, false);
});

test('updates after recovery include the session csrf token', async () => {
  const ui = page({ replies: [{ status: 200, body: preferences }, { status: 200, body: preferences }] });
  await flush();
  await ui.fire('manage-form', 'submit');
  assert.equal(ui.requests[1].method, 'PATCH');
  assert.equal(ui.requests[1].headers['X-CSRF-Token'], 'csrf');
});

test('rate limiting gives a retry message and re-enables recovery buttons', async () => {
  const ui = page({ replies: [{ status: 401 }, { status: 429 }] });
  await flush();
  await ui.fire('recovery-email-form', 'submit');
  assert.match(ui.elements.get('manage-status').textContent, /wait a few minutes/);
  assert.equal(ui.elements.get('verify-email-button').disabled, false);
});
