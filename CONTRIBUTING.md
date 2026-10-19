# Contributing

Thanks for your interest in contributing to **ATC Home**.

## Development setup

1. Install [Android Studio](https://developer.android.com/studio) (Hedgehog or newer) with JDK 17.
2. Clone the repo and open the project root in Android Studio.
3. Wait for Gradle sync, then run on a device/emulator (API 24+).

Command-line build:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew assembleDebug
```

## Pull requests

1. Fork the repository and create a branch from `master`.
2. Keep changes focused — one concern per PR when practical.
3. Match existing Kotlin / Compose style in the surrounding code.
4. Ensure `./gradlew assembleDebug` succeeds before opening a PR.
5. Fill out the PR template so reviewers know what changed and how to test it.

## Issues

- Use the **Bug report** template for defects.
- Use the **Feature request** template for ideas.
- Search existing issues first to avoid duplicates.

## Code of conduct

Be respectful and constructive in issues and pull requests. Harassment or abuse will not be tolerated.
