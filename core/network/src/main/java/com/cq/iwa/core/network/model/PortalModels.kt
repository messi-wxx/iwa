package com.cq.iwa.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val code: String,
    val name: String,
    val password: String,
    val answer: String,
    val captcha: String,
    val deviceId: String = "",
)

@Serializable
data class LoginUserDto(
    val id: Int = 0,
    val state: Int = 0,
    val name: String = "",
    val code: String = "",
    val token: String = "",
    val customer: String = "",
    val menu: List<MenuDto>? = null,
)

@Serializable
data class MenuDto(
    val name: String = "",
    val path: String = "",
    val children: List<ChildMenuDto>? = null,
)

@Serializable
data class ChildMenuDto(
    val name: String = "",
    val path: String = "",
)

@Serializable
data class CaptchaDto(
    val image: String = "",
    val answer: String = "",
    val contentType: String = "",
)

@Serializable
data class VerificationRequest(
    val moduleCode: String = "Portal",
    val token: String,
    val customerCode: String = "",
)

@Serializable
data class VerificationResultDto(
    val moduleCode: String = "",
    val token: String = "",
)

@Serializable
data class ResetPasswordRequest(
    val code: String,
    val oldPassword: String,
    val password: String,
    val confirmPassword: String,
)

@Serializable
data class UserConfigDto(
    val id: Int = 0,
    val customerId: String? = null,
    val kind: String? = null,
    val configName: String? = null,
    val configValue: String? = null,
    val seq: Int = 0,
    val description: String? = null,
)
