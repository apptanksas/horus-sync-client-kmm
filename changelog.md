# Changelog
## v0.4.7
- Fixed exception when try to insert in horus_entities with a entity name exists.
- Fixed issue in Entity get attributes polymorphic like getInt and getFloat.

## v0.4.6
- Fixed issue when it tries to insert or update a entity with a null value.

## v0.4.5
- Added fallback to find local sync file. 
- Fix onUpload progress calculation.

## v0.4.4
- Added method in Horus entity to get file reference.

## v0.4.3
- Fixed method onReady to invoke the callback when the horus is already ready.

## v0.4.2
- Added methods to get attributes safely from entities.

## v0.4.1
- Fixed version in gradle properties.

## v0.4.0
- Added method into `HorusDataFacade` to insert multiple entities in batch.
- Added method into `HorusDataFacade` to update multiple entities in batch.

## v0.3.0

- Added support to upload files to the server and synchronize them.
- Added method into `HorusDataFacade` to upload files.
- Added method into `HorusDataFacade` to get the url of the file.
- Added method into `HorusDataFacade` to validate if horus is ready.
- [BreakingChange] HorusConfig now receives new param called `UploadFilesConfig` to configure the
  upload files.

## v0.2.0

- Added method into `HorusDataFacade` to force a synchronization.
- Added method into `HorusDataFacade` to get the last synchronization date.
- Added method into `HorusDataFacade` to get the entity names list.
- Added method into `HorusDataFacade` to validate if exists data to synchronize.
- Added batch synchronization support.
- [BreakingChange]  HorusConfigurator now receives a `HorusConfig` object instead of a parameters
  list.

## v0.1.5

- Fixed JSON map serializer
- Fixed Http client implementation
- Change IOS HorusConfigurator to implement logger
- Fixed avoid parallel remote synchronizer manager

## v0.1.4

## v0.1.3

- Fix releasing to maven central

## v0.1.2

- Fix gradle properties

## v0.1.1

- Update README.md
- Added workflows for CI/CD.

## v0.1.0

- Initial version