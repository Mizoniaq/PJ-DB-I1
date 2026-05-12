# Step 1 - Understanding ArtConnect & Defining the Functional Scope

## 1. Main Application Features

The application is organized in **7 tabs**:

| Tab | Features (from user perspective) |
|-----|-----------------------------------|
| **Artists** | List all artists · Search by name · Filter by discipline · Filter by city · View artist details (name, city, email, birth year, disciplines) |
| **Artworks** | List all artworks · Filter by artist / type / status · View artwork details (title, type, medium, price, status) |
| **Exhibitions** | List all exhibitions · Filter by gallery / date range · View artworks in an exhibition · View curator and theme |
| **Galleries** | List all galleries · View rating and contact info · Browse exhibitions hosted by a gallery |
| **Workshops** | List all workshops · Filter by level / instructor · View date, location, price, max participants |
| **Community** | List community members · View member bookings · View member reviews · Filter by city / membership type |
| **Discover** | Featured exhibitions (cards) · Upcoming workshops (cards) |

**Planned / Evolutionary Features** (not yet in the interface):
- User authentication and login
- Workshop booking by a community member (button to register)
- Writing and submitting a review on an artwork
- Artist registration / profile creation
- Event notifications / alerts

---

## 2. User Roles / Profiles

### Role 1 — Organizer (Artist / Admin)
- Creates and manages their own artist profile
- Adds, modifies, and deletes their artworks
- Proposes and manages workshops (as instructor)
- Participates in exhibitions organized by galleries
- Has a contact email, phone, website, city, list of disciplines

### Role 2 — Community Member (Visitor)
- Browses artists, artworks, exhibitions, and workshops
- Books spots in workshops
- Writes reviews on artworks (rating 1–5 + comment)
- Has a list of favorite disciplines
- Has a membership type (free or premium)

---

## 3. Use Case Diagram (PlantUML)

```plantuml
@startuml ArtConnect_UseCases

left to right direction

actor "Organizer\n(Artist/Admin)" as Org
actor "Community Member\n(Visitor)" as Vis
actor "System" as Sys

rectangle ArtConnect {

  ' ---- Organizer use cases ----
  usecase "Manage own Artist Profile\n(create, edit, delete)" as UC_ManageArtist
  usecase "Manage Artworks\n(add, edit, delete)" as UC_ManageArtworks
  usecase "Create / Manage Workshop\n(as instructor)" as UC_ManageWorkshop
  usecase "View Gallery & Exhibition Info" as UC_ViewGallery

  ' ---- Visitor use cases ----
  usecase "Browse Artists" as UC_BrowseArtists
  usecase "Search Artists\n(by name / discipline / city)" as UC_SearchArtists
  usecase "Browse Artworks" as UC_BrowseArtworks
  usecase "Browse Exhibitions" as UC_BrowseExhibitions
  usecase "Browse Workshops" as UC_BrowseWorkshops
  usecase "Book a Workshop" as UC_BookWorkshop
  usecase "Write a Review\non an Artwork" as UC_WriteReview
  usecase "Discover\n(featured events)" as UC_Discover

  ' ---- System use cases ----
  usecase "Generate Discover Feed\n(featured exhibitions & workshops)" as UC_Feed
  usecase "Check Workshop Capacity\nbefore Booking" as UC_Capacity

  ' ---- Relationships ----
  Org --> UC_ManageArtist
  Org --> UC_ManageArtworks
  Org --> UC_ManageWorkshop
  Org --> UC_ViewGallery

  Vis --> UC_BrowseArtists
  Vis --> UC_SearchArtists
  Vis --> UC_BrowseArtworks
  Vis --> UC_BrowseExhibitions
  Vis --> UC_BrowseWorkshops
  Vis --> UC_BookWorkshop
  Vis --> UC_WriteReview
  Vis --> UC_Discover

  UC_SearchArtists .> UC_BrowseArtists : <<include>>
  UC_BookWorkshop .> UC_Capacity : <<include>>
  UC_Discover .> UC_Feed : <<include>>
}

note right of UC_BookWorkshop
  **Planned / Evolutionary**
  (not yet in current UI)
end note

note right of UC_WriteReview
  **Planned / Evolutionary**
  (not yet in current UI)
end note

note right of UC_ManageArtist
  **Planned / Evolutionary**
  (currently no auth)
end note

@enduml
```

**Legend:**
- **Present in current UI**: Browse Artists, Search Artists, Browse Artworks, Browse Exhibitions, Galleries, Browse Workshops, Browse Community, Discover feed
- **Planned (evolutionary)**: Book a Workshop, Write a Review, Manage own Artist Profile, User authentication

---

## 4. Class Diagrams (PlantUML)

### 4a. Model Layer — Domain Classes

