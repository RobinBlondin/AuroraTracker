/* ===== Leaflet map functions ===== */

const map = L.map("map").setView([51.505, -0.09], 13);

L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 20,
  attribution: "OpenStreetMap"
}).addTo(map);

let currentMarker = null

function placeMarker(lat, lon) {
    if (currentMarker) {
        map.removeLayer(currentMarker)
    }
    currentMarker = L.marker([lat, lon]).addTo(map)
    map.setView([lat, lon])
}

const unSubLocation = () => {
    L.marker([], 13).addTo(map);
    map.setView([], 13);
};

const toggleDisplayMap = (style) => {
    const mapElement = document.getElementById("map")
    mapElement.style.display = style
}

const updateLocation = async () => {
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
        const lat = pos.coords.latitude
        const lon = pos.coords.longitude
        placeMarker(lat, lon)
        const subscription = await subscribe(lat, lon);
        await saveSubscription(subscription)
        toggleDisplayMap("block")
    },
    (err) => {
      console.error(err);
    }
  );
};



/* =====  Helper functions ===== */

const UI = {
    setText(selector, text) {
        document.querySelector(selector).textContent = text
    },
    formatDate(dateString) {
        const date = new Date(dateString)
        return date.toISOString().slice(0,16).replace("T"," ")
    }
}

function urlBase64ToUint8Array(base64String) {
    const padding = "=".repeat((4 - base64String.length % 4) % 4)
    const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/")
    const rawData = atob(base64)
    const outputArray = new Uint8Array(rawData.length)

    for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i)
    }
    return outputArray
}



/* ===== Push notification logic ===== */

const subscribe = async (lat, lon) => {
    let sw = await navigator.serviceWorker.ready
    let key = urlBase64ToUint8Array(PUBLIC_KEY)

    let push = (await sw.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: key
    })).toJSON()

    return {
        endPoint: push.endpoint,
        auth: push.keys.auth,
        p256dh: push.keys.p256dh,
        userId: localStorage.getItem("userId"),
        lat: lat,
        lon: lon
    }
}

async function saveSubscription(data) {
    const res = await fetch("/api/subscriptions/subscribe", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(data)
    })

    if (!res.ok) throw new Error("Request failed")
    return res.json()
}



/* ===== UI functions ===== */

const fetchUserDataAndUpdateElements = async (userId) => {
    const response = await fetch("/api/subscriptions/" + userId)
    if (response.status === 200) {
        const user = await response.json()
        placeMarker(user.lat, user.lon)

        if (user.lastNotificationTime) {
            UI.setText(".notification-timestamp", UI.formatDate(user.lastNotificationTime))
        }

        const positionString = `Latitude: ${user.lat.toFixed(4)}, Longitude: ${user.lon.toFixed(4)}`
        UI.setText(".position-data", positionString)
    } else {
        toggleDisplayMap("none")
    }
}



document.addEventListener("DOMContentLoaded", async () => {
    let userId = localStorage.getItem("userId")
    if (!userId) {
        userId = crypto.randomUUID()
        localStorage.setItem("userId", userId)
    }

    await fetchUserDataAndUpdateElements(userId)
    await navigator.serviceWorker.register("/sw.js")
    console.log("SW registered")
})