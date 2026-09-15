# Contributing to Anek Browser

Thank you for your interest in contributing!

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/anek-browser.git`
3. Open in Android Studio Hedgehog or newer
4. Build and run on device/emulator API 24+

## Development Guidelines

### Code Style
- Follow official Kotlin code style
- Use Jetpack Compose best practices
- Avoid unnecessary recompositions (use `remember`, `derivedStateOf`, `LaunchedEffect`)
- Use coroutines for IO operations, never block main thread
- Handle WebView lifecycle properly (destroy in `DisposableEffect`)

### Architecture
- MVVM with ViewModel + StateFlow
- UI -> ViewModel -> Repository -> DataStore/Room
- Keep browser engine logic separate from UI
- No God Activities

### Security
- Never bypass SSL errors globally
- Never expose dangerous JavaScript bridges
- Validate URLs before loading
- Request only necessary permissions
- Do not store passwords in plaintext

### Performance
- Use LazyColumn/LazyVerticalGrid for lists
- Avoid keeping unlimited WebViews in memory
- Properly destroy WebViews
- Test on low-end devices (2GB RAM)

## Pull Requests

1. Create a feature branch: `git checkout -b feature/my-feature`
2. Make changes, test thoroughly
3. Ensure project builds: `./gradlew assembleDebug`
4. Update README if needed
5. Submit PR with clear description

## Reporting Issues

Use GitHub Issues with:
- Device model & Android version
- Steps to reproduce
- Expected vs actual behavior
- Logs if applicable

## Feature Requests

Open an issue with `[Feature Request]` prefix.

## License

By contributing, you agree that your contributions will be licensed under MIT License.
