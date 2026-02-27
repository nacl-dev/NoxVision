package com.noxvision.app

import android.content.Context
import android.content.SharedPreferences
import com.noxvision.app.billing.FakeSharedPreferencesSecurity
import com.noxvision.app.util.TestSecureStorageManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class CameraSettingsTest {

    private lateinit var mockContext: Context
    private lateinit var fakePrefs: FakeSharedPreferencesSecurity
    private lateinit var testSecureManager: TestSecureStorageManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferencesSecurity()
        mockContext = TestContext(fakePrefs)
        testSecureManager = TestSecureStorageManager()
        CameraSettings.setSecureStorageManager(testSecureManager)
    }

    @Test
    fun testSetWifiPasswordEncodesIt() {
        val password = "secretPassword123"
        CameraSettings.setWifiPassword(mockContext, password)

        // Verify it's stored encrypted in prefs
        val stored = fakePrefs.getString("wifi_password", null)
        assertEquals("ENC:$password", stored)
    }

    @Test
    fun testGetWifiPasswordDecodesIt() {
        // Setup prefs with encrypted password
        fakePrefs.put("wifi_password", "ENC:secretPassword123")

        val retrieved = CameraSettings.getWifiPassword(mockContext)
        assertEquals("secretPassword123", retrieved)
    }

    @Test
    fun testGetWifiPasswordHandlesLegacyPlaintext() {
        // Setup prefs with legacy plaintext password
        fakePrefs.put("wifi_password", "legacyPassword")

        val retrieved = CameraSettings.getWifiPassword(mockContext)
        // Should return plaintext as-is because TestSecureStorageManager.decrypt handles it
        assertEquals("legacyPassword", retrieved)
    }

    @Test
    fun testDefaultPassword() {
        // No password set
        val retrieved = CameraSettings.getWifiPassword(mockContext)
        // Should return default ("12345678")
        assertEquals("12345678", retrieved)
    }
}

