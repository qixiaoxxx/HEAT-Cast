package com.mcast.heat.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Keep
@Serializable
data class BaseResponse<T>(
    val code: Int,
    val message: String? = "",
    val data: T
)

@Keep
@Serializable
data class UpdateResponse(
    @SerializedName("package")
    val updateResponsePackage: String? = "",
//    val mkbrokers: List<Any>?,
    val sc: Sc?,
    val description: String? = "",
    val incompatibleVersion: Int? = 0,
    val name: String? = "",
    val code: Int? = 0,
    val info: String = "",
    val release: Release?
)

@Keep
@Serializable
data class Sc(
    val sysId: Int? = 0,
    val xDomain: String? = ""
)

@Keep
@Serializable
data class Release(
    val versionName: String? = "",
    val versionCode: Int? = 0,
    val changeLog: String? = "",
    val targetSdkVersion: Int? = 0,
    val minSdkVersion: Int? = 0,
    val updateDate: String? = "",
    val channel: String? = "",
    val url: String? = "",
    val md5: String? = "",
    val size: Int? = 0
)
