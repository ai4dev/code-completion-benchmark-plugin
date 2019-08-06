import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

group = "org.jetbrains.research.groups.ml_methods"
version = "0.1"

plugins {
    id("org.jetbrains.intellij") version "0.4.9" apply true
    kotlin("jvm") version "1.3.41" apply true
    java
}

repositories {
    mavenCentral()
    jcenter()
}

intellij {
    pluginName = "code-completion-benchmark"
    version = "2019.1.3"
    setPlugins(project(":code-completion-benchmark-toolkit"))

    updateSinceUntilBuild = false
}

dependencies {
    compile("org.jboss.marshalling", "jboss-marshalling", "2.0.7.Final")
    compile("org.jetbrains.kotlin", "kotlin-reflect", "1.3.41")
    implementation(kotlin("stdlib-jdk8"))
}


tasks {
    withType<KotlinJvmCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<RunIdeTask> {
        jvmArgs("-Xmx1g")
    }
}