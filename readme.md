<p align="center">  
<img src="https://raw.githubusercontent.com/apptanksas/horus-sync-php/master/assets/logo-horusync.svg" width="400" alt="Horusync Logo"></a>  
</p>  

<p align="center">  
<a href="https://github.com/laravel/framework/actions"><img src="https://github.com/apptanksas/horus-sync-php/actions/workflows/unit_tests.yml/badge.svg" alt="Build Status"></a>  
</p>  

**Please note:** This library currently is testing stage until publish the version 1.0.0.

# Horusync client KMM

Horus is a client library for Kotlin Multiplatform aimed at providing an easy and simple way to store data locally and synchronize it with a remote server, ensuring data security and integrity.

## Features

- Easy-to-use interface.
- It is safety.
- Validates data integrity across clients.


# 1. How to start

## Install

### Gradle

Add the repository and dependency in your `build.gradle` file:

```gradle 

repositories {
    mavenCentral() // Add the repository if it is not already added
}

dependencies {
   implementation("org.apptank.horus:client:{version}") // Kotlin Multiplatform
   implementation("org.apptank.horus:client-android:{version}") // Android
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

In the **Application** of your app configure horus using the **HorusConfigurator** class indicating
the base URL where your Horus server is configured and if you want to use horus in debug mode.

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

        // Configure Horus      
        HorusConfigurator(BASE_SERVER_URL, isDebug = true).configure(this)

        // Register the activity lifecycle callbacks      
        registerActivityLifecycleCallbacks(HorusActivityLifeCycle())
    }
}   
```   

## IOS

### Setup

* Add the configuration of ```-lsqlite3``` in the application's linker flags in XCode > Build
  Settings > "Other Linker Flags".

# 2. How to use

The main way to interact with Horus is through its Facade class called **HorusDataFacade**, with it
you will manage all data operations of your application.

## Callbacks

### Initialization validation

Horus requires an internal validation and check before starting to be used, to ensure that Horus is
ready to operate, use the **onReady** method of the **HorusDataFacade** class to know when this happens.

```kotlin  
HorusDataFacade.onReady {
    /** PUT YOUR CODE **/
}  
```  
### Subscribe to data changes

To know when a record is inserted, updated or deleted in an entity, subscribe to data changes by adding a **DataChangeListener** using the **addDataChangeListener** method of the **HorusDataFacade** class.

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

Horus internally validates whether there is an internet connection or not to synchronize the information
with the server. The operations do not depend on an internet connection, Horus will always first
register in the local database of the device.

* The ID generated internally by Horus for the records of each entity is in UUID format.
* Currently has not support for integer IDs.

### Insert data into an entity

To add a new record, use the **insert** method passing the entity name and a map with the record
attributes. The method will return a **DataResult** with the new record ID if the operation was
successful.

#### Attention

* Horus always generates the record ID internally, so it should not be given as a parameter within the
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

### Actualizar datos de un registro
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

# 3. Local testing

### Publish locally

```shell 
../gradlew publishToMavenLocal
```

Location of the dependencies

C:\Users\[User]\.m2\repository\com\apptank\horus\client\core

#### Android implementation


``` groovy 

implementation("org.apptank.horus:client-android:{version}")   
```