// Minimal Context implementation for testing SharedPreferences
// Implementing all abstract members based on error messages
class TestContext(private val prefs: SharedPreferences) : Context() {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return prefs
    }

    override fun getPackageName(): String {
        return "com.noxvision.app"
    }

    // Abstract methods implementation
    override fun getAssets() = throw NotImplementedError()
    override fun getResources() = throw NotImplementedError()
    override fun getPackageManager() = throw NotImplementedError()
    override fun getContentResolver() = throw NotImplementedError()
    override fun getMainLooper() = throw NotImplementedError()
    override fun getApplicationContext() = this
    override fun setTheme(resid: Int) {}
    override fun getTheme() = throw NotImplementedError()
    override fun getClassLoader() = throw NotImplementedError()
    override fun getApplicationInfo() = throw NotImplementedError()
    override fun getPackageResourcePath() = throw NotImplementedError()
    override fun getPackageCodePath() = throw NotImplementedError()
    override fun getFilesDir() = throw NotImplementedError()
    override fun getNoBackupFilesDir() = throw NotImplementedError()
    override fun getExternalFilesDir(type: String?) = throw NotImplementedError()
    override fun getExternalFilesDirs(type: String?) = throw NotImplementedError()
    override fun getObbDir() = throw NotImplementedError()
    override fun getObbDirs() = throw NotImplementedError()
    override fun getCacheDir() = throw NotImplementedError()
    override fun getCodeCacheDir() = throw NotImplementedError()
    override fun getExternalCacheDir() = throw NotImplementedError()
    override fun getExternalCacheDirs() = throw NotImplementedError()
    override fun getExternalMediaDirs() = throw NotImplementedError()
    override fun fileList() = throw NotImplementedError()
    override fun getDir(name: String?, mode: Int) = throw NotImplementedError()
    override fun openFileInput(name: String?) = throw NotImplementedError()
    override fun openFileOutput(name: String?, mode: Int) = throw NotImplementedError()
    override fun deleteFile(name: String?) = throw NotImplementedError()
    override fun getFileStreamPath(name: String?) = throw NotImplementedError()
    override fun getDataDir() = throw NotImplementedError()
    override fun getWallpaper() = throw NotImplementedError()
    override fun peekWallpaper() = throw NotImplementedError()
    override fun getWallpaperDesiredMinimumWidth() = 0
    override fun getWallpaperDesiredMinimumHeight() = 0
    override fun setWallpaper(bitmap: android.graphics.Bitmap?) {}
    override fun setWallpaper(data: java.io.InputStream?) {}
    override fun clearWallpaper() {}
    override fun startActivity(intent: android.content.Intent?) {}
    override fun startActivity(intent: android.content.Intent?, options: android.os.Bundle?) {}
    override fun startActivities(intents: Array<out android.content.Intent>?) {}
    override fun startActivities(intents: Array<out android.content.Intent>?, options: android.os.Bundle?) {}
    override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {}
    override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: android.os.Bundle?) {}
    override fun sendBroadcast(intent: android.content.Intent?) {}
    override fun sendBroadcast(intent: android.content.Intent?, receiverPermission: String?) {}
    override fun sendOrderedBroadcast(intent: android.content.Intent?, receiverPermission: String?) {}
    override fun sendOrderedBroadcast(intent: android.content.Intent, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) {}
    override fun sendBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) {}
    override fun sendBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?) {}
    override fun sendOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) {}
    override fun sendStickyBroadcast(intent: android.content.Intent?) {}
    override fun sendStickyOrderedBroadcast(intent: android.content.Intent?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) {}
    override fun removeStickyBroadcast(intent: android.content.Intent?) {}
    override fun sendStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) {}
    override fun sendStickyOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) {}
    override fun removeStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) {}

    // Implement missing methods flagged by compiler
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?): android.content.Intent? = null
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?): android.content.Intent? = null
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, flags: Int): android.content.Intent? = null
    override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?, flags: Int): android.content.Intent? = null

    override fun unregisterReceiver(receiver: android.content.BroadcastReceiver?) {}
    override fun startService(service: android.content.Intent?) = null
    override fun stopService(service: android.content.Intent?) = false
    override fun bindService(service: android.content.Intent, conn: android.content.ServiceConnection, flags: Int) = false
    override fun unbindService(conn: android.content.ServiceConnection) {}

    override fun startInstrumentation(className: android.content.ComponentName, profileFile: String?, arguments: android.os.Bundle?) = false

    override fun getSystemService(name: String): Any? = null
    override fun getSystemServiceName(serviceClass: Class<*>) = null
    override fun checkPermission(permission: String, pid: Int, uid: Int) = 0
    override fun checkCallingPermission(permission: String) = 0
    override fun checkCallingOrSelfPermission(permission: String) = 0
    override fun checkSelfPermission(permission: String) = 0
    override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) {}
    override fun enforceCallingPermission(permission: String, message: String?) {}
    override fun enforceCallingOrSelfPermission(permission: String, message: String?) {}
    override fun grantUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) {}
    override fun revokeUriPermission(uri: android.net.Uri?, modeFlags: Int) {}
    override fun revokeUriPermission(targetPackage: String?, uri: android.net.Uri?, modeFlags: Int) {}

    override fun checkUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int) = 0
    override fun checkCallingUriPermission(uri: android.net.Uri?, modeFlags: Int) = 0
    override fun checkCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int) = 0
    override fun checkUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int) = 0
    override fun enforceUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int, message: String?) {}
    override fun enforceCallingUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) {}
    override fun enforceCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) {}
    override fun enforceUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int, message: String?) {}

    override fun createPackageContext(packageName: String?, flags: Int) = throw NotImplementedError()
    override fun createConfigurationContext(overrideConfiguration: android.content.res.Configuration) = throw NotImplementedError()
    override fun createDisplayContext(display: android.view.Display) = throw NotImplementedError()
    override fun isRestricted() = false
    override fun getDisplay() = throw NotImplementedError()
    override fun createDeviceProtectedStorageContext() = throw NotImplementedError()
    override fun isDeviceProtectedStorage() = false
    override fun deleteSharedPreferences(name: String?) = false

    // Newer Android API methods that might be required
    // Removed the problematic checkUriPermission with callerToken as it seems to not match or not be needed based on previous error
    override fun getDatabasePath(name: String?) = throw NotImplementedError()
    override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?) = throw NotImplementedError()
    override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?) = throw NotImplementedError()
    override fun deleteDatabase(name: String?) = false
    override fun databaseList() = throw NotImplementedError()
    override fun moveDatabaseFrom(sourceContext: Context?, name: String?) = false
    override fun moveSharedPreferencesFrom(sourceContext: Context?, name: String?) = false

    override fun startForegroundService(service: android.content.Intent?): android.content.ComponentName? = null
    override fun createContextForSplit(splitName: String?): android.content.Context? = null
}
