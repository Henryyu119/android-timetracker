package com.timetracker.data.remote

import com.google.gson.annotations.SerializedName

data class SyncRequest(
    @SerializedName("date")
    val date: String,
    @SerializedName("buckets")
    val buckets: Map<String, List<UsageEvent>>
)

data class UsageEvent(
    @SerializedName("id")
    val id: Long,
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("duration")
    val duration: Double,
    @SerializedName("data")
    val data: UsageData
)

data class UsageData(
    @SerializedName("app")
    val app: String,
    @SerializedName("title")
    val title: String
)

data class SyncResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("synced_count")
    val syncedCount: Int? = null
)
