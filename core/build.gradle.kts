plugins {
    alias(libs.plugins.kotlin.jvm)
    id("buildlogic.java-conventions")
}

dependencies {
    api(libs.guava)
    api(libs.kotlin.test)
    api(libs.kotlin.compilerEmbeddable)
    api(libs.jna)
    api(libs.googlejavaformat)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

description = "Ktfmt"
