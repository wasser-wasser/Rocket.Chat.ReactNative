package fdroid_up.rocket.reactnative.notification

import android.content.Context
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class PushNotificationsModule : Module() {
    private val pushService = PushServiceImpl()

    override fun definition() = ModuleDefinition {
        Name("PushNotifications")

        OnStartObserving {
            pushService.setModule(this@PushNotificationsModule)
        }

        OnStopObserving {
            pushService.setModule(null)
        }

        Function("register") {
            // Start registration here if needed
        }
    }
}
