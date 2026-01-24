# Contributing to NoxVision

Thank you for your interest in NoxVision! 🎉

## How to Contribute

### Bug Reports
- Use [GitHub Issues](../../issues) for bug reports
- Describe the problem as detailed as possible
- Include screenshots or logs if available
- Provide your device model and Android version

### Feature Requests
- Open an issue with the `enhancement` label
- Describe the desired feature scope
- Explain the use case

### Code Contributions

1. **Fork** the repository
2. **Clone** your fork:
   ```bash
   git clone https://github.com/YOUR-USERNAME/NoxVision.git
   ```
3. Create a **feature branch**:
   ```bash
   git checkout -b feature/my-new-feature
   ```
4. **Commit** your changes:
   ```bash
   git commit -m "feat: description of change"
   ```
5. **Push** to your fork:
   ```bash
   git push origin feature/my-new-feature
   ```
6. Open a **Pull Request**

### Commit Message Format
We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation
- `style:` Code style (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Add/modify tests
- `chore:` Build process, dependencies

### Code Style
- Kotlin Code Conventions
- Compose Best Practices
- Comments in English preferred

## Development

### Prerequisites
- JDK 17+
- Android SDK 35
- Gradle 8.x

### Build
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Tests
./gradlew test
```

### Testing without Emulator
You can transfer the APK directly to your device:
1. Build with `./gradlew assembleDebug`
2. APK is located at `app/build/outputs/apk/debug/NoxVision-v1.0-debug.apk`
3. Transfer via LocalSend, ADB, or USB

## Branches

- `main` - Stable releases
- `beta` - Development version, new features

## Questions?

Open an issue or start a discussion!

---

Thank you for your support! 🙏
