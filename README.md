# SimplyDish 🍽️

**SimplyDish** is a modern Android application designed for food enthusiasts to discover, create, and manage recipes. This project was developed as a final assignment for a Computer Science course, demonstrating proficiency in Android development, API integration, and cloud-based backend services.

## 📱 Features

* **User Authentication:** Secure Login and Registration system using Firebase Authentication.
* **Recipe Feed:** Browse a dynamic feed of recipes shared by the community.
* **Global Search:** Integrated with **TheMealDB API** to search thousands of recipes online.
* **Recipe Management:**
    * **Create:** Users can add their own custom recipes with ingredients and instructions.
    * **Favorites:** Save recipes from the web directly to your personal collection (Firestore).
* **Detailed View:** View comprehensive recipe details including ingredients, measurements, and preparation steps.
* **Smart Navigation:** Smooth user experience using Android Navigation Component.

## 🛠️ Tech Stack & Architecture

The application is built using **Java** and follows modern Android development practices:

* **Language:** Java
* **Architecture:** Single Activity Architecture with Navigation Component.
* **UI:** XML Layouts, ConstraintLayout, ViewBinding, Material Design.
* **Networking:** Retrofit 2 (for API calls), Gson (JSON parsing).
* **Backend (BaaS):**
    * **Firebase Authentication:** For user management.
    * **Cloud Firestore:** NoSQL database for storing user recipes and favorites.
* **Image Loading:** Glide.
* **Asynchronous Processing:** Asynchronous callbacks for network and database operations.

## 📸 Screenshots
<img width="270" height="480" alt="image" src="https://github.com/user-attachments/assets/bb4ff11a-560b-406a-95d0-966fc9cebe74" />

## 🚀 Getting Started

To run this project locally, follow these steps:

### Prerequisites
* Android Studio Ladybug (or newer)
* JDK 11 or higher

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/SimplyDish.git](https://github.com/your-username/SimplyDish.git)
    ```

2.  **Open in Android Studio:**
    Open Android Studio -> File -> Open -> Select the cloned folder.

3.  **Firebase Configuration (Critical Step):**
    * This project relies on Firebase. For security reasons, the `google-services.json` file is **not** included in this repository.
    * Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    * Enable **Authentication** (Email/Password) and **Cloud Firestore**.
    * Download your own `google-services.json` file.
    * Place the file in the `app/` directory of the project:
        ```text
        SimplyDish/
        ├── app/
        │   ├── google-services.json  <-- Place here
        │   ├── src/
        │   └── ...
        └── ...
        ```

4.  **Build and Run:**
    Sync the project with Gradle files and run the app on an Emulator or Physical Device.

## 🔗 API Reference

This project uses the free tier of [TheMealDB](https://www.themealdb.com/api.php) for searching public recipes.
* Base URL: `https://www.themealdb.com/api/json/v1/1/`

## 📝 License

This project is open-source and available under the MIT License.

---
*Developed by Meir G - Computer Science Student, 2026*
