import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    kotlin("multiplatform") version "1.4-M1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4-M1"
    maven
    `maven-publish`
}

group = "org.rnett.kframe"
version = "1.0-SNAPSHOT"

fun getNewestCommit(gitURL: String, default: String = ""): String {
    try {

        return URL("https://api.github.com/repos/$gitURL/commits?client_id=f98835efcec776b42a9c&client_secret=a73806fc946ced540c208be4320839ecb61c65d5").readText()
            .substringAfter("\"sha\":\"").substringBefore("\",").substring(0, 10)
    } catch (e: java.lang.Exception) {
        return default
    }
}

val delegates_version = getNewestCommit("rnett/delegates", "e67c18c9b3")

val ktor_version = "1.3.2-1.4-M1"
val coroutines_version = "1.3.5-1.4-M1"
//val kotlinx_html_version = "0.6.12"
val serialization_version = "0.20.0-1.4-M1"

val do_jitpack_commit_fix = true

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/soywiz/soywiz")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

kotlin {
    js {
        produceExecutable()

        browser {
            dceTask {
                keep("ktor-ktor-io.\$\$importsForInline\$\$.ktor-ktor-io.io.ktor.utils.io")
            }
            webpackTask {
                mode = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
            }
        }

//        useCommonJs()


//        configure(compilations) {
//            kotlinOptions {
//                noStdlib = true
//                sourceMapEmbedSources = "always"
//                metaInfo = true
//                sourceMap = true
//                moduleKind = "commonjs"
//            }
//        }
//
//        compilations["main"].kotlinOptions {
//            main = "call"
//        }
    }

    jvm {
        // copy compiled JS to JVM resources folder during build
        compilations.named<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation>("main") {
            tasks.getByName<Copy>(processResourcesTaskName) {
                dependsOn("jsBrowserWebpack")
                from(File("$buildDir/distributions"))
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serialization_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")

                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-servlet:$ktor_version")
                implementation("io.ktor:ktor-server-jetty:$ktor_version")
                implementation("io.ktor:ktor-server-sessions:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-apache:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
                implementation("io.ktor:ktor-auth:$ktor_version")

                implementation("ch.qos.logback:logback-classic:1.2.3")

                tasks.withType<KotlinCompile> {
                    kotlinOptions.jvmTarget = "1.8"
                }
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutines_version")

//                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinx_html_version")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        all {
            languageSettings.apply {
                enableLanguageFeature("InlineClasses")
                enableLanguageFeature("NewInference")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            }
        }
    }
}


tasks.register<JavaExec>("runKtor") {
    dependsOn("jvmJar")
    group = "application"
    main = "io.ktor.server.jetty.EngineMain"
    val t = tasks.named<Jar>("jvmJar")

    classpath(configurations.named("jvmRuntimeClasspath"), t.get())
}

val latest_commit_version = getNewestCommit("rnett/" + project.name)
val do_jitpack_fix = do_jitpack_commit_fix && "jitpack" in projectDir.path

if (do_jitpack_fix) {
    tasks["publishToMavenLocal"].doLast {
        val artifacts = publishing.publications.filterIsInstance<MavenPublication>().map { it.artifactId }

        val dir: File = File(publishing.repositories.mavenLocal().url)
            .resolve(project.group.toString().replace('.', '/'))

        dir.listFiles { it -> it.name in artifacts }
            .flatMap {
                (it.listFiles { it -> it.isDirectory }?.toList()
                    ?: emptyList<File>()) + it.resolve("maven-metadata-local.xml")
            }
            .flatMap {
                if (it.isDirectory) {
                    it.listFiles { it ->
                        it.extension == "module" ||
                                "maven-metadata" in it.name ||
                                it.extension == "pom"
                    }?.toList() ?: emptyList()
                } else listOf(it)
            }
            .forEach {
                val text = it.readText()
                println("Replacing ${project.version} with $latest_commit_version and ${project.group} with com.github.rnett.${project.name} in $it")
                it.writeText(
                    text
                        .replace(project.version.toString(), latest_commit_version)
                        .replace(project.group.toString(), "com.github.rnett.${project.name}")
                )
            }
    }
}