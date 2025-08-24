plugins {
    kotlin("jvm") version "2.0.21"
    id("com.typewritermc.module-plugin") version "1.3.0"
}

group = "dev.willem.extension"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://maven.typewritermc.com/beta")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.typewritermc:EntityExtension:0.9.0")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.9")
}

typewriter {
    namespace = "willemdev"

    extension {
        name = "ModelEngine"
        shortDescription = "Simple usage of ModelEngine for NPCs"
        description =
            "Adding the capability to use ModelEngine for non player characters in your quests. I need atleast 100 characters blablablalbal"
        engineVersion = "0.9.0-beta-165"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA

        paper {
            dependency("ModelEngine")
        }

        dependencies {
            dependency("typewritermc", "Entity")
        }
    }
}

tasks.jar {
    archiveVersion.set("")
}

kotlin {
    jvmToolchain(21)
}