plugins {
    kotlin("jvm") version "1.9.20-Beta2"
    application
}

group = "com.lomovtsev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


val littleKtVersion = "0.7.0" // get the latest release at the top
val kotlinCoroutinesVersion = "1.6.4" // or whatever version you are using

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.lehaine.littlekt:core:$littleKtVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}