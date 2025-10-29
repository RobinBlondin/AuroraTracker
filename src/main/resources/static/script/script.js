const config = self.APP_CONFIG
const keys = self.APP_KEYS;

firebase.initializeApp(config);
const messaging = firebase.messaging();

navigator.serviceWorker.register("/sw.js").then((registration) => {
    messaging.useServiceWorker(registration);
});

/* ===== Leaflet map functions ===== */

const map = L.map("map").setView([51.505, -0.09], 12);

L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; <a href="https://carto.com/attributions">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 19
}).addTo(map);

let currentMarker = null;

function placeMarker(lat, lon) {
    if (currentMarker) {
        map.removeLayer(currentMarker);
    }
    const icon = L.icon({
        iconUrl: '/images/pin-icon.png',
        iconSize: [41, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34]
    });

    currentMarker = L.marker([lat, lon], { icon }).addTo(map);
    map.setView([lat, lon]);
}

const unSubscribeOnLocation = async () => {
    let userId = localStorage.getItem("userId");
    await unsubscribeServiceWorker()

    const response = await fetch(
        "/api/subscriptions/unsubscribe/" + userId,
        {
            method: "DELETE",
        }
    );

    if (response.ok) {
        toggleDisplayMap("none")
        UI.setText(".position-data", "No current position selected")
    }
};

const toggleDisplayMap = (style) => {
    const mapElement = document.getElementById("map");
    mapElement.style.display = style;
};

const updateLocation = async () => {
    navigator.geolocation.getCurrentPosition(
        async (pos) => {
            const lat = pos.coords.latitude;
            const lon = pos.coords.longitude;
            placeMarker(lat, lon);
            const subscription = await subscribe(lat, lon);
            await saveSubscription(subscription);
            toggleDisplayMap("block");
            UI.setText(".position-data", createPositionString(lat, lon))
        },
        (err) => {
            console.error(err);
        }
    );
};

/* =====  Helper functions ===== */

const UI = {
    setText(selector, text) {
        document.querySelector(selector).textContent = text;
    },
    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toISOString().slice(0, 16).replace("T", " ");
    },
};

function urlBase64ToUint8Array(base64String) {
    const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
    const rawData = atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
}

/* ===== Push notification logic ===== */

async function subscribeWebPush(lat, lon) {
    const registration = await navigator.serviceWorker.ready;
    let subscription = await registration.pushManager.getSubscription();
    if (!subscription) {
        subscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(keys.webPushVapidKey)
        });
    }

    const push = subscription.toJSON();
    return {
        endpoint: push.endpoint,
        auth: push.keys.auth,
        p256dh: push.keys.p256dh,
        userId: localStorage.getItem("userId"),
        lat,
        lon,
    };
}

async function subscribeFCM(lat, lon) {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") throw new Error("Permission denied");

    const token = await messaging.getToken({
        vapidKey: keys.firebaseVapidKey
    });

    return {
        firebaseToken: token,
        userId: localStorage.getItem("userId"),
        lat,
        lon,
    };
}

const subscribe = async (lat, lon) => {
    let payload = null;

    if (isChrome()) {
        payload = await subscribeFCM(lat, lon);
    } else {
        payload = await subscribeWebPush(lat, lon);
    }

    return payload;
};



async function saveSubscription(data) {
    const res = await fetch("/api/subscriptions/subscribe", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(data),
    });

    if (!res.ok) throw new Error("Request failed");
    return res.json();
}

async function unsubscribeServiceWorker() {
    const registration = await navigator.serviceWorker.ready;
    const existing = await registration.pushManager.getSubscription();
    if (existing) {
        await existing.unsubscribe();
        console.log("Old subscription removed");
    }

    return registration
}

/* ===== UI functions ===== */

const fetchUserDataAndUpdateElements = async (userId) => {
    const response = await fetch("/api/subscriptions/" + userId);
    if (response.status === 200) {
        const user = await response.json();
        placeMarker(user.lat, user.lon);

        if (user.lastNotificationTime) {
            UI.setText(
                ".notification-timestamp",
                UI.formatDate(user.lastNotificationTime)
            );
        }

        UI.setText(".position-data", createPositionString(user.lat, user.lon));
    } else {
        toggleDisplayMap("none");
    }
};

function createPositionString(lat, lon) {
    return  `Latitude: ${lat.toFixed(
        4
    )}, Longitude: ${lon.toFixed(4)}`;
}

function isChrome() {
    return /Chrome/.test(navigator.userAgent) && /Google Inc/.test(navigator.vendor);
}

document.addEventListener("DOMContentLoaded", async () => {
    console.log("script laddat")
    let userId = localStorage.getItem("userId");
    if (!userId) {
        userId = crypto.randomUUID();
        localStorage.setItem("userId", userId);
    }

    await navigator.serviceWorker.register("/sw.js");
    console.log("SW registered");

    await navigator.serviceWorker.ready;
    await fetchUserDataAndUpdateElements(userId);
});
