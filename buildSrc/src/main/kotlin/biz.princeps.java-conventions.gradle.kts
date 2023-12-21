plugins {
    `java-library`
    `maven-publish`
}

group = "biz.princeps"
version = "2.3"

repositories {
    mavenCentral()
        maven("https://jitpack.io")
    // EldoUtilitites & Landlord
    maven ("https://eldonexus.de/repository/maven-public/")
    maven ("https://eldonexus.de/repository/maven-proxies/")
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
