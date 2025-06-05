package fdroid_up.rocket.reactnative


import android.app.Application
import android.content.res.Configuration
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader

import com.nozbe.watermelondb.jsi.WatermelonDBJSIPackage;
import com.facebook.react.bridge.JSIModulePackage;

import expo.modules.ApplicationLifecycleDispatcher
import fdroid_up.rocket.reactnative.networking.SSLPinningPackage;
import fdroid_up.rocket.reactnative.notification.UnifiedPushPackage;

import fdroid_up.rocket.reactnative.notification.CustomPushNotification;

import fdroid_up.rocket.reactnative.notification.UnifiedPushModule;
import fdroid_up.rocket.reactnative.BuildConfig



open class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              add(SSLPinningPackage())
              add(UnifiedPushPackage())
            }

        override fun getJSIModulePackage(): JSIModulePackage {
            return WatermelonDBJSIPackage()
        }

        override fun getJSMainModuleName(): String = "index"
        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG
        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }

  override val reactHost: ReactHost
    get() = getDefaultReactHost(this.applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, false)
    // Bugsnag.start(this)

    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      load()
    }
    ApplicationLifecycleDispatcher.onApplicationCreate(this)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }
}
