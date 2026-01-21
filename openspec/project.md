# Project Context

## Purpose
AutoX.js v7 is an Android platform JavaScript runtime and development environment with accessibility service support. It aims to be similar to JsBox and Workflow, enabling automation, UI testing, and app development through JavaScript.

## Tech Stack
- **Languages:** Kotlin, Java, TypeScript, JavaScript (ES6+)
- **Build:** Gradle (Kotlin DSL), Android Gradle Plugin 8.5.0
- **JavaScript Engines:** Rhino v1.8.0, Javet (Node.js integration)
- **UI Frameworks:** Vue3, Jetpack Compose (Material Design 3)
- **Android:** Accessibility Service, Shizuku API support
- **Development:** Node.js 20+ required for building JS modules
- **Testing:** Android instrumentation tests

## Project Conventions

### Code Style
- **Kotlin:** Follow official Kotlin coding conventions
- **TypeScript:** Strict type checking enabled
- **Java:** Standard Android Java conventions
- **JavaScript/TypeScript modules:** Located in `autojs/src/main/js/`

### Architecture Patterns
- **Multi-module Gradle project:** Core modules include `app`, `autojs`, `common`, `automator`, `paddleocr`
- **JavaScript API versions:** v6 API (legacy) and v7 API (new non-blocking modules)
- **Accessibility-based automation:** UI element interaction via Android Accessibility Service
- **Plugin system:** Support for npm packages and custom modules

### Testing Strategy
- Android instrumentation tests in `autojs/src/androidTest/`
- Manual testing on connected Android devices via adb
- Test devices must allow test APK installation

### Git Workflow
- **Main branch:** `setup-v7`
- **Commit style:** Conventional commits preferred
- **Pre-build steps:** Run `./gradlew autojs:buildJsModule` once, then again when module code changes

## Domain Context

### Key Concepts
- **Accessibility Service:** Core mechanism for UI automation without root
- **Selector API:** UI element discovery and interaction (similar to UiAutomator)
- **Shizuku:** Advanced API requiring adb/setup for privileged operations
- **Root Mode:** Enhanced capabilities for screen capture, precise input, shell commands
- **Script Packing:** Export JavaScript projects as standalone APKs

### Module Structure
- `app/` - Main Android application
- `autojs/` - JavaScript runtime core and APIs
- `common/` - Shared utilities
- `automator/` - UI automation engine
- `paddleocr/` - OCR integration
- `inrt/` - Project runtime template
- `apkbuilder/` - APK packaging utilities

## Important Constraints
- **Java Version:** JDK 17 required
- **Node.js:** Version 20+ required for building JavaScript modules
- **License:** GPL-V2 (MPL-2.0 + non-commercial use from original Auto.js)
- **Android API:** Target modern Android versions with Material Design 3
- **Build Order:** Must build JS modules before compiling app

## External Dependencies
- **Rhino:** Mozilla JavaScript engine v1.8.0
- **Javet:** Node.js/V8 engine integration for Java
- **Shizuku:** Privileged API framework
- **PaddleOCR:** On-device OCR capabilities
- **Material Design 3:** Android UI design system
- **npm packages:** lodash, bluebird-co for JavaScript module support
