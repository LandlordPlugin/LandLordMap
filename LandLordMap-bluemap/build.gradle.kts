plugins {
    id("biz.princeps.java-conventions")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation(project(":LandLordMap-core"))
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("biz.princeps:landlord-core:4.358-SNAPSHOT")
    compileOnly("de.eldoria:eldo-util:1.9.2-DEV")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v1.7.0")
}

description = "LandLordMap-bluemap"

val shadebade = project.group as String + ".landlord."

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