plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.zurdo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    //añadir junit
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    
    //añadir api javalin y dependencias
    implementation("io.javalin:javalin:6.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    //añadir ssl-plugin
    implementation("io.javalin.community.ssl:ssl-plugin:6.1.4")

    //añadir utilidades json
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.8.9")

    //BASE DE DATOS
    //añadir jdbi
    implementation("org.jdbi:jdbi3-core:3.45.1")
    implementation("org.jdbi:jdbi3-jackson2:3.45.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    //Autentificación
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.mindrot:jbcrypt:0.4")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "me.zurdo.Main")
    }
}

tasks.test {
    useJUnitPlatform()
}