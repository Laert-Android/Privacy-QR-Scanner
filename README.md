# Privacy QR Scanner

A fast, lightweight, and fully offline QR code and barcode scanner for Android. No internet permission. No tracking. No ads.

---

## Features

- Scan QR codes and barcodes instantly using your camera
- Fully offline — no internet connection required
- No tracking, no analytics, no ads
- Copy, share, or open scanned results directly
- Open URLs in browser or compatible apps
- Vibration feedback on successful scan
- Clean Material Design UI with dark background
- Supports all major barcode formats (QR, EAN, UPC, Code 128, and more)

---

## Privacy

This app requests only one permission:

- `CAMERA` — required to scan QR codes

No network permission is requested. No data leaves your device. No third-party SDKs with telemetry are active.

---

## Download

- [GitHub Releases](https://github.com/Laert-Android/Privacy-QR-Scanner/releases)
- F-Droid *(coming soon)*

---

## Build

This app is built with Android Studio using Java and Gradle.

**Requirements:**
- Android Studio Hedgehog or newer
- Java 11+
- Android SDK 21+

**Clone and build:**

```bash
git clone https://github.com/Laert-Android/Privacy-QR-Scanner.git
cd Privacy-QR-Scanner
./gradlew assembleRelease
```

---

## Tech Stack

| Component | Library |
|---|---|
| Camera | CameraX 1.3.4 |
| Barcode scanning | ML Kit Barcode Scanning 17.3.0 (bundled) |
| UI | Material Components |
| Language | Java |
| Min SDK | Android 5.0 (API 21) |
| Target SDK | Android 16 (API 35) |

---

## License

This project is licensed under the GNU General Public License v3.0 — see the [LICENSE](LICENSE) file for details.

---

## Author

Made by [Laert](https://github.com/Laert-Android)
