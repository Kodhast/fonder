pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Komponentbiblioteket android-ui (ARKITEKTUR.md → Byggkonfiguration). GitHub Packages kräver
        // inloggning även för läsning: GPR_USER/GPR_TOKEN i sessionen, satta av setup-android i CI.
        // exclusiveContent: se.kodhast.ui hämtas bara härifrån. Annars frågas Maven Central först,
        // och en 429 därifrån fäller bygget innan GitHub Packages ens prövas.
        exclusiveContent {
            forRepository {
                maven("https://maven.pkg.github.com/Kodhast/android-ui") {
                    name = "androidUi"
                    credentials {
                        username = providers.environmentVariable("GPR_USER")
                            .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                        password = providers.environmentVariable("GPR_TOKEN")
                            .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
                    }
                }
            }
            filter { includeGroup("se.kodhast.ui") }
        }
    }
}

rootProject.name = "Fonder"
include(":core", ":app")
