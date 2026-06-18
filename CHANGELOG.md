# Changelog

All notable changes to this project will be documented in this file.

## [1.0.1] - 2025-10-30

### Changed
- Plugin icon updated. Now better in terms of concept and relevancy.
- A few updates to test cases for parametrized test support.
- Refactoring here and there to the code.

### Removed
- Unused import statements and commented lines removed as part of cleanup process.

## [1.0.2] - 2025-10-30

### Added
- Plugin published through JetBrains marketplace!

### Changed
- Plugin config file polished to add to plugin description.


## [1.0.3] - 2025-10-31

### Added
- Message dialogs added here and there to alert user for different failed operations.

### Changed
- Replaced logging statements, making it consistent all over app.
- Add remote URI to enable submission of user satisfaction rate, user feedback text, number of method statement and llm type (per method).

### Removed
- Unnecessary consol writes (was dedicated for debugging!).

## [1.0.4] - 2026-05-18

### Changed
- Replaced deprecated API of ReadAction abstraction with new reliable implementations.

### Removed
- Removed and replaced preview-features of Java 21 (those that made permanent in Java 25+) to make the plugin compatible with IDE run with Java recent versions
