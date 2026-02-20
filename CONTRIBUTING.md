# Contributing to NoxVision

Thank you for your interest in NoxVision.

## How to Contribute

### Bug Reports

- Use [GitHub Issues](../../issues) for bug reports.
- Describe the problem as clearly as possible.
- Include screenshots or logs if available.
- Provide device model and Android version.

### Feature Requests

- Open an issue with the `enhancement` label.
- Describe scope and expected behavior.
- Explain the use case.

### Code Contributions

1. Fork the repository.
2. Clone your fork.
   ```bash
   git clone https://github.com/YOUR-USERNAME/NoxVision.git
   ```
3. Create a feature branch.
   ```bash
   git checkout -b feature/my-new-feature
   ```
4. Commit your changes.
   ```bash
   git commit -m "feat: short description"
   ```
5. Push your branch.
   ```bash
   git push origin feature/my-new-feature
   ```
6. Open a pull request.

## Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation
- `style:` Formatting only
- `refactor:` Code refactoring
- `test:` Test changes
- `chore:` Tooling or dependency changes

## Code Style

- Kotlin Code Conventions
- Compose best practices
- Comments in English preferred

## Development

### Prerequisites

- JDK 17+
- Android SDK 35
- Gradle Wrapper (`./gradlew`)

### Build

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release AAB for Play Console
./gradlew :app:bundleRelease

# Unit tests
./gradlew :app:test
```

### Testing

- Preferred external testing channel: Google Play test tracks.
- Local testing: sideload the debug APK from `app/build/outputs/apk/debug/`.
- For public test groups, avoid debug APK distribution and use Play opt-in links instead.

## Branches

- `main`: Stable releases
- `beta`: Feature development

## Questions

Open an issue or a discussion.
