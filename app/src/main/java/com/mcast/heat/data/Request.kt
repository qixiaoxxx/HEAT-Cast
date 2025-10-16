package com.mcast.heat.data

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * .params("package", Config.packageName)
 * .params("channel", Custom.RELEASE_CHANNEL)
 * .params("from", from)
 * .params("appvercode", Config.versionCode)
 * .params("appvername", Config.versionName)
 * .params("sysvercode", Config.sysVerCode)
 * .params("sysvername", Config.sysVersion)
 */

@Keep
@Serializable
data class UpdateRequest(
    @SerialName("package")
    val packageName: String?,
    val channel: String?,
    val from: String?,
    val appvercode: Int?,
    val appvername: String?,
    val sysvercode: String?,
    val sysvername: String?
)