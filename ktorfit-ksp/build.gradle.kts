import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val enableSigning = project.hasProperty("signingInMemoryKey")

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    signing
    alias(libs.plugins.detekt)
    kotlin("kapt")
    id("app.cash.licensee")
    id("org.jlleitschuh.gradle.ktlint")
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

mavenPublishing {
    coordinates(
        libs.versions.groupId.get(),
        "ktorfit-ksp",
        libs.versions.ktorfitKsp.get(),
    )
    publishToMavenCentral()
    // publishToMavenCentral(SonatypeHost.S01) for publishing through s01.oss.sonatype.org
    if (enableSigning) {
        signAllPublications()
    }
}

dependencies {
    implementation(projects.ktorfitAnnotations)
    implementation(libs.kspApi)
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoet.ksp)

    compileOnly(libs.autoService)
    kapt(libs.autoService)

    // TEST
    testImplementation(libs.junit)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.mockito.kotlin)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config = files("../detekt-config.yml")
    buildUponDefaultConfig = false
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])

            pom {
                name.set(project.name)
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/Foso/Ktorfit/issues")
                }
                description.set("KSP Plugin for Ktorfit")
                url.set("https://github.com/Foso/Ktorfit")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/Foso/Ktorfit/blob/master/LICENSE.txt")
                    }
                }
                scm {
                    url.set("https://github.com/Foso/Ktorfit")
                    connection.set("scm:git:git://github.com/Foso/Ktorfit.git")
                }
                developers {
                    developer {
                        name.set("Jens Klingenberg")
                        url.set("https://github.com/Foso")
                    }
                }
            }
        }
    }

    repositories {
        if (
            hasProperty("sonatypeUsername") &&
            hasProperty("sonatypePassword") &&
            hasProperty("sonatypeSnapshotUrl") &&
            hasProperty("sonatypeReleaseUrl")
        ) {
            maven {
                val url =
                    when {
                        "SNAPSHOT" in version.toString() -> property("sonatypeSnapshotUrl")
                        else -> property("sonatypeReleaseUrl")
                    } as String
                setUrl(url)
                credentials {
                    username = property("sonatypeUsername") as String
                    password = property("sonatypePassword") as String
                }
            }
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}
