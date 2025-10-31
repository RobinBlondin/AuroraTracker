const config = self.APP_CONFIG
const keys = self.APP_KEYS;

firebase.initializeApp(config);
const messaging = firebase.messaging();

navigator.serviceWorker.register("/sw.js").catch(err => {
    console.error("Service worker registration failed:", err);
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

    currentMarker = L.marker([lat, lon], {icon}).addTo(map);
    map.setView([lat, lon]);
}

const unSubscribeOnLocation = async () => {
    let userId = localStorage.getItem("userId");
    toggleButtonColors("green")
    await unsubscribeServiceWorker()

    const response = await fetch(
        "/api/subscriptions/unsubscribe/" + userId,
        {
            method: "DELETE",
            headers: {
                "X-Request-ID":keys.secretKey
            }
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
    toggleButtonColors("red");

    navigator.geolocation.getCurrentPosition(
        async (pos) => {
            const lat = pos.coords.latitude;
            const lon = pos.coords.longitude;

            placeMarker(lat, lon);
            const subscription = await subscribe(lat, lon);

            if (subscription == null) throw Error("Unsupported browser");

            await saveSubscription(subscription);
            toggleDisplayMap("block");
            UI.setText(".position-data", createPositionString(lat, lon));
        },
        (err) => {
            console.error("Geolocation error:", err);

            switch (err.code) {
                case err.PERMISSION_DENIED:
                    showMapWarning("Location access denied. Turn on location to enable alerts.", 6000);
                    break;
                case err.POSITION_UNAVAILABLE:
                    showMapWarning("Unable to retrieve location. Check your GPS or connection.", 6000);
                    break;
                case err.TIMEOUT:
                    showMapWarning("Location request timed out. Try again.", 6000);
                    break;
                default:
                    showMapWarning("An unknown error occurred while getting location.", 6000);
                    break;
            }

            toggleButtonColors("green");
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
        lat: lat,
        lon: lon,
    };
}

async function subscribeFCM(lat, lon) {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") throw new Error("Permission denied");

    const registration = await navigator.serviceWorker.ready;
    const token = await messaging.getToken({
        vapidKey: keys.firebaseVapidKey,
        serviceWorkerRegistration: registration
    });

    return {
        firebaseToken: token,
        userId: localStorage.getItem("userId"),
        lat: lat,
        lon: lon,
    };
}

const subscribe = async (lat, lon) => {
    try {
        const notifPermission = await Notification.requestPermission();
        if (notifPermission !== "granted") {
            showMapWarning("Notifications blocked. Enable notifications to receive alerts.", 6000);
            return null;
        }

        const type = await getPushType();

        switch (type) {
            case "fcm":
                return await subscribeFCM(lat, lon);
            case "webpush":
                return await subscribeWebPush(lat, lon);
            default:
                showMapWarning("Your browser does not support push notifications.", 6000);
                return null;
        }
    } catch (err) {
        console.error(err);
        showMapWarning("Failed to enable alerts. Please try again later.", 6000);
    }
};


async function saveSubscription(data) {
    const res = await fetch("/api/subscriptions/subscribe", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-Request-ID": keys.secretKey
        },
        body: JSON.stringify(data),
    });
    if(res.status === 429) showMapWarning("Too many requests, wait 1 min before updating location again", 5000)
    if (!res.ok) throw new Error("Request failed");
    return res.json();
}

async function unsubscribeServiceWorker() {
    const registration = await navigator.serviceWorker.ready;
    const existing = await registration.pushManager.getSubscription();
    if (existing) {
        await existing.unsubscribe();
    }
    return registration
}

/* ===== UI functions ===== */

const fetchUserDataAndUpdateElements = async (userId) => {
    try {
        const response = await fetch(`/api/subscriptions/${userId}`, {
            headers: { "X-Request-ID": keys.secretKey }
        });

        if (!response.ok) {
            toggleDisplayMap("none");
            toggleButtonColors("green")
            return;
        }

        const user = await response.json();
        placeMarker(user.lat, user.lon);
        toggleButtonColors("red")
        if (user.lastNotificationTime) {
            UI.setText(".notification-timestamp", UI.formatDate(user.lastNotificationTime));
        }
        UI.setText(".position-data", createPositionString(user.lat, user.lon));
    } catch (err) {
        console.error("Failed to fetch subscription:", err);
        toggleDisplayMap("none");
    }
};

function toggleButtonColors(active) {
    const greenButton = document.querySelector(".sub-button");
    const redButton = document.querySelector(".unsub-button");

    if (active === "green") {
        greenButton.style.backgroundColor = "#00f5a0";
        greenButton.style.boxShadow =
            "0 0 6px rgba(0, 255, 160, 0.4), 0 0 12px rgba(0, 255, 160, 0.2)";

        redButton.style.backgroundColor = "#7a2222";
        redButton.style.boxShadow = "none";
    } else {
        greenButton.style.backgroundColor = "#006b49";
        greenButton.style.boxShadow = "none";

        redButton.style.backgroundColor = "#ff4b4b";
        redButton.style.boxShadow =
            "0 0 6px rgba(255, 75, 75, 0.5), 0 0 12px rgba(255, 75, 75, 0.3)";
    }
}


function createPositionString(lat, lon) {
    return `Latitude: ${lat.toFixed(
        4
    )}, Longitude: ${lon.toFixed(4)}`;
}

function showMapWarning(message, durationMs = 5000) {
    const box = document.getElementById("map-warning");
    const text = document.getElementById("map-warning-text");

    text.textContent = message;
    box.classList.remove("hidden");

    if (durationMs > 0) {
        setTimeout(() => hideMapWarning(), durationMs);
    }
}

function hideMapWarning() {
    document.getElementById("map-warning").classList.add("hidden");
}

async function getPushType() {
    if (navigator.userAgentData?.brands) {
        const brands = navigator.userAgentData.brands.map(b => b.brand);
        if (brands.some(b => /Chromium|Google Chrome|Microsoft Edge|Brave|Opera/i.test(b))) {
            return 'fcm';
        }
    }

    const ua = navigator.userAgent;
    const vendor = navigator.vendor || '';

    if (/Safari/i.test(ua) && /Apple/i.test(vendor) && !/Chrom(e|ium)/i.test(ua)) {
        return 'webpush';
    }

    if (/Firefox/i.test(ua)) {
        return 'webpush';
    }

    if (/Chrom(e|ium)/i.test(ua)) {
        return 'fcm';
    }

    return 'unsupported';
}
async function drawHeatLayer() {
    const data = await fetchAuroraPoints();
    const points = data.map(p => [p.lat, p.lon, p.probability / 100]);

    L.heatLayer(points, {
        radius: 30,
        blur: 25,
        maxZoom: 6,
        gradient: {
            0.2: 'transparent',
            0.4: '#32CD32',
            0.6: '#00FF7F',
            0.8: '#ADFF2F',
            1.0: '#C0FF00'
        }
    }).addTo(map)
}

async function fetchAuroraPoints() {
    const response = await fetch("/api/points/all")
    return await response.json()
}

document.addEventListener("DOMContentLoaded", async () => {
    let userId = localStorage.getItem("userId");
    if (!userId) {
        userId = crypto.randomUUID();
        localStorage.setItem("userId", userId);
    }

    await navigator.serviceWorker.register("/sw.js");
    await navigator.serviceWorker.ready;
    await fetchUserDataAndUpdateElements(userId);
});
