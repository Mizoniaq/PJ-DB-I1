# Detailed Recap — Step 4 (JDBC Integration)

This document summarizes all changes made to the **ArtConnect Pro** project to replace in-memory storage (InMemory) with real persistence via the MySQL `artconnect` database, in accordance with the **Step 4** requirements.

---

## 1. Connection Layer (Database Layer) & Security

*   **Externalized Configuration (`database.properties`)**:
    *   Created `src/main/resources/database.properties` containing the real credentials (`db.user`, `db.password`, `db.url`).
    *   Created `database.properties.example` as a template for other developers.
    *   Created a `.gitignore` file at the project root to ignore `database.properties` and ensure the password is never published publicly (e.g., on GitHub).
*   **`DatabaseConfig.java`**:
    *   Refactored to dynamically load credentials from `database.properties` instead of hardcoding them, securing the application.
*   **`ConnectionManager.java`**:
    *   Updated to use a **HikariCP connection pool** (max 10 connections, min 2 idle) instead of opening a raw `DriverManager` connection on every call.

---

## 2. Persistence Layer (JDBC DAOs)

Six new classes implementing the DAO interfaces were created in the package `com.project.artconnect.persistence`.
*All use `PreparedStatement` for SQL injection prevention and `try-with-resources` for safe connection handling.*

*   **`JdbcArtistDao.java`**:
    *   Full CRUD implementation (`findAll`, `findByCity`, `save`, `update`, `delete`).
    *   **Key feature**: Transaction management (`conn.setAutoCommit(false)`) in `save` and `update` to atomically insert the artist row and its linked rows in the `artist_discipline` junction table.
*   **`JdbcArtworkDao.java`**:
    *   Full CRUD implementation.
    *   **Key feature**: Uses a JOIN (`JOIN artist`) in `findAll()` to reconstruct the parent `Artist` object and attach it to the `Artwork` object in Java.
*   **`JdbcExhibitionDao.java`**:
    *   **Key feature**: Fetches gallery info (`JOIN gallery`) and loads the list of artworks linked to the exhibition via the `exhibition_artwork` junction table.
*   **`JdbcGalleryDao.java`**:
    *   Full CRUD implementation (`findById`, `findAll`, `save`, `update`, `delete`).
    *   **Key feature**: Automatically loads the `Exhibition` objects hosted by each gallery.
*   **`JdbcWorkshopDao.java`**:
    *   Full CRUD implementation (`findById`, `findAll`, `save`, `update`, `delete`).
    *   **Key feature**: JOINs the `artist` table to reconstruct the `Artist` object acting as instructor when loading workshops.
*   **`JdbcCommunityMemberDao.java`**:
    *   Full CRUD implementation (`findById`, `findAll`, `save`, `update`, `delete`).
    *   **Key feature**: Complex cascade loading. For each member, the DAO also fetches their bookings (`Booking`) and reviews (`Review`). Save/update are transactional to handle the `member_favorite_discipline` junction table atomically.

---

## 3. Service Layer (JDBC Services)

Five new classes implementing the service interfaces were created in the package `com.project.artconnect.service.impl` to connect the UI to the new JDBC DAOs.

*   **`JdbcArtistService.java`**: Delegates to `JdbcArtistDao`. Also handles `getAllDisciplines()` via a direct SQL query, and filters results in Java for the search feature.
    *   *Bug fix*: Added a null safety check (`a.getCity() != null`) when cross-filtering by discipline/city/name, for artists with no city stored in the database.
*   **`JdbcArtworkService.java`**: Delegates to `JdbcArtworkDao`.
*   **`JdbcGalleryService.java`**: Delegates to `JdbcGalleryDao`.
*   **`JdbcWorkshopService.java`**: Delegates to `JdbcWorkshopDao` and handles booking logic (`bookWorkshop`) by inserting directly into the `booking` junction table.
*   **`JdbcCommunityService.java`**: Delegates to `JdbcCommunityMemberDao`.

---

## 4. Dependency Injection (Wiring)

*   **`ServiceProvider.java`**:
    *   The singleton factory was updated (hard-switch).
    *   All returned instances are now the JDBC implementations (`JdbcArtistService`, etc.) constructed by passing the corresponding `JdbcXxxDao` instances.
    *   *Note*: The `InMemory` classes were kept in the project (unused) for testing or reference purposes, as per the project guidelines. The `initData()` method is no longer called since data now comes from MySQL.

---

## 5. Maven Configuration (`pom.xml`)

*   **JavaFX Version**: Updated `<javafx.version>` from `17.0.2` to `23.0.1`.
*   **Compiler Target**: Updated `<maven.compiler.source>` and `<target>` from `17` to `23` to match the local environment.
*   **Maven Compiler Plugin**: Updated `<release>` from `17` to `23`.
*   **HikariCP**: Added `com.zaxxer:HikariCP:5.1.0` dependency for connection pooling.

---

## Final State

The application compiles successfully (`mvn clean compile`) and runs (`mvn javafx:run`). The graphical interface displays live data from the local MySQL server and all persistence operations are functional, completing Step 4.
