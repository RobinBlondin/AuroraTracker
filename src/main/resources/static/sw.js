self.addEventListener("install", (event) => {
    console.log("[Service Worker] Installed");
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    console.log("[Service Worker] Activated");
    return self.clients.claim();
});

self.addEventListener("push", (event) => {
    const title =  "Aurora Alert";
    const options = {
        body:  "Aurora activity has been detected near your location!",
        icon: "/images/icon-192.png",
        badge: "/images/icon-192.png",
        data:  "/"
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
    event.notification.close();
    event.waitUntil(clients.openWindow(event.notification.data));
});

/* For Chrome users via FCM */
importScripts('/config.js');
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

firebase.initializeApp(self.APP_CONFIG);

const messaging = firebase.messaging();

messaging.onBackgroundMessage(payload => {
    console.log("FCM background message:", payload);
    const { title, body } = payload.notification;
    self.registration.showNotification(title, { body });
});

