# Changelog

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