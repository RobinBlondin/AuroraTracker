document.addEventListener("DOMContentLoaded", async () => {
    let userId = localStorage.getItem("userId")
    if (!userId) {
        userId = crypto.randomUUID()
        localStorage.setItem("userId", userId)
    }

    const response = await fetch("/api/subscriptions/" + userId)
        .then(data => {

        })


})

const setPinOnMap = (lon, lat) => {
    L.marker([lat, lon]).addTo(map);
}

const updateLastPosition = (lon, lat) => {

}