# Horusync client KMM

**Descripción corta**: Una breve descripción de lo que hace tu librería.

## Características

- Lista breve de características clave de la librería.
- Ejemplo de cómo puede mejorar el flujo de trabajo o la aplicación.

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

# Modo de Uso
## Android

### Configuración

#### Permisos

Horusync necesita que los permisos de *INTERNET* y *ACCESS_NETWORK_STATE* esten implementando en el **AndroidManifest.xml** de tu aplicación.
```xml
<uses-permission android:name="android.permission.INTERNET" />  
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Inicialización

En el **Application** de tu app configura a horus usando la clase de **HorusConfigurator** indicando la URL base en donde este configurado tu servidor de Horus y si quieres usar horus en modo de depuración.

Tambien es necesario registrar **HorusSynchronizeHandlerActivityLifeCycle** para que este escuchando el ciclo de la vida de la aplicación.

```kotlin
class MainApplication : Application() {  
      
    override fun onCreate() {  
        super.onCreate()  
  
        setupHorus()  
    }  
      
    private fun setupHorus(){  
  
        val BASE_SERVER_URL = "https://api.yourdomain.com/sync"  
  // Configure Horus  
  HorusConfigurator(BASE_SERVER_URL, isDebug = true).configure(this)  
  
        // Register the activity lifecycle callbacks  
  registerActivityLifecycleCallbacks(HorusSynchronizeHandlerActivityLifeCycle())  
    }  
}
```


## Pruebas en local

### Publicar en local

```shell ../gradlew publishToMavenLocal```

Ubicacion de las dependencias

C:\Users\[User]\.m2\repository\com\apptank\horus\client\core

#### Implementación en Android

```groovy  
 implementation("com.apptank.horus.client:core-android:{version}")   
```  

## Links de ayuda

- [How to publish your own Kotlin Multiplatform library to MavenCentral](https://medium.com/@cristurean.marius.ovidiu/how-to-publish-your-own-kotlin-multiplatform-library-to-mavencentral-4bc02c8e109d)