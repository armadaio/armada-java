/*
 * Copyright JAMF Software, LLC
 */

val springDataVersion = "3.3.2"
val springContextSupportVersion = "6.1.11"
val jacksonVersion = "2.17.2"

plugins {
    id("regatta.library-conventions")
}

group = "com.jamf.regatta"
version = "1.4.1-SNAPSHOT"

dependencies {
    api("org.springframework.data:spring-data-keyvalue:$springDataVersion")
    api("org.springframework:spring-context-support:$springContextSupportVersion")
    api(project(mapOf("path" to ":regatta-java-core")))

    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
}
