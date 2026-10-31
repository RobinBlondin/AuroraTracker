import { subscribePush } from "./push.js";

const form = document.getElementById("signup-form");
const emailInput = document.getElementById("email");
const locationButton = document.getElementById("get-location");
const locationStatus = document.getElementById("location-status");
const signupStatus = document.getElementById("signup-status");
const submitButton = document.getElementById("submit-button");

function getNotificationMethod() {
    return document.querySelector(
        'input[name="method"]:checked'
    ).value;
}

function updateEmailVisibility() {
    const method = getNotificationMethod();
    const emailEnabled = method === "email" || method === "both";

    emailInput.disabled = !emailEnabled;
    emailInput.required = emailEnabled;
}

document.querySelectorAll('input[name="method"]').forEach(radio => {
    radio.addEventListener("change", updateEmailVisibility);
});

updateEmailVisibility();

locationButton.addEventListener("click", getUserLocation);

function getUserLocation() {
    if (!navigator.geolocation) {
        locationStatus.textContent = "Geolocation is not supported.";
        return;
    }

    locationButton.disabled = true;
    locationStatus.textContent = "Getting your location...";

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const lat = position.coords.latitude.toFixed(3);
            const lon = position.coords.longitude.toFixed(3);

            document.getElementById("lat").value = lat;
            document.getElementById("lon").value = lon;

            locationStatus.textContent = "Location retrieved!";
            locationButton.disabled = false;
        },
        (error) => {
            console.error("Geolocation error:", error);

            locationStatus.textContent =
                "Could not retrieve location. Please try again.";

            locationButton.disabled = false;
        },
        {
            enableHighAccuracy: false,
            timeout: 10000,
            maximumAge: 60000
        }
    );
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const method = getNotificationMethod();

    const lat = parseFloat(document.getElementById("lat").value);
    const lon = parseFloat(document.getElementById("lon").value);

    if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
        signupStatus.textContent = "Please select your location.";
        return;
    }

    const email = emailInput.value.trim();
    const level = document.querySelector(
        'input[name="level"]:checked'
    ).value;

    const wantsPush = method === "push" || method === "both";
    const wantsEmail = method === "email" || method === "both";

    submitButton.disabled = true;
    submitButton.textContent = "Enabling alerts...";
    signupStatus.textContent = "";

    try {
        const subscription = {
            email: wantsEmail ? email : null,
            notificationLevel: level,
            lat,
            lon
        };

        if (wantsPush) {
            signupStatus.textContent =
                "Please allow notifications in your browser.";

            const pushData = await subscribePush();

            Object.assign(subscription, pushData);
        }

        const response = await fetch("/api/subscriptions/subscribe", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Subscription-Request": "1"
            },
            body: JSON.stringify(subscription)
        });

        if (!response.ok) {
            if (response.status === 429) {
                throw new Error(
                    "Too many requests. Please wait before trying again."
                );
            }

            if (response.status === 409) {
                throw new Error("This browser is already registered. Open Manage alerts to verify and update your subscription.");
            }
            throw new Error("Failed to register subscription. Please check your details and try again.");
        }

        const needsVerification = response.status === 202;
        signupStatus.textContent = needsVerification
            ? "Check your email to verify your subscription. If you already registered, use Manage alerts to request a management link."
            : "Alerts successfully enabled!";
        submitButton.textContent = needsVerification ? "Check your email" : "Alerts enabled";

    } catch (error) {
        console.error("Registration failed:", error);

        signupStatus.textContent = error.message;
        submitButton.disabled = false;
        submitButton.textContent = "Enable alerts";
    }
});