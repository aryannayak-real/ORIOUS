<p align="center">
  <img width="100" src="metadata/en-US/images/icon.png" alt="ORIOUS icon">
</p>

<h1 align="center">ORIOUS Authenticator</h1>

<p align="center">
  A free, secure and open-source two-factor authentication app for Android.
</p>

<p align="center">
  <a href="https://github.com/aryannayak-real/ORIOUS">GitHub Repository</a>
</p>

ORIOUS Authenticator helps protect your online accounts with secure two-factor authentication. It supports HOTP and TOTP, making it compatible with thousands of services.

ORIOUS is based on [Aegis Authenticator](https://github.com/beemdevelopment/Aegis), originally developed by Alexander Bakker and Michael Schättgen.

For frequently asked questions, see the [FAQ](FAQ.md).

The security design and vault format are described in the [vault documentation](docs/vault.md).

## Features

- Free and open source
- Secure encrypted vault using AES-256-GCM
- Unlock using a password or biometrics
- Screen capture prevention
- Tap to reveal codes
- Compatible with Google Authenticator
- Supports HOTP and TOTP standards
- Add entries by scanning QR codes or images
- Manually enter account details
- Import from other authenticator apps:
  - 2FAS Authenticator
  - Authenticator Plus
  - Authy
  - andOTP
  - FreeOTP and FreeOTP+
  - Google Authenticator
  - Microsoft Authenticator
  - Steam
  - TOTP Authenticator
  - WinAuth
- Organize and group entries
- Alphabetical and custom sorting
- Custom or automatically generated icons
- Search by name or issuer
- Light, dark and AMOLED themes
- Export entries in plaintext or encrypted formats
- Automatic vault backups

## Screenshots

[<img width="200" alt="Screenshot 1" src="metadata/en-US/images/phoneScreenshots/screenshot1.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot1.png?raw=true)
[<img width="200" alt="Screenshot 2" src="metadata/en-US/images/phoneScreenshots/screenshot2.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot2.png?raw=true)
[<img width="200" alt="Screenshot 3" src="metadata/en-US/images/phoneScreenshots/screenshot3.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot3.png?raw=true)
[<img width="200" alt="Screenshot 4" src="metadata/en-US/images/phoneScreenshots/screenshot4.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot4.png?raw=true)
[<img width="200" alt="Screenshot 5" src="metadata/en-US/images/phoneScreenshots/screenshot5.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot5.png?raw=true)
[<img width="200" alt="Screenshot 6" src="metadata/en-US/images/phoneScreenshots/screenshot6.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot6.png?raw=true)
[<img width="200" alt="Screenshot 7" src="metadata/en-US/images/phoneScreenshots/screenshot7.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot7.png?raw=true)
[<img width="200" alt="Screenshot 8" src="metadata/en-US/images/phoneScreenshots/screenshot8.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot8.png?raw=true)

## Downloads

ORIOUS releases and installation instructions will be published through this repository when available.

[View the ORIOUS GitHub repository](https://github.com/aryannayak-real/ORIOUS)

## Icon packs

ORIOUS supports compatible third-party icon packs. These community projects can help you customize the icons used for your entries.

- [aegis-icons](https://github.com/aegis-icons/aegis-icons) — Unofficial monochrome-styled 2FA icons.
- [delta-aegis-icons](https://github.com/Delta-Icons/aegis-icons) — Delta version of the monochrome icon pack.
- [aegis-simple-icons](https://github.com/alexbakker/aegis-simple-icons) — Icon pack generated from Simple Icons.
- [aegis-simple-icons-outlined](https://github.com/michaelschattgen/aegis-simple-icons-outlined) — Outlined version of the Simple Icons pack.

To learn how to create a compatible icon pack, see the [icon pack documentation](docs/iconpacks.md).

## Contributing

Contributions, translations, bug reports and pull requests are welcome.

Please read the [contributing guidelines](CONTRIBUTING.md) before contributing.

## Credits

ORIOUS is based on [Aegis Authenticator](https://github.com/beemdevelopment/Aegis).

Original developers:
- [Alexander Bakker](https://alexbakker.me)
- [Michael Schättgen](https://github.com/michaelschattgen)

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

Some vendored libraries have their own licenses:

- [TextDrawable](app/src/main/java/com/amulyakhare/textdrawable)
- [TrustedIntents](app/src/main/java/info/guardianproject/trustedintents)
