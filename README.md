# StillFresh? - Android

**StillFresh?** is an Android application designed to help you manage your groceries effectively and reduce food waste. By tracking expiration dates and scanning receipts, the app ensures you stay informed about the status of your pantry.

---

## 📋 Features

The application includes the following core functionalities to enhance your grocery management experience:
* **Receipt Scanning:** Users can scan general supermarket receipts to quickly add items to their inventory.
* **Push Notifications:** Receive timely alerts regarding the expiration dates of your products.
* **Product Data:** Automatically retrieve detailed information about added products from an open data source.
* **Account Creation:** Users can create personal accounts to manage their grocery lists.
* **User Interface:** A dedicated UI designed for ease of use in tracking food items.

---

## 🛠️ Technical Stack

The app leverages several modern APIs and security practices to ensure a robust experience:

### External APIs

* **Openfoodfacts API:** Used as the open data source to fetch product-specific information.


* **Supabase API:** Utilized for backend services and data management.


* **Open Router API:** Integrated for expanded application functionality.

### Security

* **Data Encryption:** To ensure user privacy, all usernames and passwords are required to be encrypted.

---

## 🚀 How It Works

Managing your groceries with **StillFresh?** follows a simple three-step process:

1. **Do your groceries:** Shop at your preferred supermarket as usual.
2. **Add groceries to the app:** Use the scanning feature or manual entry to input your items.
3. **Get notifications:** The app tracks the items and notifies you when they are about to expire.

---

## 🛠 Coding & Conventions

### Keys and Secrets
Keys and secrets are saved in the `local.properties` file. An example of how keys and values are being stored is written below:

```
API_KEY="The API Key"
```

If the keys should be provided in the application, the keys from the `local.properties` file should be added to the `build.gradle.kts` file too.
To add these keys, add the following code at the end of `android -> defaultConfig`:

```kotlin
buildConfigField(
    type = "String",
    name = "API_KEY",
    value = properties.getProperty("API_KEY") ?: ""
)
//Replace API_KEY with the key you've added to the local.properties file
```

### Main Conventions
The **StillFresh?** Android app is made with Kotlin. To provide good code quality, the code is structured using the following rules:
- The app is using activities, with each activity providing a kotlin and a xml file. The kotlin files are stored at `com.stillfresh.activities`, and the xml files are stored at `/res/layout/`.
- Classes are stored into sub-packages based on their functional responsibility (e.g., `activities`, `handlers`, `config`)
- Classes are written in **Pascal Case**, like `MainActivity`
- There are no duplicate lines of code. If code can be reused, it is stored in a general class.
- Functions and normal fields are written in **Camel Case**, like `onCreate` or `newField`
- Constant values (e.g. API-keys, excluding constant values in xml files) are written in **Capitalized Snake Case**, like `API_KEY`. 
- Colors, strings, themes images and icons are saved in the `/res/` folder, where colors, strings and themes are located in `/res/values/`, images and icons in `/res/mipmap/`

---

**Developers:** Bryan Potze, Gideon Dijkhuis, Jaâfar Jawadi, and Luc van Koppen.
