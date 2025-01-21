<p align="center">  
<img src="https://raw.githubusercontent.com/apptanksas/horus-sync-php/master/assets/logo-horusync.svg" width="400" alt="Horusync Logo">
<br/>
<img src="https://github.com/apptanksas/horus-sync-client-kmm/actions/workflows/unit_tests.yml/badge.svg" alt="Build Status">
<img src="https://img.shields.io/maven-central/v/org.apptank.horus/client" alt="Latest Stable Version">
<img src="https://img.shields.io/github/license/apptanksas/horus-sync-client-kmm" alt="License">
</p>  

**Please note:** This library currently is testing stage until publish the version 1.0.0. Meanwhile,
it could have breaking changes in the API.

# Table of Contents

- [Horusync client KMM](#horusync-client-kmm)
  - [Features](#features)
- [1. How to start](#1-how-to-start)
  - [Install](#install)
    - [Gradle](#gradle)
  - [Android](#android)
    - [Setup](#setup)
      - [Permissions](#permissions)
      - [Initialization](#initialization)
  - [IOS](#ios)
    - [Setup](#setup-1)
      - [1. Create an app delegate to handle lifecycle events](#1-create-an-app-delegate-to-handle-lifecycle-events)
      - [2. Implement a NetworkValidator to check the network status](#2-implement-a-networkvalidator-to-check-the-network-status)
      - [3. Configure Horus in the initialization of the application](#3-configure-horus-in-the-initialization-of-the-application)
- [2. How to use](#2-how-to-use)
  - [Callbacks](#callbacks)
    - [Initialization validation](#initialization-validation)
    - [Subscribe to data changes](#subscribe-to-data-changes)
      - [Remove the listener](#remove-the-listener)
      - [Clear all listeners](#clear-all-listeners)
  - [Data management](#data-management)
    - [Insert data into an entity](#insert-data-into-an-entity)
    - [Update data of a record](#update-data-of-a-record)
    - [Delete a record](#delete-a-record)
  - [Simple record query](#simple-record-query)
    - [Get a record by ID](#get-a-record-by-id)
    - [Upload files](#upload-files)
- [Utilities](#utilities)
  - [Get entities name](#get-entities-name)
  - [Force synchronization](#force-synchronization)
  - [Validate if exists data to synchronize](#validate-if-exists-data-to-synchronize)
  - [Get the last synchronization date](#get-the-last-synchronization-date)
- [Authentication](#authentication)
  - [Setup access token](#setup-access-token)
  - [Clear session](#clear-session)
  - [Setup to act as a guest user by another user](#setup-to-act-as-a-guest-user-by-another-user)
- [3. Local testing](#3-local-testing)
  - [Publish locally](#publish-locally)


# Horusync client KMM

Horus is a client library for Kotlin Multiplatform aimed at providing an easy and simple way to
store data locally and synchronize it with a remote server, ensuring data security and integrity.

## Features

- Easy-to-use interface.
- It is safety.
- Validates data integrity across clients.
- Support for file uploads.
- Support for entity restrictions.


# 1. How to start

## Install

### Gradle

Add the repository and dependency in your `build.gradle` file:

```gradle 

kotlin {
     
     /** ... Another configurations ... */
     
     sourceSets {
          androidMain.dependencies {
              implementation("org.apptank.horus:client-android:{version}") // Android
          }
          commonMain.dependencies {
            /** ... Dependencies for common module ... */
          }
          iosMain.dependencies {
              implementation("org.apptank.horus:client:{version}") // IOS
          }
    }
}

```

## Android

### Setup

#### Permissions

Horusync needs the *INTERNET* and *ACCESS_NETWORK_STATE* permissions to be implemented in the
*AndroidManifest.xml* of your application.

```xml

<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

```   

### Initialization

In the **Application** of your app configure horus using the **HorusConfigurator** class passing a
**HorusConfig** object with the base server URL and the configuration of the pending actions.

The `UploadFilesConfig` class defines the settings for uploading files to the server.
* **baseStoragePath**: The base path where the files will be stored.
* **mimeTypesAllowed**: List of allowed mime types.
* **maxFileSize**: Maximum file size allowed in bytes.

The `PushPendingActionsConfig` class defines the settings for managing pending actions before
synchronization.
It includes the size of batches and the time expiration threshold to do synchronization.

* **batchSize**: Number of actions to synchronize in each batch.
* **expirationTime**: The maximum time in seconds allowed between synchronizations before forcing
  one. Default is 12 hours.

It is also necessary to register **HorusActivityLifeCycle** to listen to the
application's life cycle.

```kotlin 
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupHorus()
    }

    private fun setupHorus() {

        val BASE_SERVER_URL = "https://api.yourdomain.com/sync"

        val uploadFileConfig = UploadFilesConfig(
          baseStoragePath = filesDir.absolutePath,
          mimeTypesAllowed = listOf(FileMimeType.IMAGE_JPEG_IMAGE_JPG, FileMimeType.IMAGE_PORTABLE_NETWORK_GRAPHICS),
          maxFileSize = 1024 * 1024 * 5 // 5MB
        )
      
        // Configure Horus      
        val config = HorusConfig(
            BASE_SERVER_URL,
            uploadFileConfig,
            PushPendingActionsConfig(batchSize = 10, expirationTime = 60 * 60 * 12L),
            isDebug = true
        )

        HorusConfigurator(config).configure(this)

        // Register the activity lifecycle callbacks      
        registerActivityLifecycleCallbacks(HorusActivityLifeCycle())
    }
}   
```   

## IOS

### Setup

* Add the configuration of ```-lsqlite3``` in the application's linker flags in XCode > Build
  Settings > "Other Linker Flags".

### Initialization

#### 1. Create an app delegate to handle lifecycle events

```swift
    class AppDelegate: NSObject, UIApplicationDelegate {  
	    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {  
	        IOSHorusLifeCycle().onCreate()  
	        IOSHorusLifeCycle().onResume()  
	        return true  
	    }  
	  
	    func applicationDidBecomeActive(_ application: UIApplication) {  
	        IOSHorusLifeCycle().onResume()  
	    }  
	  
	    func applicationWillResignActive(_ application: UIApplication) {  
	        IOSHorusLifeCycle().onPause()  
	    }
}
```   

#### 2. Implement a NetworkValidator to check the network status

```swift

final class NetworkValidator: ClientINetworkValidator {
    
    static let shared = NetworkValidator()

    private let queue = DispatchQueue(label: "NetworkMonitor")
    private let mutableQueue = DispatchQueue(label: "NetworkMonitor.mutable")
    private let monitor = NWPathMonitor()
    private var networkChangeCallback: (() -> Void)?
    private var isMonitoring = false
    
    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            guard let self = self else { return }

            if self.isMonitoring {
                self.networkChangeCallback?()
            }
        }
        monitor.start(queue: queue)
    }

    func isNetworkAvailable() -> Bool {
        let path = monitor.currentPath
        return path.status == .satisfied
    }

    func onNetworkChange(callback: @escaping () -> Void) {
        self.networkChangeCallback = callback
        callback()
    }

    func registerNetworkCallback() {
        guard !isMonitoring else { return }
        setIsMonitoring(true)
    }

    func unregisterNetworkCallback() {
        guard isMonitoring else { return }
        setIsMonitoring(false)
    }
    
    private func setIsMonitoring(_ bool: Bool) {
        mutableQueue.sync {
            isMonitoring = bool
        }
    }
}
```

#### 3. Configure Horus in the initialization of the application

```swift

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    init(){
        IOSHorusConfigurator().configure(networkValidator: NetworkValidator.shared)
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

# 2. How to use

The main way to interact with Horus is through its Facade class called **HorusDataFacade**, with it
you will manage all data operations of your application.

## Callbacks

### Initialization validation

Horus requires an internal validation and check before starting to be used, to ensure that Horus is
ready to operate, use the **onReady** method of the **HorusDataFacade** class to know when this
happens.

```kotlin  
HorusDataFacade.onReady {
    /** PUT YOUR CODE **/
}  
```  

### Subscribe to data changes

To know when a record is inserted, updated or deleted in an entity, subscribe to data changes by
adding a **DataChangeListener** using the **addDataChangeListener** method of the **HorusDataFacade
** class.

```kotlin 
HorusDataFacade.addDataChangeListener(object : DataChangeListener {

    override fun onInsert(entity: String, id: String, data: DataMap) {
        /** WHEN IS INSERTED A NEW RECORD **/
    }
    override fun onUpdate(entity: String, id: String, data: DataMap) {
        /** WHEN IS UPDATED A RECORD **/
    }

    override fun onDelete(entity: String, id: String) {
        /** WHEN IS DELETED A RECORD **/
    }
})  
```  

Remove the listener

```kotlin
HorusDataFacade.removeDataChangeListener(listener)
```

Clear all listeners

```kotlin
HorusDataFacade.removeAllDataChangeListeners()
```

## Data management

Horus internally validates whether there is an internet connection or not to synchronize the
information
with the server. The operations do not depend on an internet connection, Horus will always first
register in the local database of the device.

* The ID generated internally by Horus for the records of each entity is in UUID format.
* Currently has not support for integer IDs.

### Insert data into an entity

To add a new record, use the **insert** method passing the entity name and a map with the record
attributes. The method will return a **DataResult** with the new record ID if the operation was
successful.

#### Attention

* Horus always generates the record ID internally, so it should not be given as a parameter within
  the
  attributes.

```kotlin  

val entityName = "users"
val newData = mapOf("name":"Aston", "lastname":"Coleman")

val result = HorusDataFacade.insert(entityName, newData)

when (result) {
    is DataResult.Success -> {
        val entityId = result.data
        /** YOUR CODE HERE WHEN INSERT IS SUCCESSFUL */
    }
    is DataResult.Failure -> {
        /** YOUR CODE HERE WHEN INSERT FAILS */
    }
    is DataResult.NotAuthorized -> {
        /** YOUR CODE HERE WHEN INSERT FAILS BECAUSE OF NOT AUTHORIZED */
    }
}  
```  

Alternative result validation

```kotlin  
result.fold(
    onSuccess = {
        val entityId = result.data
        /** YOUR CODE HERE WHEN INSERT IS SUCCESSFUL */
    },
    onFailure = {
        /** YOUR CODE HERE WHEN INSERT FAILS */
    })  
```  

### Update data of a record

To update a record, use the **update** method passing the entity name, the record ID, and a map with
the attributes to update.

```kotlin  
val userId = "0ca2caa1-74f1-4e58-a6a7-29e79efedfe4"
val newName = "Elton"
val result = HorusDataFacade.update(
    "users", userId, mapOf(
        "name" to newName
    )
)
when (result) {
    is DataResult.Success -> {
        /** YOUR CODE HERE WHEN SUCCESS */
    }

    is DataResult.Failure -> {
        /** YOUR CODE HERE WHEN FAILURE */
    }

    is DataResult.NotAuthorized -> {
        /** YOUR CODE HERE WHEN UPDATE FAILS BECAUSE OF NOT AUTHORIZED */
    }
}  
```  

### Delete a record

To delete a record, use the **delete** method passing the entity name and the record ID.

```kotlin  

val userId = "0ca2caa1-74f1-4e58-a6a7-29e79efedfe4"

val result = HorusDataFacade.delete("users", userId)

when (result) {
    is DataResult.Success -> {
        /** YOUR CODE HERE WHEN SUCCESS */
    }
    is DataResult.Failure -> {
        /** YOUR CODE HERE WHEN FAILURE */
    }
    is DataResult.NotAuthorized -> {
        /** YOUR CODE HERE WHEN UPDATE FAILS BECAUSE OF NOT AUTHORIZED */
    }
}  
```  

## Simple record query

To query the records of an entity, use the **querySimple** method passing the entity name and a list
of search conditions.

Optional parameters:

* **orderBy**: Name of the column by which the query will be ordered.
* **limit**: Limit of records to obtain.
* **offset**: Number of records to skip.

```kotlin  
val whereConditions = listOf(
    SQL.WhereCondition(SQL.ColumnValue("age", 10), SQL.Comparator.GREATER_THAN_OR_EQUALS),
)
HorusDataFacade.querySimple("users", whereConditions, orderBy = "name")
    .fold(
        onSuccess = { users ->
            //** YOUR CODE HERE WHEN SUCCESS */ 
        },
        onFailure = {
            //** YOUR CODE HERE WHEN FAILURE */ 
        })

```  

### Get a record by ID

To get a record by its ID, use the **getById** method passing the entity name and the record ID.

```kotlin  

val userId = "0ca2caa1-74f1-4e58-a6a7-29e79efedfe4"

val user = HorusDataFacade.getById("users", userId)

if (user != null) {
//** YOUR CODE HERE WHEN RECORD EXISTS **/
}

```  

### Upload files

To upload files to the server is simple, use the **uploadFile** method passing the file data in bytes and then use the **getFileUrl** method to get the file URL to use where you need it.

```kotlin  

val fileReference = HorusDataFacade.uploadFile(fileData)

val fileUrl = HorusDataFacade.getFileUrl(fileReference)

```  


## Utilities

### Get entities name

To get the list of entities that are being managed by Horus, use the **getEntityNames** method.

```kotlin
val entityNames = HorusDataFacade.getEntityNames()
```

### Force synchronization

To force the synchronization of the data with the server, use the **forceSync** method.

```kotlin
HorusDataFacade.forceSync(onSuccess = {
    /** YOUR CODE HERE WHEN SUCCESS */
}, onFailure = {
    /** YOUR CODE HERE WHEN FAILURE */
})
```

### Validate if exists data to synchronize

To validate if there is data to synchronize, use the **hasDataToSync** method.

```kotlin
val hasDataToSync = HorusDataFacade.hasDataToSync()
```

### Get the last synchronization date

To get the last synchronization date, use the **getLastSyncDate** method. This method will return a
timestamp in seconds.

```kotlin
val lastSyncDate = HorusDataFacade.getLastSyncDate()
```

## Authentication

Horus needs a user access token in session to be able to send the information to the remote server,
for this use the **HorusAuthentication** class within the life cycle of the user session of your
application to be able to configure the access token.

### Setup access token

```kotlin  
HorusAuthentication.setupUserAccessToken("{ACCESS_TOKEN}")  
```  

### Clear session

```kotlin
HorusAuthentication.clearSession()  
```  

### Setup to act as a guest user by another user

If the user must act on behalf of another user by invitation, the owner user of the entities must be
configured as follows:

```kotlin  
HorusAuthentication.setUserActingAs("{USER_OWNER_ID}")  
```  

## Entity restrictions

You can add specific restrictions for an entity, for example, to limit the number of records that can be stored in an entity. If the restriction is reached, the operation will fail and data result will be **DataResult.NotAuthorized**.

Use the **setEntityRestrictions** method of the **HorusDataFacade** class to set the restrictions.

```kotlin
 HorusDataFacade.setEntityRestrictions(
  listOf(
    MaxCountEntityRestriction("tasks", 100) // Limit to 100 records in the "tasks" entity
  )
)
```

### Restrictions supported
- **MaxCountEntityRestriction**: Limit the number of records that can be stored in an entity.

# 3. Local testing

### Publish locally

```shell 
../gradlew publishToMavenLocal
```

Location of the dependencies

C:\Users\[User]\.m2\repository\com\apptank\horus\client\core