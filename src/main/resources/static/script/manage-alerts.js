import { getExistingPushIdentity } from './push.js';

const form = document.getElementById('manage-form');
const status = document.getElementById('manage-status');
const locationStatus = document.getElementById('location-status');
const latInput = document.getElementById('lat');
const lonInput = document.getElementById('lon');
const saveButton = document.getElementById('save-button');
const unsubscribeButton = document.getElementById('unsubscribe-button');
const locationButton = document.getElementById('get-location');
const enabledInput = document.getElementById('enabled');
const endpoint = '/api/subscriptions/manage';
let csrfToken;
const recoveryOptions = document.getElementById('recovery-options');
const verifyLink = document.getElementById('verify-link');
const verifyLinkButton = document.getElementById('verify-link-button');
const pushButton = document.getElementById('verify-push-button');
const emailButton = document.getElementById('verify-email-button');
const emailForm = document.getElementById('recovery-email-form');
const linkToken = new URLSearchParams(window.location.hash.slice(1)).get('token');
if (window.location.hash) {
  window.history.replaceState(null, '', window.location.pathname + window.location.search);
}

function showRecovery() {
  form.hidden = true;
  verifyLink.hidden = true;
  recoveryOptions.hidden = false;
  csrfToken = undefined;
}

function setRecoveryBusy(busy) {
  pushButton.disabled = busy;
  emailButton.disabled = busy;
  verifyLinkButton.disabled = busy;
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: { 'Accept': 'application/json', ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}), ...(options.headers || {}) }
  });
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      showRecovery();
      throw new Error('Verify your subscription to continue. Your previous session or link may have expired.');
    }
    if (response.status === 429) throw new Error('Too many requests. Please wait a few minutes and try again.');
    throw new Error(`Request failed (${response.status}). Please try again.`);
  }
  return response.status === 204 || response.status === 202 ? null : response.json();
}

function setBusy(busy) {
  saveButton.disabled = busy;
  unsubscribeButton.disabled = busy;
  locationButton.disabled = busy;
}

async function load() {
  try {
    const data = await request(endpoint);
    csrfToken = data.csrfToken;
    latInput.value = Number(data.lat).toFixed(3);
    lonInput.value = Number(data.lon).toFixed(3);
    const level = [...document.querySelectorAll('input[name="level"]')]
      .find(input => input.value === data.notificationLevel);
    if (level) level.checked = true;
    recoveryOptions.hidden = true;
    verifyLink.hidden = true;
    form.hidden = false;
    status.textContent = '';
  } catch (error) {
    status.textContent = error.message;
  }
}

locationButton.addEventListener('click', () => {
  if (!navigator.geolocation) {
    locationStatus.textContent = 'Geolocation is not supported.';
    return;
  }
  locationButton.disabled = true;
  locationStatus.textContent = 'Getting your location...';
  navigator.geolocation.getCurrentPosition(position => {
    latInput.value = position.coords.latitude.toFixed(3);
    lonInput.value = position.coords.longitude.toFixed(3);
    locationStatus.textContent = 'Location updated. Remember to save.';
    locationButton.disabled = false;
  }, error => {
    locationStatus.textContent = error.code === 1
      ? 'Location permission denied.' : 'Could not retrieve location.';
    locationButton.disabled = false;
  }, { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 });
});

form.addEventListener('submit', async event => {
  event.preventDefault();
  const lat = Number(latInput.value);
  const lon = Number(lonInput.value);
  const level = document.querySelector('input[name="level"]:checked')?.value;
  if (!Number.isFinite(lat) || !Number.isFinite(lon) ||
      lat < -90 || lat > 90 || lon < -180 || lon > 180 || !level) {
    status.textContent = 'Please provide a valid location and notification level.';
    return;
  }
  setBusy(true);
  status.textContent = 'Saving...';
  try {
    await request(endpoint, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ lat, lon, notificationLevel: level })
    });
    status.textContent = 'Your alert preferences have been saved!';
  } catch (error) {
    status.textContent = error.message;
  } finally {
    setBusy(false);
  }
});

unsubscribeButton.addEventListener('click', async () => {
  if (!window.confirm('Unsubscribe from all Aurora Tracker alerts?')) return;
  setBusy(true);
  status.textContent = 'Unsubscribing...';
  try {
    await request(endpoint, { method: 'DELETE' });
    form.hidden = true;
    status.textContent = 'You have been unsubscribed from Aurora Tracker alerts.'
  } catch (error) {
    status.textContent = error.message;
    setBusy(false);
  }
});

async function recoveryRequest(path, body) {
  return request(`${endpoint}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Management-Request': '1' },
    body: JSON.stringify(body)
  });
}

emailForm.addEventListener('submit', async event => {
  event.preventDefault();
  setRecoveryBusy(true);
  status.textContent = 'Requesting your link...';
  try {
    await recoveryRequest('email', { email: document.getElementById('recovery-email').value.trim() });
    status.textContent = 'If a subscription exists for this email, we have sent a management link. Check your inbox and spam folder.';
  } catch (error) {
    status.textContent = error.message;
  } finally {
    setRecoveryBusy(false);
  }
});

pushButton.addEventListener('click', async () => {
  setRecoveryBusy(true);
  status.textContent = 'Checking this browser...';
  try {
    const identity = await getExistingPushIdentity();
    await recoveryRequest('challenge', identity);
    status.textContent = 'If this browser has a matching subscription, a notification is on its way. Tap it to continue. If it does not arrive, try email.';
  } catch (error) {
    status.textContent = error.message;
  } finally {
    setRecoveryBusy(false);
  }
});

verifyLinkButton.addEventListener('click', async () => {
  setRecoveryBusy(true);
  status.textContent = 'Verifying your link...';
  try {
    await recoveryRequest('verify', { token: linkToken });
    await load();
  } catch (error) {
    status.textContent = error.message;
  } finally {
    setRecoveryBusy(false);
  }
});

if (linkToken) {
  verifyLink.hidden = false;
  status.textContent = '';
} else {
  load();
}

