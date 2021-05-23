plugins {
    id("biz.princeps.java-conventions")
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    implementation(project(":LandLordMap-core"))
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("biz.princeps:landlord-core:4.350")
    compileOnly("de.eldoria:eldo-util:1.8.4")
    compileOnly("us.dynmap:dynmap-api:3.0")
    implementation("org.codemc.worldguardwrapper:worldguardwrapper:1.2.0-SNAPSHOT")
}

description = "LandLordMap-dynmap"

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