plugins {
    id("biz.princeps.java-conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("de.chojo.publishdata") version "1.3.0"
}

dependencies {
    implementation(project(":LandLordMap-core"))
    compileOnly("us.dynmap:dynmap-api:3.0")
    implementation("org.codemc.worldguardwrapper:worldguardwrapper:1.2.0-SNAPSHOT")
}

description = "LandLordMap-dynmap"

val shadebade = project.group as String + ".landlord."

publishData {
    useInternalEldoNexusRepos()
    publishTask("shadowJar")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            publishData.configurePublication(this)
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("NEXUS_USERNAME")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
            name = "Eldonexus"
            url = uri(publishData.getRepository())
        }
    }
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("plugin.yml") {
                expand(
                        "version" to version
                )
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    shadowJar {
        relocate("org.codemc.worldguardwrapper", "biz.princeps.landlordmap.lib.worldguardwrapper")
        dependencies {
            include(dependency("org.codemc.worldguardwrapper:worldguardwrapper"))
        }
        relocate("de.eldoria.eldoutilities", shadebade + "eldoutilities")

        archiveBaseName.set(project.name)
        destinationDirectory.set(File("../build/libs"))
    }

    test {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }


}
