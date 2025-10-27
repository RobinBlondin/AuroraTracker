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
