plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.example.movieexplorer.data"; compileSdk = 35; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }; kotlinOptions { jvmTarget = "17" } }
dependencies { implementation(project(":core:domain")); implementation("com.squareup.okhttp3:okhttp:4.12.0"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0") }
dependencies { testImplementation("junit:junit:4.13.2"); testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0"); testImplementation("org.json:json:20240303") }
