# Changelog

# v0.15.0
- Handle Move actions in the sync data process when a entity is moved.

# v0.14.5
- Added validate airplane mode in network validator.

# v0.14.4
- Fixed network validator when have not permission to access the network state.

# v0.14.3
- Remove sync data when on change network.

# v0.14.2
- Fixed get signal strength in android 13 and above.

# v0.14.1
- Fixed gradle configuration.

# v0.14.0
- Added restore horus tables and validation.
- Since now the insert actions are processed with replace to avoid conflicts with unique constraints.
- Added files download validation.

# v0.13.4
- Fixed sort sync actions by entity level.
- Added header request id in the service requests.

# v0.13.3
- Fixed problems related with identify entities owner in the insert actions.

# v0.13.2
- Added emit event failure when fail upload file.

# v0.13.1
- Improvement initial sync data using stream to avoid memory issues. 

# v0.13.0
- Refactor start synchronization task to download data from url json.
- Added emit progress sync data.

# v0.12.2
- Fixed issue when insert entity level 0 with user acting as.

# v0.12.1
- Added setup global callback failure.

# v0.12.0
- Added configuration to setup custom headers in the requests.

# v0.11.8
- Fixed issue  sync file manager when user is logged out.

# v0.11.7
- Fixed setup when set user acting as.

# v0.11.6
- Fixed hashes entities validation when the user is invited.
- Added chunked post actions. 

# v0.11.5
- Fixed get queue actions endpoint with excludes id.

# v0.11.4
- Fixed issue when truncate entities in refresh readable entities task.

# v0.11.3 
- Fixed calculate diff in hours.

# v0.11.2
- Fixed restore corrupted data when the entity is deleted by integrity.
- Improved hashing validation.

# v0.11.1
- Added mutex in entity restriction validator to handle concurrent access.

# v0.11.0
- Added support to shared entities.
- Fixed issue when is delete corrupted data by foreign key constraint.
- Added refresh readable data entities.

## v0.10.3
- Fixed filter delete operations in batch.

## v0.10.2
- Validate attributes datetime.
- Changed attribute type as timestamp in SQLITE as Integer.

## v0.10.1
- Fixed hashing data with null and boolean values.

## v0.10.0
- Added support to custom attribute types for entities.

## v0.9.0
- Added support to setup if a foreing entity is delete on cascade. 

## v0.8.0
- Added method `executeBatchOperations` in HorusDataFacade to execute a batch of operations.
- Deprecated method `IOperationDatabaseHelper.executeOperations` without post operation callback.

## v0.7.3
- Fixed issue when map to entity data with boolean and float values.

## v0.7.2
- Improved to getting the file uri from service when could not get the file from the local storage.
- Disposable on ready callback when the horus is ready.
- Fixed integrity data when is missing data in the database.

## v0.7.1
- Added clear database when user clear session.

## v0.7.0
- Added support for querying IS NULL and IS NOT NULL in the query builder comparator.
- Added a method to retrieve the count from a query builder.

## v0.6.0
- Added support to multiple order by in query.

## v0.5.0
- Added workflow to validate release branch.
- Added method to do query with more complex conditions.
- Added method to get count records in a entity.
- Added support query with like operator.
- Added support to entity restrictions when entity reaches a limit of records for a specific entity.
- Added handle network error in service when the connection is weak or lost.

## v0.4.10
- Added print log queries.|

## v0.4.9
- Fixed issue when try serialize a entity with long values in AnySerializer.
- Fixed when to it sends dummy attributes in validation hashing.

## v0.4.8
- Fixed sanitize SQL sentence operations.

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