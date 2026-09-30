# Combats Network 1.0.0

Independent Kotlin/JVM REST library targeting Java 17 bytecode. It exposes `HttpClient`, `HttpRequest`, `HttpResponse`, a replaceable transport, DTOs, centralized error handling and `SupabaseClient`. Android applications call the synchronous API from an IO dispatcher.

Use JDK 21+ and `./gradlew test jar`. JAR: `build/libs/network-1.0.0.jar`. `./gradlew publishToMavenLocal` publishes `ru.professionals:network:1.0.0`; publication is an explicit local developer step.

Android supplies org.json. JVM consumers must provide `org.json:json:20240303` at runtime. Tests provide it with MockWebServer. No Android UI or application domain dependency exists.

For the competition's independent-VCS requirement, place this directory in its own repository. The provided parent repository currently carries both independent builds together.
