plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "lab.onerm.app"
 compileSdk = 35
 defaultConfig { applicationId = "lab.onerm.app"; minSdk = 26; targetSdk = 35; versionCode = 17; versionName = "1.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation(project(":engine")) }