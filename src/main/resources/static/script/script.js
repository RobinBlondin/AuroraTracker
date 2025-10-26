var map = L.map("map").setView([51.505, -0.09], 13);

L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 20,
  attribution:
    '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);


document.addEventListener("DOMContentLoaded", async () => {
    let userId = localStorage.getItem("userId")
    if (!userId) {
        userId = crypto.randomUUID()
        localStorage.setItem("userId", userId)
    }

    const response = await fetch("/api/subscriptions/" + userId)
        .then(data => {
        })

const updateLocation = () => {
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      console.log(pos.coords.latitude, pos.coords.longitude);
      L.marker([pos.coords.latitude, pos.coords.longitude]).addTo(map);
      map.setView([pos.coords.latitude, pos.coords.longitude]);
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

//rensa vid varje knapptryuck i updatelocation och unsub vid start ska inte vara någon karta vid unsub
//ska kartan försvvinna som vid start

