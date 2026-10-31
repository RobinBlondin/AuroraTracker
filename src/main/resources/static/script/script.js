
const globeEl = document.getElementById("globeViz");
const globeParent = globeEl.parentElement;

let globe;

function initializeGlobe() {
    globe = Globe()(globeEl)
        .globeImageUrl(
            "https://cdn.jsdelivr.net/npm/three-globe/example/img/earth-dark.jpg"
        )
        .width(globeParent.clientWidth)
        .height(globeParent.clientHeight)
        .pointOfView({
            lat: 59.34,
            lng: 18.05,
            altitude: 2
        }, 1000);

    globe.controls().autoRotate = true;
    globe.controls().autoRotateSpeed = 0.2;

    fetchAndPrintAuroraPoints();
}

async function fetchAndPrintAuroraPoints() {
    if (!globe) return;

    try {
        const response = await fetch("/api/points/all", {
            method: "GET",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error(
                `Failed to fetch aurora points: ${response.status}`
            );
        }

        const points = await response.json();

        if (!Array.isArray(points) || points.length === 0) {
            globe.pointsData([]);
            return;
        }

        const maxProb = Math.max(
            0,
            ...points.map(p => Number(p.probability) || 0)
        );

        globe
            .pointsData(points)
            .pointLat(d => d.lat)
            .pointLng(d => d.lon)
            .pointAltitude(0.001)
            .pointRadius(d => {
                const latFactor = Math.cos(
                    (d.lat * Math.PI) / 180
                );

                return Math.max(0, 0.4 * latFactor);
            })
            .pointColor(d => {
                const norm = maxProb > 0
                    ? Math.max(0, Math.min(1, d.probability / maxProb))
                    : 0;

                const opacity = Math.pow(norm, 2);

                return `rgba(0, 255, 100, ${opacity})`;
            })
            .pointsMerge(true);

    } catch (error) {
        console.error("Failed to update aurora points:", error);
    }
}

function resizeGlobe() {
    if (!globe) return;

    globe
        .width(globeParent.clientWidth)
        .height(globeParent.clientHeight);
}

function goToSignUpPage() {
    window.location.href = "/signup";
}

window.goToSignUpPage = goToSignUpPage;

function initializeApp() {
    initializeGlobe();

    window.addEventListener("resize", resizeGlobe);

    setInterval(fetchAndPrintAuroraPoints, 15 * 60 * 1000);
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeApp);
} else {
    initializeApp();
}
