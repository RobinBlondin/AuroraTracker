document.addEventListener("DOMContentLoaded", () => {
    const defaultLat = 59.3293; // fallback (Stockholm)
    const defaultLng = 18.0686;

    const map = L.map("map").setView([defaultLat, defaultLng], 5);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const marker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(map);
    const latInput = document.getElementById("latitude");
    const lngInput = document.getElementById("longitude");

    function updateInputs(lat, lng) {
        latInput.value = lat;
        lngInput.value = lng;
    }
    updateInputs(defaultLat, defaultLng);

    marker.on("dragend", e => {
        const { lat, lng } = e.target.getLatLng();
        updateInputs(lat, lng);
    });

    map.on("click", e => {
        const { lat, lng } = e.latlng;
        marker.setLatLng([lat, lng]);
        updateInputs(lat, lng);
    });

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(pos => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            map.setView([lat, lng], 13);
            marker.setLatLng([lat, lng]);
            updateInputs(lat, lng);
        }, () => {
            console.warn("User denied or failed geolocation, using fallback location");
        });
    } else {
        console.warn("Geolocation not supported by this browser, using fallback location");
    }
});

window.addEventListener('DOMContentLoaded', () => {
    const alerts = document.querySelectorAll('.alert-message');
    alerts.forEach(alert => {
        if (alert.style.display !== 'none') {
            setTimeout(() => {
                alert.style.display = 'none';
            }, 5000);
        }
    });
});
