# Combats UIKit 1.0.0

Independent Android/Jetpack Compose library with bundled Poppins and original design SVG assets. Public API lives in package `ru.professionals.uikit`: theme, button, field, select and sheet, pagination, card, bottom bar, timer, checkbox.

Set `sdk.dir` in local.properties, use JDK 21+, then run `./gradlew assembleRelease`. AAR: `build/outputs/aar/uikit-release.aar`. `./gradlew publishToMavenLocal` publishes `ru.professionals:uikit:1.0.0`; publication is an explicit local developer step.

The parent training repository includes this independent build with Gradle composite builds. For the competition's independent-VCS requirement, place this directory in its own repository. The sibling Storybook app demonstrates component states.
