<p align="center">
  <img width="100" src="metadata/en-US/images/icon.png" alt="ORIOUS icon">
</p>

<h1 align="center">ORIOUS</h1>

<p align="center">
  A free, secure, offline-first <strong>Two-Factor Authenticator</strong>, <strong>Password Manager</strong>, and <strong>Private Notebook</strong> for Android.
</p>

<p align="center">
  <a href="https://github.com/aryannayak-real/ORIOUS">GitHub Repository</a> ·
  <a href="https://github.com/aryannayak-real/ORIOUS/releases">Releases</a> ·
  <a href="FAQ.md">FAQ</a> ·
  <a href="docs/vault.md">Vault Security</a>
</p>

ORIOUS protects your accounts, passwords, and personal notes inside a single AES-256-GCM encrypted vault stored locally on your device—with zero cloud accounts, zero telemetry, and full encrypted backup/restore support.

ORIOUS is based on [Aegis Authenticator](https://github.com/beemdevelopment/Aegis), originally developed by Alexander Bakker and Michael Schättgen.

## What's Inside

ORIOUS organizes your security essentials into three independent sections inside one unified encrypted vault:

### 1. Authenticator (2FA Tokens)
- Supports **TOTP**, **HOTP**, **Steam**, **mOTP**, and **YAOTP** standards
- Compatible with Google Authenticator (`otpauth://` and batch `otpauth-migration://` QR imports)
- Add entries by scanning QR codes, decoding gallery images, importing from clipboard, or manual entry
- Tap to reveal codes, live expiration progress indicator, and haptic expiry alerts
- Organize with custom groups, custom/auto-generated icons, and alphabetical or custom drag-and-drop sorting
- Import from 2FAS Authenticator, andOTP, Authenticator Plus, Authy, Bitwarden, Duo, Ente Auth, FreeOTP, FreeOTP+, Google Authenticator, Microsoft Authenticator, Proton Authenticator, Steam, Stratum, TOTP Authenticator, and WinAuth

### 2. Password Manager
- Store service names, usernames, passwords, websites, and password notes inside the encrypted vault
- Built-in configurable password generator (length, uppercase/lowercase, numbers, symbols, exclude ambiguous characters)
- Quick copy password/username actions marked sensitive (`EXTRA_IS_SENSITIVE`) with optional auto-minimize
- Search and sort across saved credentials

### 3. Private Notebook
- Create, view, edit, search, favorite, and delete personal multiline notes
- Distraction-free note editor supporting long notes, preserved line breaks, live word counts, and last-modified timestamps
- Filter by **All notes** or **Favorites**, search across titles and bodies, and sort by last modified, title, or creation date
- Delete confirmation guard and zero plaintext note storage outside the vault

## Security & Backups

- **AES-256-GCM + scrypt Encryption**: Master key protected by password and/or Android Keystore biometric slots
- **Offline-First & Private**: No separate account, no cloud lock-in, no plaintext note or credential storage
- **Screen Capture Prevention**: `FLAG_SECURE` enabled across vault views and editors
- **Auto-Lock**: Lock on minimize, screen off, or back button, clearing in-memory entries and notes immediately
- **Full Encrypted Backup & Restore**: All 2FA tokens, passwords, and private notes are included in encrypted vault backups, restores, imports, and HTML exports

For cryptographic details and vault format specifications, see [docs/vault.md](docs/vault.md) and [FAQ.md](FAQ.md).

## Screenshots

[<img width="200" alt="Screenshot 1" src="metadata/en-US/images/phoneScreenshots/screenshot1.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot1.png?raw=true)
[<img width="200" alt="Screenshot 2" src="metadata/en-US/images/phoneScreenshots/screenshot2.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot2.png?raw=true)
[<img width="200" alt="Screenshot 3" src="metadata/en-US/images/phoneScreenshots/screenshot3.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot3.png?raw=true)
[<img width="200" alt="Screenshot 4" src="metadata/en-US/images/phoneScreenshots/screenshot4.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot4.png?raw=true)
[<img width="200" alt="Screenshot 5" src="metadata/en-US/images/phoneScreenshots/screenshot5.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot5.png?raw=true)
[<img width="200" alt="Screenshot 6" src="metadata/en-US/images/phoneScreenshots/screenshot6.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot6.png?raw=true)
[<img width="200" alt="Screenshot 7" src="metadata/en-US/images/phoneScreenshots/screenshot7.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot7.png?raw=true)
[<img width="200" alt="Screenshot 8" src="metadata/en-US/images/phoneScreenshots/screenshot8.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot8.png?raw=true)

## Downloads & Releases

Signed release APKs are built and published automatically on tagged releases (`v*`) via GitHub Actions.

- Latest releases: [GitHub Releases](https://github.com/aryannayak-real/ORIOUS/releases)
- Repository: [github.com/aryannayak-real/ORIOUS](https://github.com/aryannayak-real/ORIOUS)

## Building & Releasing

Requires JDK 17+ and Android SDK (API 36).

```bash
# Run unit & Robolectric tests
./gradlew testDebugUnitTest

# Run Android Lint checks
./gradlew lintDebug

# Build debug APK
./gradlew assembleDebug

# Build signed release APK (using environment secrets or GitHub Actions tag v*)
./gradlew assembleRelease
```

### Publishing a GitHub Release

1. Ensure `versionCode` and `versionName` in `app/build.gradle` and `app/src/main/assets/changelog.html` are updated.
2. Verify CI secrets (`ORIOUS_KEYSTORE_BASE64`, `ORIOUS_STORE_PASSWORD`, `ORIOUS_KEY_ALIAS`, `ORIOUS_KEY_PASSWORD`) are configured in GitHub repository secrets.
3. Push to `main` and push a version tag (for example `git tag v3.5.0 && git push origin v3.5.0`) to trigger `.github/workflows/build-app-workflow.yaml`.

## Icon Packs

ORIOUS supports compatible third-party icon packs:

- [aegis-icons](https://github.com/aegis-icons/aegis-icons) — Monochrome-styled 2FA icons
- [delta-aegis-icons](https://github.com/Delta-Icons/aegis-icons) — Delta monochrome icon pack
- [aegis-simple-icons](https://github.com/alexbakker/aegis-simple-icons) — Simple Icons pack
- [aegis-simple-icons-outlined](https://github.com/michaelschattgen/aegis-simple-icons-outlined) — Outlined Simple Icons pack

See [docs/iconpacks.md](docs/iconpacks.md) to create custom icon packs.

## Contributing

Contributions, translations, bug reports, and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

## Credits

ORIOUS is based on [Aegis Authenticator](https://github.com/beemdevelopment/Aegis).

Original upstream developers:
- [Alexander Bakker](https://alexbakker.me)
- [Michael Schättgen](https://github.com/michaelschattgen)

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

Vendored libraries with their own licenses:
- [TextDrawable](app/src/main/java/com/amulyakhare/textdrawable)
- [TrustedIntents](app/src/main/java/info/guardianproject/trustedintents)
