import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

object AppPreferences {

    private val Context.dataStore by preferencesDataStore(
        name = "app_prefs"
    )

    private val NOTIFICATION_PERMISSION_ASKED =
        booleanPreferencesKey("notification_permission_asked")

    suspend fun wasNotificationPermissionAsked(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[NOTIFICATION_PERMISSION_ASKED] ?: false
    }

    suspend fun setNotificationPermissionAsked(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATION_PERMISSION_ASKED] = true
        }
    }
}
