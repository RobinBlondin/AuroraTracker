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

    importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
    importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

firebase.initializeApp({
    apiKey: FIREBASE_API_KEY,
    authDomain: FIREBASE_AUTH_DOMAIN,
    projectId: FIREBASE_PROJECT_ID,
    storageBucket: FIREBASE_STORAGE_BUCKET,
    messagingSenderId: FIREBASE_MESSAGING_SENDER_ID,
    appId: FIREBASE_APP_ID,
    measurementId: FIREBASE_MEASUREMENT_ID
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(payload => {
    console.log("FCM background message:", payload);
    const { title, body } = payload.notification;
    self.registration.showNotification(title, { body });
});

