# AuroraTracker
AuroraTracker is a Kotlin/Spring Boot service that monitors aurora (northern lights) conditions and notifies registered users by email when visibility is likely near their location. It combines NOAA auroral activity data, Kp index information, local weather/sunlight conditions, and distance from the closest aurora point to determine when to alert users.
## Inspiration
Have you ever missed seeing northern lights near your location and wished you had known in advance? I have, and that’s what inspired this application. I live in southern Sweden, where auroras are rare, so it’s practical to get a notification when conditions are optimal for viewing.
## Current status
Aurora Tracker has successfully completed a real world testing period with a limited group of users. Development is now focused on building a Progressive Web App that allows users to subscribe and receive push notifications directly on their devices. Further improvements will continue as the service evolves.
## Key Features
- User management (create, fetch, delete) via REST endpoints
- Scheduled aurora checks (every ~45 minutes)
- Aurora point ingestion from NOAA (coordinates with probability)
- Kp index retrieval and dynamic thresholds by latitude
- Visibility heuristics: elevation angle, max distance, probability, Kp, sunset and cloud cover
- Email notifications using JavaMailSender, with async sending
- Test coverage for core scheduling/notification logic (MockK + coroutines)

## Architecture
- **REST API**:
  - `UserController` exposes basic CRUD operations for users
- **Services**:
  - `TrackingService`: heart of the system; fetches aurora points and Kp index, evaluates users on schedule, and sends notifications
  - `UserService`: persistence, basic validation and weather/sunlight checks (Open‑Meteo)
  - `JsonService`: service that handles API requests and parsing of the responses data into chosen data models.
  - `EmailService`: service that handles email sending.

## Notification Decision Flow
For each user on schedule:
1. **Weather/time gate via Open‑Meteo:**
   - Is it after sunset at the user’s location?
   - Is cloud cover under 20%?
2. **Cooldown:** Has the user received an email in the last 12 hours? If yes, skip.
3. **Proximity:** Any aurora grid points within range of a users location?
   - Thresholds by user latitude (meters and min Kp):
     - lat >= 65°: minKp = 2, maxDistance = 300,000 m
     - lat >= 60°: minKp = 4, maxDistance = 500,000 m
     - else: minKp = 5, maxDistance = 600,000 m
4. **Elevation:** Computed aurora elevation must be >= 15 degrees. If less the aurora might be hidden behind obstacles in the horizon.
5. **Probability and Kp:**
   - Aurora point’s probability must be >= `minProbForLat(userLat, kp)`
   - Kp >= thresholds.minKp
   - `minProbForLat` rules:
     - >=65° -> 20, >=60° -> 30, else -> 40
     - If kp >= 6, reduce base by 10
     - Minimum floor = 15
6. If any nearby point passes the checks, send an email and update user’s `lastNotificationTime`

## Scheduling
- `checkAuroraForUsers` runs every 45 minutes for now and might change later.

## REST API
Base path: `/api/users`

- `GET /api/users/all`
  - Returns all users
- `GET /api/users/{id}`
  - Returns a single user by id (404 if not found)
- `POST /api/users/save`
  - Body: `UserDto`
  - Validates required fields: name, email, phoneNumber, lon, lat
  - 409 if user with same email or phoneNumber exists
  - 200 with the saved user otherwise
- `POST /api/users/delete/{id}`
  - Deletes the user (200 on success, 404 if not found)

### Example curl
```bash
# Get all users
curl -s http://localhost:8080/api/users/all

# Create user
curl -X POST http://localhost:8080/api/users/save   -H 'Content-Type: application/json'   -d '{"name":"Ada","email":"ada@example.com","phoneNumber":"+15551234567","lon":17.474,"lat":58.947}'

# Delete user
curl -X POST http://localhost:8080/api/users/delete/1
```

## Configuration

### Environment variables (.env)
- `API_URL_NOAA`: URL for NOAA aurora points JSON, address noted below
- `API_URL_KP`: URL for Kp index JSON, address noted below
- `MAIL_ADDRESS`: "From" address used by EmailService for sending emails to users.
- `DB_URL`: JDBC url to your postgres database. Example jdbc:postgresql://localhost:5432/auroratracker
- `DB_USER`: Database username.
- `DB_PASSWORD`: Database password

### Spring properties
Example `application.properties`:
```properties
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
```

Open‑Meteo is called directly with lat/lon for each user (no API key needed). 

## Build and Run

### Prerequisites
- JDK 21 (recommended for Spring Boot 3.5.x)
- Gradle 8+
- A running postgres database and configured Spring datasource

### Build
```bash
./gradlew clean build
```

### Run
```bash
./gradlew bootRun
# Or run jar
java -jar build/libs/aurora-tracker-<version>.jar
```

## Tests
Location: `src/test/kotlin/com/example/auroratracker/service/TrackingServiceTest.kt`

### Covered scenarios
- Email sent when user is near aurora point and conditions met
- No email when too far, before sunset/too cloudy, recently notified, low Kp, or low probability

Run tests:
```bash
./gradlew test
```

## Roadmap / TODO
- Implement frontend where users can register for aurora updates.
- Implement use of an emailservice, like SendGrid.
- Implement use of queues instead of courotines, like Kafka.
- Add authentication/authorization to API
- Add SMS/Push notification channels
- Build mobile app for IOS and Android that uses push notifications instead of email.

## Troubleshooting
- **NullPointerException from mappers/services**: Verify Spring injection and MapStruct setup
- **No emails sent**:
  - Check mail configuration and credentials
  - Verify MAIL_ADDRESS in `.env`
  - Inspect logs for exceptions in `EmailService`
- **No notifications despite aurora activity**:
  - Check Kp index source and format
  - Validate NOAA coordinates JSON ([lon, lat, probability])
  - Ensure user coordinates are valid
  - Review sunset/cloud cover API responses and timezones
- **Scheduling not running**: Ensure `@EnableScheduling` is present

## API urls
  - NOAA Aurora Points JSON (No API key needed)
    ```text
      https://services.swpc.noaa.gov/json/ovation_aurora_latest.json
    ```
  - NOAA Kp index JSON (No API key needed)
    ```text
      https://services.swpc.noaa.gov/json/planetary_k_index_1m.json (No API key needed)
    ```
  - OpenMeteo JSON (No API key needed). Replace `<LAT>` and `<LON>` with your or your users lat/lon.
    ```text
     https://api.open-meteo.com/v1/forecast?latitude=<LAT>&longitude=<LON>&current=cloud_cover&daily=sunset
    ```

---

© AuroraTracker
