package tech.nimbus.utils

import java.security.MessageDigest

object EtagUtil {
    fun compute(content: String, updatedAt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((content + "|" + updatedAt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

