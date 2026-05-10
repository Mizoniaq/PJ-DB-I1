# Récapitulatif Détaillé - Step 4 (Intégration JDBC)

Ce document résume l'ensemble des modifications apportées au projet **ArtConnect Pro** afin de remplacer le stockage en mémoire (InMemory) par une persistance réelle via la base de données MySQL `artconnect`, conformément aux exigences du **Step 4**.

---

## 1. Couche de Connexion (Database Layer) & Sécurité

*   **Configuration Externalisée (`database.properties`)** :
    *   Création d'un fichier `src/main/resources/database.properties` contenant les identifiants réels (`db.user`, `db.password`, `db.url`).
    *   Création d'un fichier `database.properties.example` comme modèle pour les autres développeurs.
    *   Création d'un fichier `.gitignore` à la racine pour ignorer `database.properties` et s'assurer que le mot de passe ne soit jamais publié publiquement (ex: sur GitHub).
*   **`DatabaseConfig.java`** : 
    *   Refactorisation pour charger dynamiquement les identifiants depuis `database.properties` au lieu de les coder en dur (hardcoding), sécurisant ainsi l'application.
*   **`ConnectionManager.java`** : 
    *   Implémentation de la méthode `getConnection()` utilisant `DriverManager.getConnection()` avec les variables chargées par `DatabaseConfig`.

---

## 2. Couche de Persistance (DAO JDBC)

Six nouvelles classes implémentant les interfaces DAO ont été créées dans le package `com.project.artconnect.persistence`. 
*Toutes utilisent `PreparedStatement` pour la sécurité contre les injections SQL et `try-with-resources` pour la gestion propre des connexions.*

*   **`JdbcArtistDao.java`** :
    *   Implémentation complète du CRUD (`findAll`, `findByCity`, `save`, `update`, `delete`).
    *   **Particularité** : Gestion des transactions (`conn.setAutoCommit(false)`) pour le `save` et `update` afin de garantir que l'insertion de l'artiste et l'insertion de ses liens dans la table de jonction `artist_discipline` se fassent de manière atomique.
*   **`JdbcArtworkDao.java`** :
    *   Implémentation complète du CRUD.
    *   **Particularité** : Utilisation d'une jointure (`JOIN artist`) lors du `findAll()` pour reconstituer l'objet `Artist` parent et l'attacher à l'objet `Artwork` en Java.
*   **`JdbcExhibitionDao.java`** :
    *   **Particularité** : Récupère les informations de la galerie (`JOIN gallery`) et charge la liste des œuvres associées à l'exposition via la table de jonction `exhibition_artwork`.
*   **`JdbcGalleryDao.java`** :
    *   **Particularité** : Charge automatiquement les expositions (`Exhibition`) qui ont lieu dans chaque galerie.
*   **`JdbcWorkshopDao.java`** :
    *   **Particularité** : Jointure avec la table `artist` pour reconstituer l'objet `Artist` (qui joue le rôle d'instructeur) lors de la récupération des ateliers.
*   **`JdbcCommunityMemberDao.java`** :
    *   **Particularité** : Chargement complexe en cascade. Pour chaque membre, le DAO récupère également ses réservations (`Booking`) et ses avis (`Review`).

---

## 3. Couche Service (JDBC Services)

Cinq nouvelles classes implémentant les interfaces de services ont été créées dans le package `com.project.artconnect.service.impl` pour relier l'UI aux nouveaux DAOs JDBC.

*   **`JdbcArtistService.java`** : Délègue à `JdbcArtistDao`. Gère également la méthode `getAllDisciplines()` via une requête SQL directe, et filtre en Java les résultats pour la recherche.
    *   *Correction de Bug* : Ajout d'une sécurité contre les `NullPointerException` (vérification `a.getCity() != null`) lors du filtrage croisé par discipline/ville/nom, pour les artistes n'ayant pas de ville renseignée en base.
*   **`JdbcArtworkService.java`** : Délègue à `JdbcArtworkDao`.
*   **`JdbcGalleryService.java`** : Délègue à `JdbcGalleryDao`.
*   **`JdbcWorkshopService.java`** : Délègue à `JdbcWorkshopDao` et gère la logique de réservation (`bookWorkshop`) en insérant directement dans la table de jonction `booking`.
*   **`JdbcCommunityService.java`** : Délègue à `JdbcCommunityMemberDao`.

---

## 4. Injection des Dépendances (Wiring)

*   **`ServiceProvider.java`** :
    *   Le système de distribution des singletons a été modifié (Hard-Switch). 
    *   Toutes les instances retournées sont désormais les implémentations JDBC (`JdbcArtistService`, etc.) construites en leur passant les `JdbcXxxDao`.
    *   *Note* : Les classes `InMemory` ont été conservées dans le projet (non appelées) à des fins de tests ou de référence, conformément aux consignes du projet. La méthode `initData()` n'est plus appelée puisque les données viennent de MySQL.

---

## 5. Fichier de Configuration Maven (`pom.xml`)

Pour corriger un bug majeur de crash sur macOS (`NSInternalInconsistencyException` / `NSTrackingRectTag`) survenant lors du lancement de l'interface graphique :
*   **JavaFX Version** : Mise à jour de `<javafx.version>` de `17.0.2` vers `23.0.1`.
*   **Compiler Target** : Mise à jour de `<maven.compiler.source>` et `<target>` de `17` vers `23` pour correspondre à ton environnement local.
*   **Maven Compiler Plugin** : Mise à jour de `<release>` de `17` vers `23`.

---

## État Final
L'application compile avec succès (`mvn clean compile`) et s'exécute (`mvn javafx:run`). L'interface graphique affiche en temps réel les données issues du serveur MySQL local et toutes les opérations de persistance sont opérationnelles, clôturant ainsi le Step 4.
