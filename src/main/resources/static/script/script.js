const config = self.APP_CONFIG
const keys = self.APP_KEYS;

// navigator.serviceWorker.register("/sw.js").catch(err => {
//      console.error("Service worker registration failed:", err);
//  });

const globeEl = document.getElementById("globeViz");
const globeParent = globeEl.parentElement;
let globe;


const initializeGlobe = async () => {
    //const coords = await getUserCountryCoordinates();

    globe = Globe()(document.getElementById("globeViz"))
        .globeImageUrl(
            "https://cdn.jsdelivr.net/npm/three-globe/example/img/earth-dark.jpg",
        )
        .width(globeParent.clientWidth)
        .height(globeParent.clientHeight)
        .pointOfView({ lat: 59.34, lng: 18.05, altitude: 2 }, 1000);

    globe.controls().autoRotate = true
    globe.controls().autoRotateSpeed = 0.2

    fetchAndPrintAuroraPoints();
}


initializeGlobe();
window.addEventListener("resize", () => {
    if (!globe) return;
    globe.width(globeEl.clientWidth).height(globeEl.clientHeight);
});

setInterval(fetchAndPrintAuroraPoints, 60000 * 5);

function fetchAndPrintAuroraPoints() {
    fetch("/api/points/all", {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "x-request-id": "630d768e-ca2f-4241-b396-96de0e44b644",
        },
        mode: "cors",
    })
        .then((res) => res.json())
        .then((points) => {
            const maxProb = Math.max(...points.map((p) => p.probability));

            globe
                .pointsData(points)
                .pointLat((d) => d.lat)
                .pointLng((d) => d.lon)
                .pointAltitude(0.001)
                .pointRadius((d) => {
                    const latFactor = Math.cos((d.lat * Math.PI) / 180);
                    return 0.4 * latFactor;
                })
                .pointColor((d) => {
                    const norm = d.probability / maxProb;
                    const opacity = norm;
                    return `rgba(0, 255, 100, ${opacity})`;
                })
                .pointsMerge(true);
        });
}

window.addEventListener('orientationchange', () => {
    window.location.reload()
});


async function getUserCountryCoordinates() {
    try {
        const response = await fetch('https://ipapi.co/json/');
        const data = await response.json();

        console.log("Land:", data.country_name);
        console.log("Lat/Lon:", data.latitude, data.longitude);

        return {
            country: data.country_name,
            lat: Math.round(data.latitude * 100) / 100,
            lon: Math.round(data.longitude * 100) / 100,
        };
    } catch (error) {
        console.error("Misslyckades att hämta plats:", error);
        return null;
    }
}



async function unsubscribeServiceWorker() {
    const registration = await navigator.serviceWorker.ready;
    const existing = await registration.pushManager.getSubscription();
    if (existing) {
        await existing.unsubscribe();
    }
    return registration
}



const goToSignUpPage = () => {
    window.location.href = "/signup";
};

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

async function fetchAuroraPoints() {
    const response = await fetch("/api/points/all")
    return await response.json()
}

document.addEventListener("DOMContentLoaded", async () => {
    // await navigator.serviceWorker.register("/sw.js");
    // await navigator.serviceWorker.ready;
});
