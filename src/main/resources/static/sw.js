self.addEventListener("install", (event) => {
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    return self.clients.claim();
});

self.addEventListener("push", (event) => {
    if (!event.data) return;
    let data = {};
    try {
        data = event.data.json();
    } catch {
        data = { body: event.data.text() };
    }

    if (data.data) data = data.data;

    const title = data.title || "Aurora Alert";
    const options = {
        body: data.body || "Aurora activity has been detected near your location!",
        icon: "/images/icon-192.png",
        badge: "/images/icon-192.png",
        data: data.url || "/",
        tag: data.type === "management" ? "aurora_management" : "aurora_alert"
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
    event.notification.close();
    let url;
    try {
        url = new URL(event.notification.data || "/", self.location.origin);
        if (url.origin !== self.location.origin) return;
    } catch {
        return;
    }
    event.waitUntil(clients.openWindow(url.href));
});

importScripts("/config.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

firebase.initializeApp(self.APP_CONFIG);
const messaging = firebase.messaging();

messaging.onBackgroundMessage(() => {});
