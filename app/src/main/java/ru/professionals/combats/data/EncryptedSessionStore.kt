package ru.professionals.combats.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import ru.professionals.domain.Session

/** Persists sessions with a non-exportable Android Keystore AES-GCM key. Created: 30-09-2026. Author: participant number pending. */
class EncryptedSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("contest_session_encrypted", Context.MODE_PRIVATE)
    private val alias = "contest.session.aes.v1"

    /** Returns a decrypted session, or clears invalid/restored data when its device key is unavailable. */
    fun load(): Session? {
        val value = preferences.getString("session", null) ?: return null
        return try {
            val parts = value.split(':', limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            val json = JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
            Session(json.getString("id"), json.getString("email"), json.getString("access"), json.getString("refresh"), json.getLong("expires"))
        } catch (error: Exception) {
            Log.e("SessionStore", "[SessionStore]: Ошибка — Не удалось прочитать сессию (${error.javaClass.simpleName})")
            clear()
            null
        }
    }

    /** Stores tokens as one authenticated encrypted record; passwords are never retained. */
    fun save(session: Session) {
        val payload = JSONObject().put("id", session.userId).put("email", session.email)
            .put("access", session.accessToken).put("refresh", session.refreshToken).put("expires", session.expiresAtEpochSeconds)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString("session", value).commit()) { "Не удалось сохранить сессию" }
    }

    /** Removes the encrypted credentials. */
    fun clear() { preferences.edit().clear().commit() }

    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
}
