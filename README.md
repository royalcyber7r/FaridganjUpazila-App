# Faridganj Upazila Android App

Starter Android Studio project for `https://faridganjapp.rf.gd/`.

## Build configuration\n- Android Gradle Plugin 8.10.1, Gradle 8.11.1, JDK 17.\n- compileSdk/targetSdk 36 (current Google Play requirement for new apps as of August 31, 2026).\n\n## Current starter scope
- Kotlin Android app with a WebView wrapper.
- Loads the existing PHP website over HTTPS.
- Website content continues to come from the existing website/database.
- Android back navigation and external `tel:` / `mailto:` links.
- GitHub Actions builds a debug APK and an unsigned release AAB.

## Open in Android Studio
1. Download and extract this repository.
2. Open the extracted folder in Android Studio.
3. Use JDK 17, Android SDK 36, and allow Gradle sync.
4. Run the `app` configuration on a device.

## Create the GitHub repository
1. On GitHub, create a new **private** repository named `FaridganjUpazila-App`.
2. Upload the contents of this folder to the repository root (not the containing folder).
3. Commit to the `main` branch.
4. Open **Actions** and run `Build Faridganj Upazila Android App`, or push a commit.
5. Open the completed workflow run and download its artifact.

## Important release notes
- The generated debug APK is for testing and direct installation.
- The release AAB from this starter is **unsigned**. Before Play Store submission, create a release keystore and configure secure GitHub Actions signing secrets. Never commit a keystore or passwords to GitHub.
- AdMob is not enabled yet. Add it only after creating the AdMob app/ad units and configuring consent/privacy handling.
- This starter is a WebView shell, not the completed native/API app. It deliberately does not modify the PHP website or database. The website remains the single source of truth. After receiving the PHP source and database schema (with secrets removed), the next stage can add authenticated API endpoints and native app screens while reusing the same MySQL database.
- If the hosting provider blocks WebView traffic or hotlinking, the website hosting rules must be checked; do not bypass server security.
