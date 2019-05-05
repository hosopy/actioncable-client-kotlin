# actioncable-client-kotlin

[![Build Status](https://travis-ci.org/raquezha/actioncable-client-kotlin.svg?branch=0.0.10-beta)](https://travis-ci.org/raquezha/actioncable-client-kotlin)
[![Release](https://jitpack.io/v/raquezha/actioncable-client-kotlin.svg)](https://jitpack.io/#raquezha/actioncable-client-kotlin)

# Note
I decided to fork this repo because hosopy is not updating his project anymore. If you want can use this forked version you can read the instructions below.

This is the actioncable client library for Kotlin.
Please see [Action Cable Overview](http://guides.rubyonrails.org/action_cable_overview.html) to understand actioncable itself.

# Usage

## Requirements

* Kotlin 1.1.4-3 or later
* [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) (experimental in Kotlin 1.1)

## Gradle

```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.raquezha:actioncable-client-kotlin:0.0.10-beta'
}
```

## Basic

```kotlin
// 1. Setup
val uri = URI("ws://cable.example.com")
val consumer = ActionCable.createConsumer(uri)

// 2. Create subscription
val appearanceChannel = Channel("AppearanceChannel")
val subscription = consumer.subscriptions.create(appearanceChannel)

subscription.onConnected = {
    // Called when the subscription has been successfully completed
}

subscription.onRejected = {
    // Called when the subscription is rejected by the server
}

subscription.onReceived = { data: Any? ->
    // Called when the subscription receives data from the server
    // Possible types...
    when (data) {
        is Int -> { }
        is Long -> { }
        is BigInteger -> { }
        is String -> { }
        is Double -> { }
        is Boolean -> { }
        is JsonObject -> { }
        is JsonArray<*> -> { }
    }
}

subscription.onDisconnected = {
    // Called when the subscription has been closed
}

subscription.onFailed = { error ->
    // Called when the subscription encounters any error
}

// 3. Establish connection
consumer.connect()

// 4. Perform any action
subscription.perform("away")

// 5. Perform any action with params
subscription.perform("hello", mapOf("name" to "world"))
```

## Passing Parameters to Channel

```kotlin
val chatChannel = Channel("ChatChannel", mapOf("room_id" to 1))
```

The parameter container is `Map<String, Any?>` and is converted to `JsonObject(Klaxon)` internally.
To know what type of value can be passed, please read [Klaxon user guide](https://github.com/cbeust/klaxon).

## Passing Parameters to Subscription#perform

```kotlin
subscription.perform("send", mapOf(
    "comment" to mapOf(
        "text" to "This is string.",
        "private" to true,
        "images" to arrayOf(
            "http://example.com/image1.jpg",
            "http://example.com/image2.jpg"
        )
    )
))
```

The parameter container is `Map<String, Any?>` and is converted to `JsonObject(Klaxon)` internally.
To know what type of value can be passed, please read [Klaxon user guide](https://github.com/cbeust/klaxon).

## Options

```kotlin
val uri = URI("ws://cable.example.com")
val options = Consumer.Options()
options.connection.reconnection = true

val consumer = ActionCable.createConsumer(uri, options)
```

Below is a list of available options.

* sslContext
    
    ```kotlin
    options.connection.sslContext = yourSSLContextInstance
    ```
    
* hostnameVerifier
    
    ```kotlin
    options.connection.hostnameVerifier = yourHostnameVerifier
    ```
    
* cookieHandler
    
    ```kotlin
    options.connection.cookieHandler = yourCookieManagerInstance
    ```
    
* query
    
    ```kotlin
    options.connection.query = mapOf("user_id" to "1")
    ```
    
* headers
    
    ```kotlin
    options.connection.headers = mapOf("X-Foo" to "Bar")
    ```
    
* reconnection
    * If reconnection is true, the client attempts to reconnect to the server when underlying connection is stale.
    * Default is `false`.
    
    ```kotlin
    options.connection.reconnection = false
    ```
    
* reconnectionMaxAttempts
    * The maximum number of attempts to reconnect.
    * Default is `30`.
    
    ```kotlin
    options.connection.reconnectionMaxAttempts = 30
    ```

* okHttpClientFactory
    * Factory instance to create your own OkHttpClient.
    * If `okHttpClientFactory` is not set, just create OkHttpClient by `OkHttpClient()`.
    
    ```kotlin
    options.connection.okHttpClientFactory = {
        OkHttpClient().also {
            it.networkInterceptors().add(StethoInterceptor())
        }
    }
    ```

## Authentication

How to authenticate a request depends on the architecture you choose.

### Authenticate by HTTP Header

```kotlin
val options = Consumer.Options()
options.connection.headers = mapOf("Authorization" to "Bearer xxxxxxxxxxx")

val consumer = ActionCable.createConsumer(uri, options)
```

### Authenticate by Query Params

```kotlin
val options = Consumer.Options()
options.connection.query = mapOf("access_token" to "xxxxxxxxxxx")

val consumer = ActionCable.createConsumer(uri, options)
```

### Authenticate by Cookie

```kotlin
val cookieManager = CookieManager()
// Some setup
...
options.connection.cookieHandler = cookieManager

val consumer = ActionCable.createConsumer(uri, options)
```

# License

MIT
