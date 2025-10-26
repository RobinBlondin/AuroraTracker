var map = L.map("map").setView([51.505, -0.09], 13);

L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 20,
  attribution:
    '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);


const updateLocation = async () => {
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
        const lat = pos.coords.latitude
        const lon = pos.coords.longitude
      L.marker([lat, lon]).addTo(map);
      map.setView([lat, lon]);
        const subscription = await subscribe(lat, lon);
        await postSubscription(subscription)
        toggleDisplayMap()
    },
    (err) => {
      console.error(err);
    }
  );
};

const unSubLocation = () => {
  L.marker([], 13).addTo(map);
  map.setView([], 13);
};

const setPinOnMap = (lat, lon) => {
    L.marker([lat, lon]).addTo(map);
    map.setView([lat, lon]);
}

const updateElementText = (className, textContent) => {
    const element = document.querySelector(className)
    element.textContent = textContent
}

const formatDateToString = (dateString) => {
    const arr = new Date(dateString).toISOString().split("T")
    const date = arr[0]
    const time = arr[1].substring(0, 5)
   return `${date} ${time}`
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


const subscribe = async (lat, lon) => {
    let sw = await navigator.serviceWorker.ready
    let key = urlBase64ToUint8Array(PUBLIC_KEY)

    let push = (await sw.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: key
    }))

    push = await push.toJSON()

    return {
        endPoint: push.endpoint,
        auth: push.keys.auth,
        p256dh: push.keys.p256dh,
        userId: localStorage.getItem("userId"),
        lat: lat,
        lon: lon
    }
}

const postSubscription = async (sub) =>  {
    fetch("/api/subscriptions/subscribe", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(sub)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Request failed")
            }
            return response.json()
        })
        .then(result => {
            console.log("Server svar:", result)
        })
        .catch(err => {
            console.error(err)
        })

}

const fetchUserDataAndUpdateElements = async (userId) => {
    const response = await fetch("/api/subscriptions/" + userId)
    if (response.status === 200) {
        const user = await response.json()
        setPinOnMap(user.lat, user.lon)

        if (user.lastNotificationTime) {
            const timeString = formatDateToString(user.lastNotificationTime)
            updateElementText(".notification-timestamp", timeString)
        }

        const positionString = `Latitude: ${user.lat.toFixed(4)}, Longitude: ${user.lon.toFixed(4)}`
        updateElementText(".position-data", positionString)
    } else {
        toggleDisplayMap()
    }
}

const toggleDisplayMap = () => {
    const mapElement = document.getElementById("map")
    mapElement.style.display = mapElement.style.display === "none"? "block" : "none"
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