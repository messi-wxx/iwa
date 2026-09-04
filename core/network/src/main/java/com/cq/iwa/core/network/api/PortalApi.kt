package com.cq.iwa.core.network.api

import com.cq.iwa.core.network.model.CaptchaDto
import com.cq.iwa.core.network.model.LoginRequest
import com.cq.iwa.core.network.model.LoginUserDto
import com.cq.iwa.core.network.model.UserConfigDto
import com.cq.iwa.core.network.model.ResetPasswordRequest
import com.cq.iwa.core.network.model.VerificationRequest
import com.cq.iwa.core.network.model.VerificationResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Portal 认证相关接口（服务端直接返回业务 JSON，无统一包装类）
 */
interface PortalApi {

    @POST("portal/v1/Login?moduleCode=app")
    suspend fun login(@Body request: LoginRequest): Response<LoginUserDto>

    @GET("portal/v1/Captcha")
    suspend fun getCaptcha(): Response<CaptchaDto>

    @GET("portal/v1/security/Config/APPConfig")
    suspend fun getAppConfig(): Response<List<UserConfigDto>>

    @POST("portal/v1/verification")
    suspend fun refreshToken(@Body request: VerificationRequest): Response<VerificationResultDto>

    @POST("portal/v1/verification/token")
    suspend fun refreshTokenSimple(@Body request: VerificationRequest): Response<VerificationResultDto>

    @POST("portal/v1/security/Account")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>
}
