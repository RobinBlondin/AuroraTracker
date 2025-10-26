var map = L.map("map").setView([51.505, -0.09], 13);

L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 20,
  attribution:
    '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);


const updateLocation = async () => {

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      L.marker([pos.coords.latitude, pos.coords.longitude]).addTo(map);
      map.setView([pos.coords.latitude, pos.coords.longitude]);
    },
    (err) => {
      console.error(err);
    }
  );

    await subscribe()
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

const subscribe = async () => {
    const key = `[[${key}]]`;
    let sw = await navigator.serviceWorker.ready
    let push = await sw.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: key
    })

    console.log(push)
}

document.addEventListener("DOMContentLoaded", async () => {
    let userId = localStorage.getItem("userId")
    if (!userId) {
        userId = crypto.randomUUID()
        localStorage.setItem("userId", userId)
    }

    addEventListener('load', async () => {
        let sw  = await navigator.serviceWorker.register("script/sw.js")
    })

    const response = await fetch("/api/subscriptions/" + userId)

    if (response.status === 200) {

    const user = await response.json()


    console.log(user)
    setPinOnMap(user.lat, user.lon)
    if (user.lastNotificationTime) {
        const timeString = formatDateToString(user.lastNotificationTime)
        updateElementText(".notification-timestamp", timeString)
    }
    const positionString = `Latitude: ${user.lat.toFixed(4)}, Longitude: ${user.lon.toFixed(4)}`
    updateElementText(".position-data", positionString)
    } else {
        const mapElement = document.getElementById("map")
        mapElement.style.display = "none"
    }
})



//rensa vid varje knapptryuck i updatelocation och unsub vid start ska inte vara någon karta vid unsub
//ska kartan försvvinna som vid start

