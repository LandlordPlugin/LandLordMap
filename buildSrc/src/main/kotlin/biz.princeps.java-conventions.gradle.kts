plugins {
    `java-library`
    `maven-publish`
}

group = "biz.princeps"
version = "2.3"

repositories {
    mavenCentral()
    // JitPack
    maven { url = uri("https://jitpack.io") }
    // Spigot
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    // EldoUtilitites & Landlord
    maven { url = uri("https://eldonexus.de/repository/maven-public/") }
    // CodeMc-public
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    // Dynmap
    maven { url = uri("https://repo.mikeprimm.com/") }
}

allprojects {
    java {
        withSourcesJar()
        withJavadocJar()
        toolchain{
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }
}

tasks {
    publish {
        dependsOn(build)
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