```plantuml
@startuml ArtConnect_ModelClasses

skinparam classAttributeIconSize 0

' ===== Enum (outside class) =====
enum Status {
  FOR_SALE
  SOLD
  EXHIBITED
}

' ===== Lookup / Value Objects =====
class Discipline {
  - name : String
  --
  + getName() : String
  + setName(name : String)
}

class ArtworkTag {
  - name : String
  --
  + getName() : String
  + setName(name : String)
}

' ===== Core Entities =====
class Artist {
  - name : String
  - bio : String
  - birthYear : Integer
  - contactEmail : String
  - phone : String
  - city : String
  - website : String
  - socialMedia : String
  - isActive : boolean
  --
  + getName() : String
  + getDisciplines() : List~Discipline~
  + getArtworks() : List~Artwork~
  + addArtwork(a : Artwork)
  + isActive() : boolean
}

class Artwork {
  - title : String
  - creationYear : Integer
  - type : String
  - medium : String
  - dimensions : String
  - description : String
  - price : double
  - status : Status
  --
  + getTitle() : String
  + getArtist() : Artist
  + setArtist(a : Artist)
  + getTags() : List~ArtworkTag~
  + getStatus() : Status
}

class Gallery {
  - name : String
  - address : String
  - ownerName : String
  - openingHours : String
  - contactPhone : String
  - rating : double
  - website : String
  --
  + getName() : String
  + getExhibitions() : List~Exhibition~
  + addExhibition(e : Exhibition)
}

class Exhibition {
  - title : String
  - startDate : LocalDate
  - endDate : LocalDate
  - description : String
  - curatorName : String
  - theme : String
  --
  + getTitle() : String
  + getGallery() : Gallery
  + getArtworks() : List~Artwork~
}

class Workshop {
  - title : String
  - date : LocalDateTime
  - durationMinutes : int
  - maxParticipants : int
  - price : double
  - location : String
  - description : String
  - level : String
  --
  + getTitle() : String
  + getInstructor() : Artist
  + getDate() : LocalDateTime
}

class CommunityMember {
  - name : String
  - email : String
  - birthYear : Integer
  - phone : String
  - city : String
  - membershipType : String
  --
  + getName() : String
  + getEmail() : String
  + getBookings() : List~Booking~
  + getReviews() : List~Review~
  + addBooking(b : Booking)
  + getFavoriteDisciplines() : List~Discipline~
}

class Booking {
  - bookingDate : LocalDateTime
  - paymentStatus : String
  --
  + getWorkshop() : Workshop
  + getMember() : CommunityMember
  + getPaymentStatus() : String
}

class Review {
  - rating : int
  - comment : String
  - reviewDate : LocalDate
  --
  + getReviewer() : CommunityMember
  + getArtwork() : Artwork
  + getRating() : int
}

' ===== Relationships =====
Artwork --> Status : uses

Artist "1" --> "0..*" Artwork      : creates >
Artist "1" --> "0..*" Workshop     : instructs >
Artist "0..*" --> "0..*" Discipline : practices >

Artwork "0..*" --> "0..*" ArtworkTag : tagged with >
Artwork "1" <-- "0..*" Review       : reviewed in

Gallery "1" --> "0..*" Exhibition       : hosts >
Exhibition "0..*" --> "0..*" Artwork    : features >

Workshop "1" <-- "0..*" Booking         : booked for
CommunityMember "1" --> "0..*" Booking  : makes >
CommunityMember "1" --> "0..*" Review   : writes >
CommunityMember "0..*" --> "0..*" Discipline : favorite >

@enduml
```

### 4b. Architecture — Layer Diagram

```plantuml
@startuml ArtConnect_Architecture

package "Presentation Layer" <<Frame>> {
  class MainController
  class ArtistController
  class ArtworkController
  class ExhibitionController
  class GalleryController
  class WorkshopController
  class CommunityController
  class DiscoverController
}

package "Service Layer" <<Frame>> {
  interface ArtistService
  interface ArtworkService
  interface GalleryService
  interface WorkshopService
  interface CommunityService

  package "InMemory Implementations" {
    class InMemoryArtistService
    class InMemoryArtworkService
    class InMemoryGalleryService
    class InMemoryWorkshopService
    class InMemoryCommunityService
  }
}

package "DAO Layer" <<Frame>> {
  interface ArtistDao
  interface ArtworkDao
  interface ExhibitionDao
  interface GalleryDao
  interface WorkshopDao
  interface CommunityMemberDao

  package "JDBC Implementations (TODO)" {
    class JdbcArtistDao
    class JdbcArtworkDao
  }
}

package "Model Layer" <<Frame>> {
  class Artist
  class Artwork
  class Exhibition
  class Gallery
  class Workshop
  class CommunityMember
  class Booking
  class Review
  class Discipline
  class ArtworkTag
}

package "Utilities" <<Frame>> {
  class ServiceProvider
  class ConnectionManager
  class DatabaseConfig
}

"Presentation Layer" --> "Service Layer" : uses
"Service Layer" --> "DAO Layer" : uses
"DAO Layer" --> "Model Layer" : maps
InMemoryArtistService ..|> ArtistService
JdbcArtistDao ..|> ArtistDao
"JDBC Implementations (TODO)" --> ConnectionManager : uses
ConnectionManager --> DatabaseConfig : configures

ArtistService ..> ArtistDao : (future)
ServiceProvider ..> ArtistService : provides

@enduml
```

---

## 5. Summary of Identified Relationships

| Relationship | Type | Description |
|---|---|---|
| Artist → Artwork | 1:N | One artist creates many artworks |
| Artist → Discipline | N:M | An artist practices multiple disciplines |
| Artist → Workshop | 1:N | An artist instructs multiple workshops |
| Artwork → ArtworkTag | N:M | An artwork has multiple tags |
| Artwork → Review | 1:N | An artwork receives multiple reviews |
| Exhibition → Gallery | N:1 | An exhibition is hosted in one gallery |
| Exhibition → Artwork | N:M | An exhibition features multiple artworks |
| Workshop → Booking | 1:N | A workshop has multiple bookings |
| CommunityMember → Booking | 1:N | A member makes multiple bookings |
| CommunityMember → Review | 1:N | A member writes multiple reviews |
| CommunityMember → Discipline | N:M | A member has multiple favorite disciplines |
