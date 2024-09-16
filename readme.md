# Horusync client KMM

Horus en una libreria cliente para kotlin multiplaform con el objetivo de proporcionar un forma
facil y sencilla de guardar datos en local y sincronizarlos con un servidor remoto, asegurando la
seguridad y la integridad de los datos.

## Características

- Interfaz facil de utilizar.
- Es seguro.
- Valida la integrar de los datos entre cada cliente.

# 1. Como empezar

## Instalación

### Gradle

Agregar el repositorio y la dependencia en tu archivo `build.gradle`:

```gradle 
repositories {
    mavenCentral() // o jitpack, dependiendo de donde publiques la librería
}

dependencies {
    implementation 'com.ejemplo:nombre-libreria:1.0.0'
}
```

## Android

### Configuración

#### Permisos

Horusync necesita que los permisos de *INTERNET* y *ACCESS_NETWORK_STATE* esten implementando en el*
*AndroidManifest.xml** de tu aplicación.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```   

### Inicialización

En el **Application** de tu app configura a horus usando la clase de **HorusConfigurator** indicando
la URL base en donde este configurado tu servidor de Horus y si quieres usar horus en modo de
depuración.

Tambien es necesario registrar **HorusSynchronizeHandlerActivityLifeCycle** para que este escuchando
el ciclo de la vida de la aplicación.

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

### Configuración

* Agrega la configuracion de ```-lsqlite3``` en los linkers flag de la aplicación en XCode > Build
  Settings > "Other Linker Flags".

# 2. Como usar

La manera principal de interactuar para interactuar con Horus es a traves de su clase Facade llamada
**HorusDataFacade**, con ella gestionara todas operaciones de los datos de tu aplicación.

## Callbacks

### Validación de inicialización

Horus requiera de una validación y comprobación interna antes de empezar a ser utilizado, para
asegurarse de que Horus esta listo para poder operar utiliza el metodo **onReady** de la clase *
*HorusDataFacade** para saber cuando esto ocurra.

```kotlin  
HorusDataFacade.onReady {
    /** PUT YOUR CODE **/
}  
```  

### Suscribirse a los cambios en los datos

Para saber cuando se inserta, actualiza o elimina un registro en una entidad, suscribase a los cambios de los datos agregando un **DataChangeListener** usando el metodo **addDataChangeListener** de la clase **HorusDataFacade**.


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

Remover el listener

```kotlin
HorusDataFacade.removeDataChangeListener(listener)
```

Limpiar todos los listeners

```kotlin
HorusDataFacade.removeAllDataChangeListeners()
```

## Gestión de datos

Horus internamente valida si hay conexión a internet o no para sincronizar la información con el
servidor. La operaciones no depende de una conexion a internet, horus siempre realizara de primero
el registro en la base de datos local del dispositivo.

* Los ID generadores internamente por horus para los registros de cada entidad son del formato UUID.

### Insertar datos en una entidad

Para agregar un nuevo registro, utilize el metodo de **insert** pasando el nombre de la entidad y un
mapa con los atributos del registro. El metodo devolvera un **DataResult** con el nuevo ID del
registro en caso de que la operación fue exitosa.

#### Consideraciones

* Horus siempre generara internamente el ID del registro, por lo que no debe ser dado como parametro
  dentro de los atributos.

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

Validación alternativa del resultado

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

Para actualizar un registro, utilize el metodo de **update** pasando el nombre de la entidad, el ID
del registro y un mapa con los atributos a actualizar.

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

### Eliminar un registro

Para eliminar un registro, utilize el metodo de **delete** pasando el nombre de la entidad y el ID
del registro.

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

## Consulta simple de registros

Para consultar los registros de una entidad, utilize el metodo de **querySimple** pasando el nombre
de la entidad y una lista de condiciones de busqueda.

Parametros opcionales:

* **orderBy**: Nombre de la columna por la cual se ordenara la consulta.
* **limit**: Limite de registros a obtener.
* **offset**: Numero de registros a saltar.

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

### Obtener un registro por ID

Para obtener un registro por su ID, utilize el metodo de **getById** pasando el nombre de la entidad
y el ID del registro.

```kotlin  

val userId = "0ca2caa1-74f1-4e58-a6a7-29e79efedfe4"

val user = HorusDataFacade.getById("users", userId)

if (user != null) {
//** YOUR CODE HERE WHEN RECORD EXISTS **/
}

```  

## Autenticación

Horus necesita de un token de acceso del usuario en sesión para poder enviar la información al
servidor remoto, para esto utilize la clase de **HorusAuthentication** dentro del ciclo de vida de
la sesión usuario de su aplicación para poder configurar el access token.

### Configurar access token

```kotlin  
HorusAuthentication.setupUserAccessToken("[ACCESS_TOKEN]")  
```  

### Limpiar sesion

```kotlin
HorusAuthentication.clearSession()  
```  

### Configuración para actuar como usuario invitado

Si el usuario debe actuar en representación de otro usuario por invitación, se debe configurar el
usuario propietario de las entidades de la siguiente manera:

```kotlin  
HorusAuthentication.setUserActingAs("{USER_OWNER_ID}")  
```  

# 3. Pruebas en local

### Publicar en local

```shell 
../gradlew publishToMavenLocal
```

Ubicacion de las dependencias

C:\Users\[User]\.m2\repository\com\apptank\horus\client\core

#### Implementación en Android

``` groovy 

implementation("com.apptank.horus.client:core-android:{version}")   
``` 

## Links de ayuda

- [How to publish your own Kotlin Multiplatform library to MavenCentral](https://medium.com/@cristurean.marius.ovidiu/how-to-publish-your-own-kotlin-multiplatform-library-to-mavencentral-4bc02c8e109d)