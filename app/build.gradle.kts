plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.oneclick.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.oneclick.vpn"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEMO", "true")
            buildConfigField("String", "DEFAULT_WG_ASSET", "\"wg/client01.conf\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "DEMO", "false")
            buildConfigField("String", "DEFAULT_WG_ASSET", "\"wg/client01.conf\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "client"

    productFlavors {
        (1..30).forEach { i ->
            val name = "client%02d".format(i)
            create(name) {
                dimension = "client"
                applicationIdSuffix = ".$name"
                versionNameSuffix = "-$name"
                buildConfigField("String", "DEFAULT_WG_ASSET", "\"wg/${name}.conf\"")
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
tasks.register("generateWgStubs") {
    group = "oneclick"
    description = "Generate 30 WireGuard stub configs into assets/wg"
    doLast {
        val dir = file("src/main/assets/wg")
        if (!dir.exists()) dir.mkdirs()
        (1..30).forEach { i ->
            val name = String.format("client%02d.conf", i)
            file(dir.resolve(name)).writeText(
                """
                [Interface]
                PrivateKey = XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX=
                Address = 10.77.0.${'$'}{i + 1}/32
                DNS = 1.1.1.1

                [Peer]
                PublicKey = YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY=
                Endpoint = 0.0.0.0:51820
                AllowedIPs = 0.0.0.0/0, ::/0
                PersistentKeepalive = 25
                """.trimIndent()
            )
        }
    }
}

tasks.register("verifyWgAssets") {
    group = "verification"
    description = "Verify WG assets"
    doLast {
        val dir = file("src/main/assets/wg")
        require(dir.exists()) { "assets/wg not found" }
        (1..30).forEach { i ->
            val f = file(dir.resolve("client%02d.conf".format(i)))
            require(f.exists()) { "Missing ${'$'}f" }
            val s = f.readText()
            require("[Interface]" in s && "[Peer]" in s) { "Broken ${'$'}f" }
        }
    }
}

tasks.register("verifySecurityConfig") {
    group = "verification"
    description = "Release must forbid user-CA and cleartext"
    doLast {
        val f = file("src/release/res/xml/network_security_config.xml")
        require(f.exists()) { "network_security_config.xml missing" }
        val s = f.readText()
        require(!s.contains("src=\"user\"")) { "User CAs forbidden" }
        require(!s.contains("cleartextTrafficPermitted=\"true\"")) { "Cleartext forbidden" }
    }
}

tasks.matching { it.name.startsWith("assemble") && it.name.contains("Release") }.configureEach {
    dependsOn("verifySecurityConfig")
    dependsOn("verifyWgAssets")
}
