plugins {
    kotlin("jvm") version "2.3.0"
    application
}

group = "io.github.jwyoon1220"
version = "1.0-SNAPSHOT"

val gdxVersion = "1.13.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // libGDX core
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    // libGDX LWJGL3 desktop backend (Windows AMD64)
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    // libGDX desktop natives
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    // JSON parsing
    implementation("com.google.code.gson:gson:2.13.2")
    // VLCJ video playback
    implementation("uk.co.caprica:vlcj:4.12.1")
    implementation("uk.co.caprica:vlcj-natives:4.12.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.jwyoon1220.stellastep.DesktopLauncherKt")
}

tasks.named<JavaExec>("run") {
    workingDir = project.projectDir
}

tasks.test {
    useJUnitPlatform()
}