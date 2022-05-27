@file:Suppress("PropertyName")

import com.google.cloud.tools.jib.gradle.JibTask
import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val kafka_client_version: String by project
val aws_msk_iam_auth: String by project
val protobuf_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
    id("com.google.cloud.tools.jib") version "3.2.1"
    id("com.google.protobuf") version "0.8.18"
}

group = "com.gaplotech.mskdemo"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.gaplotech.mskdemo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/protobuf")
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // kafka clients
    implementation("org.apache.kafka:kafka-clients:$kafka_client_version")
    implementation("org.apache.kafka:kafka-streams:$kafka_client_version")

    // aws MSK IAM auth
    implementation("software.amazon.msk:aws-msk-iam-auth:$aws_msk_iam_auth")

    implementation("com.google.protobuf:protobuf-kotlin:$protobuf_version")
    implementation("com.google.protobuf:protobuf-java-util:$protobuf_version")

    if (JavaVersion.current().isJava9Compatible) {
        implementation("javax.annotation:javax.annotation-api:+")
    }

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobuf_version"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("kotlin")
            }
        }
    }
}


tasks.withType<JibTask> {
    dependsOn("build")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "11"
    }
}