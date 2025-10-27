/* ===== Leaflet map functions ===== */

const map = L.map("map").setView([51.505, -0.09], 13);

L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 20,
    attribution: "OpenStreetMap",
}).addTo(map);

let currentMarker = null;

function placeMarker(lat, lon) {
    if (currentMarker) {
        map.removeLayer(currentMarker);
    }
    currentMarker = L.marker([lat, lon]).addTo(map);
    map.setView([lat, lon]);
}

const unSubscribeOnLocation = async () => {
    let userId = localStorage.getItem("userId");
    await unsubscribeServiceWorker()

    const response = await fetch(
        "http://localhost:8080/api/subscriptions/unsubscribe/" + userId,
        {
            method: "DELETE",
        }
    );

    if (response.ok) {
        toggleDisplayMap("none")
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

const subscribe = async (lat, lon) => {
    const registration = await unsubscribeServiceWorker()

    const push = (await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(PUBLIC_KEY)
    })).toJSON()

    return {
        endPoint: push.endpoint,
        auth: push.keys.auth,
        p256dh: push.keys.p256dh,
        userId: localStorage.getItem("userId"),
        lat: lat,
        lon: lon,
    };
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

        const positionString = `Latitude: ${user.lat.toFixed(
            4
        )}, Longitude: ${user.lon.toFixed(4)}`;
        UI.setText(".position-data", positionString);
    } else {
        toggleDisplayMap("none");
    }
};

document.addEventListener("DOMContentLoaded", async () => {
    let userId = localStorage.getItem("userId");
    if (!userId) {
        userId = crypto.randomUUID();
        localStorage.setItem("userId", userId);
    }

    console.log("PUBLIC_KEY", PUBLIC_KEY, PUBLIC_KEY.length);

    await navigator.serviceWorker.register("/sw.js");
    console.log("SW registered");

    await navigator.serviceWorker.ready;
    await fetchUserDataAndUpdateElements(userId);
});
