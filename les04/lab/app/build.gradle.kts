plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)

    implementation("org.springframework:spring-context:6.2.2")

    implementation("org.springframework:spring-aop:6.2.2")
    implementation("org.aspectj:aspectjweaver:1.9.22.1")

    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.11.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "ru.bsuedu.cad.lab.App"
}

tasks.withType<JavaExec> {
    jvmArgs("-Dfile.encoding=UTF-8")
    standardOutput = System.out
    errorOutput = System.err
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}