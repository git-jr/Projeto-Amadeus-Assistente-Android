# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Amadeus is an Android virtual assistant app with voice interaction (STT/TTS), chat interface, and Firebase backend. The codebase is mid-migration from Java to Kotlin — new files should be written in Kotlin.

## Build Commands

```bash
./gradlew assembleDebug       # Build debug APK
./gradlew assembleRelease     # Build release APK
./gradlew build               # Full build (all variants)
./gradlew clean               # Clean build outputs
./gradlew test                # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests (device required)
./gradlew lint                # Run lint checks
```

- **compileSdk / targetSdk:** 35 | **minSdk:** 21 | **JVM:** 17
- **App ID:** `com.paradoxo.amadeus` | **Version:** 3.0.0-beta-01

## Architecture

### Event-Driven Pattern
The app uses **EventBus** for loose coupling between components. Activities post events; services/processors subscribe and react. Check `@Subscribe` annotations to trace data flow.

### Core Packages

- **`activity/`** — UI screens. `MainActivity` (being migrated to Kotlin) is the main chat screen; it manages the dual-mode input (typing ↔ voice listening).
- **`cognicao/`** — AI/cognitive pipeline:
  - `Processadora` — receives user input and routes it
  - `Senteciadora` — processes/parses sentences
  - `Acionadora` — executes resolved actions
  - `faisca/` — initialization module
- **`util/voz/`** — Voice layer: `VozParaTexto` (STT), `TextoParaVoz` (TTS), `EscutadaoraService` (background listening service)
- **`dao/`** — Database access objects (7 DAOs)
- **`service/`** — Background services including `GravaHistoricoService` (Firebase history upload)
- **`firebase/`** — Firebase Cloud Messaging integration
- **`adapter/`** — RecyclerView adapters for the chat message list

### Voice Input Flow
`EscutadaoraService` → `VozParaTexto` → EventBus → `Processadora` → `Acionadora` → EventBus → `MainActivity` (renders response + `TextoParaVoz` speaks it)

### Java → Kotlin Migration
`MainActivity.java` has been renamed to `MainActivityJava.java` (kept for reference); `MainActivity.kt` is the active Kotlin replacement. When migrating other classes, follow the same pattern.

## Key Dependencies

- **EventBus 3.3.1** — inter-component messaging
- **Firebase BOM 33.5.1** — Database, Messaging, Storage, Analytics
- **Lottie 6.6.0** — UI animations
- **Gson 2.11.0** — JSON serialization
- **JSoup 1.18.1** — HTML parsing for web search
- **Joda-Time 2.13.0** — date/time utilities
