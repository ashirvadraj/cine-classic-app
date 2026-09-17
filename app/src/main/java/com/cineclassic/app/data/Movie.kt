package com.cineclassic.app.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Movie(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int,
    @SerializedName("language") val language: String,
    @SerializedName("genre") val genre: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("director") val director: String,
    @SerializedName("cast") val cast: List<String>,
    @SerializedName("synopsis") val synopsis: String,
    @SerializedName("rating") val rating: String,
    @SerializedName("posterUrl") val posterUrl: String,
    @SerializedName("backdropUrl") val backdropUrl: String,
    @SerializedName("videoUrl") val videoUrl: String,
    @SerializedName("quality") val quality: String,
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long
) : Serializable {
    val castFormatted: String
        get() = cast.joinToString(", ")

    val displayYear: String
        get() = if (year in 1900..2026) "$year" else ""

    val displayTitleWithYear: String
        get() = if (year in 1900..2026) "$title ($year)" else title
}
