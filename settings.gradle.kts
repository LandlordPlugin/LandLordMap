rootProject.name = "LandLordMap"
include("LandLordMap-core")
include("LandLordMap-dynmap")
include("LandLordMap-bluemap")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}
