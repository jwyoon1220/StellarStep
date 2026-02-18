plugins {
    kotlin("jvm") version "2.3.0"
}

group = "io.github.jwyoon1220"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // Source: https://mvnrepository.com/artifact/com.googlecode.soundlibs/jlayer
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
    // Source: https://mvnrepository.com/artifact/uk.co.caprica/vlcj
    implementation("uk.co.caprica:vlcj:4.12.1")
    // Source: https://mvnrepository.com/artifact/uk.co.caprica/vlcj-natives
    implementation("uk.co.caprica:vlcj-natives:4.12.0")
    // Source: https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}