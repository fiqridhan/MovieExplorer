plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.example.movieexplorer.domain"; compileSdk = 35; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }; kotlinOptions { jvmTarget = "17" } }
dependencies { testImplementation("junit:junit:4.13.2"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") }
