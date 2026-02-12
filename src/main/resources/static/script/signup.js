function getUserLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition((position) => {
            // Round to ~3 decimals (~100m)
            const lat = position.coords.latitude.toFixed(3);
            const lon = position.coords.longitude.toFixed(3);

            document.getElementById('lat').value = lat;
            document.getElementById('lon').value = lon;
        }, (err) => {
            alert("Couldn't get your location. Please allow location access.");
        });
    } else {
        alert("Geolocation not supported by your browser.");
    }
}