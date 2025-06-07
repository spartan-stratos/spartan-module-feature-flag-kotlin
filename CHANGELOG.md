# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.8.1] 2025-06-07

### Added
- Added support for filtering flags by `enabled` status in list and metadata queries.

## [0.8.0] 2025-06-07

### Changed
- Replace kotlinx serialization with Jackson for JSON handling.

## [0.7.0] 2025-06-05

### Changed
- Refactor feature flag methods to return paginated results.

## [0.6.1] 2025-06-05

### Changed
- Renamed `updateProperties` to `update` for better consistency.

## [0.6.0] 2025-06-04

### Added
- Refactor FeatureFlagService and add updateProperties method.

## [0.5.2] 2025-05-21

### Added
- Implement FeatureFlag serialization support for Jackson.

## [0.4.3] 2025-01-14

### Changed
- Make percentage optional in UserTargeting metadata

## [0.4.2] 2025-01-13

### Changed
- Support search feature flags by name or description

## [0.3.0] 2024-10-02

### Changed
- Remove specific Jedis implementation from RedisCache.

## [0.2.0] 2024-10-01

### Changed
- Add the missing `type` column into the FeatureFlagEntity.
- Modify the code to verify if the metadata feature is enabled.

## [0.1.0] 2024-09-29

### Added
- Add Slack Notifier
- Support Whitelisting and Blacklisting for UserTargeting in Metadata

## [0.0.9]
### Added
- Support FeatureFlagResult method

## [0.0.7]
### Added

- Refactors the feature flag implementation to enhance type safety, flexibility, and correct behavior:
  * Replace Map with FeatureFlag objects for stronger typing
  * Introduce MetadataContent sealed class for structured and type-safe metadata
  * Update FeatureFlagService, Repository, and Cache to work with new FeatureFlag and MetadataContent structure
  * Improve the logic in isFeatureFlagEnabled function for correct behavior
  * Revise all tests to use FeatureFlag objects and validate new metadata handling

## [0.0.5]
### Added

- Initial release
