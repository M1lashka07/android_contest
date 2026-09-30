pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "PuzzleCombats"
include(":app", ":domain", ":storybook")
includeBuild("libraries/uikit")
includeBuild("libraries/network")
