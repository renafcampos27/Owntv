package tv.own.owntv.features.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import tv.own.owntv.core.database.entity.ProfileEntity

private val Context.automaticBackupStore by preferencesDataStore(name = "owntv_automatic_backup")

internal data class AutomaticBackupState(
    val months: Int = AutomaticBackupPolicy.DEFAULT_MONTHS,
    val configured: Boolean = false,
    val anchor: Long = 0,
    val lastSuccess: Long = 0,
    val failed: Boolean = false,
) { val nextAt: Long? get() = if (configured) AutomaticBackupPolicy.nextAt(months, anchor) else null }

internal data class AutomaticBackupConfig(val folder: String, val password: String,
    val profiles: Map<Long, String>, val revision: String)

internal object AutomaticBackupPreferences {
    private val interval = intPreferencesKey("interval_months")
    private val anchor = longPreferencesKey("anchor")
    private val lastSuccess = longPreferencesKey("last_success")
    private val failed = booleanPreferencesKey("failed")
    private val sealed = stringPreferencesKey("sealed_configuration")
    private val revision = stringPreferencesKey("revision")
    private const val KEY_ALIAS = "owntv_automatic_backup_key"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val CIPHER = "AES/GCM/NoPadding"

    private fun state(p: Preferences) = AutomaticBackupState(
        months = p[interval]?.takeIf(AutomaticBackupPolicy::valid) ?: AutomaticBackupPolicy.DEFAULT_MONTHS,
        configured = !p[sealed].isNullOrBlank(), anchor = p[anchor] ?: 0L,
        lastSuccess = p[lastSuccess] ?: 0L, failed = p[failed] ?: false)
    fun observe(context: Context) = context.applicationContext.automaticBackupStore.data.map(::state)
    suspend fun read(context: Context) = state(context.applicationContext.automaticBackupStore.data.first())

    /** A changed PIN or parental state revokes the unattended authorization. */
    fun authorization(profile: ProfileEntity): String = MessageDigest.getInstance("SHA-256")
        .digest((profile.id.toString() + ":" + profile.createdAt + ":" + profile.pinHash.orEmpty() + ":" + profile.isKids).toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> byte.toUByte().toString(16).padStart(2, '0') }

    suspend fun setInterval(context: Context, months: Int) {
        require(AutomaticBackupPolicy.valid(months))
        context.applicationContext.automaticBackupStore.edit { it[interval] = months }
        AutomaticBackupScheduler.update(context)
    }

    suspend fun configure(context: Context, folder: String, password: String, profiles: List<ProfileEntity>) {
        require(password.isNotBlank() && profiles.isNotEmpty() && folder.isNotBlank())
        val config = JSONObject().put("folder", folder).put("password", password)
            .put("profiles", JSONObject().apply { profiles.forEach { put(it.id.toString(), authorization(it)) } })
        val encrypted = encrypt(config.toString())
        context.applicationContext.automaticBackupStore.edit {
            it[sealed] = encrypted
            it[revision] = UUID.randomUUID().toString()
            it[anchor] = System.currentTimeMillis()
            it[failed] = false
        }
        AutomaticBackupScheduler.update(context)
    }

    suspend fun config(context: Context): AutomaticBackupConfig? {
        val p = context.applicationContext.automaticBackupStore.data.first()
        val raw = p[sealed] ?: return null
        val o = JSONObject(decrypt(raw))
        val profiles = o.getJSONObject("profiles")
        return AutomaticBackupConfig(o.getString("folder"), o.getString("password"),
            profiles.keys().asSequence().associate { it.toLong() to profiles.getString(it) }, p[revision].orEmpty())
    }
    suspend fun isCurrent(context: Context, expected: String): Boolean =
        context.applicationContext.automaticBackupStore.data.first()[revision] == expected
    suspend fun success(context: Context, expected: String, now: Long) {
        context.applicationContext.automaticBackupStore.edit {
            if (it[revision] == expected) { it[lastSuccess] = now; it[anchor] = now; it[failed] = false }
        }
    }
    suspend fun failure(context: Context) { context.applicationContext.automaticBackupStore.edit { it[failed] = true } }

    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(CIPHER).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance(CIPHER).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        }
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }
}
