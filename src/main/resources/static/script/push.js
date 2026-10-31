const keys = window.APP_KEYS;

let messaging;

function getMessaging() {
    if (!messaging) {
        if (!firebase.apps.length) {
            firebase.initializeApp(window.APP_CONFIG);
        }

        messaging = firebase.messaging();
    }

    return messaging;
}

function urlBase64ToUint8Array(base64String) {
    const padding = "=".repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
        .replace(/-/g, "+")
        .replace(/_/g, "/");

    const rawData = atob(base64);
    return Uint8Array.from(rawData, c => c.charCodeAt(0));
}

async function getPushType() {
    if (navigator.userAgentData?.brands) {
        const brands = navigator.userAgentData.brands.map(b => b.brand);

        if (brands.some(b =>
            /Chromium|Google Chrome|Microsoft Edge|Brave|Opera/i.test(b)
        )) {
            return "fcm";
        }
    }

    const ua = navigator.userAgent;
    const vendor = navigator.vendor || "";

    if (
        /Safari/i.test(ua) &&
        /Apple/i.test(vendor) &&
        !/Chrom(e|ium)/i.test(ua)
    ) {
        return "webpush";
    }

    if (/Firefox/i.test(ua)) {
        return "webpush";
    }

    if (/Chrom(e|ium)/i.test(ua)) {
        return "fcm";
    }

    return "unsupported";
}

async function subscribeFCM(registration) {
    const token = await getMessaging().getToken({
        vapidKey: keys.firebaseVapidKey,
        serviceWorkerRegistration: registration
    });

    if (!token) {
        throw new Error("Could not retrieve Firebase token");
    }

    return {
        firebaseToken: token
    };
}

async function subscribeWebPush(registration) {
    let subscription = await registration.pushManager.getSubscription();

    if (!subscription) {
        subscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(
                keys.webPushVapidKey
            )
        });
    }

    const push = subscription.toJSON();

    return {
        endpoint: push.endpoint,
        auth: push.keys.auth,
        p256dh: push.keys.p256dh
    };
}

export async function subscribePush() {
    if (
        !("serviceWorker" in navigator) ||
        !("PushManager" in window) ||
        !("Notification" in window)
    ) {
        throw new Error("Push notifications are not supported");
    }

    const permission = await Notification.requestPermission();

    if (permission !== "granted") {
        throw new Error("Notification permission denied");
    }

    const registration = await navigator.serviceWorker.register("/sw.js");
    await navigator.serviceWorker.ready;

    const type = await getPushType();

    switch (type) {
        case "fcm":
            return await subscribeFCM(registration);

        case "webpush":
            return await subscribeWebPush(registration);

        default:
            throw new Error("Unsupported browser");
    }
}

export async function getExistingPushIdentity() {
    if (!("serviceWorker" in navigator) || !("PushManager" in window) || !("Notification" in window) || Notification.permission !== "granted") {
        throw new Error("No active push subscription in this browser. Try verifying by email.");
    }
    const registration = await navigator.serviceWorker.getRegistration("/");
    if (!registration || !await registration.pushManager.getSubscription()) {
        throw new Error("No active push subscription in this browser. Try verifying by email.");
    }
    await registration.update();
    if (await getPushType() === "fcm") {
        const token = await getMessaging().getToken({
            vapidKey: keys.firebaseVapidKey,
            serviceWorkerRegistration: registration
        });
        if (!token) throw new Error("Could not verify this browser. Try email instead.");
        return { firebaseToken: token };
    }
    const subscription = await registration.pushManager.getSubscription();
    if (!subscription) throw new Error("Your push subscription is no longer active.");
    return { endpoint: subscription.endpoint };
}
