description = ""

kotlin.sourceSets {
    val jvmAndNixMain by getting {
        dependencies {
            api(project(":ktor-server:ktor-server-core"))
            api(project(":ktor-http:ktor-http-cio"))
        }
    }

    val jvmAndNixTest by getting {
        dependencies {
            implementation(project(":ktor-server:ktor-server-test-host"))
        }
    }
}
