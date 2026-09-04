package com.cq.iwa.feature.auth

import java.util.regex.Pattern

object PasswordValidator {

    fun isValid(password: String): Boolean {
        if (password.length < 6) return false
        val patterns = listOf(
            "(?=.*[a-z])(?=.*[0-9])(?=.*[\\W_])^.*$",
            "(?=.*[A-Z])(?=.*[0-9])(?=.*[\\W_])^.*$",
            "(?=.*[A-Z])(?=.*[a-z])(?=.*[\\W_])^.*$",
            "(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])^.*$",
        )
        return patterns.any { Pattern.compile(it).matcher(password).find() }
    }
}
