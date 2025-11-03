package my.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// @JsonClass(generateAdapter = true) tells Moshi to create a JSON adapter for this class
@JsonClass(generateAdapter = true)
data class CourseDto(
    // @Json(name = "...") maps the JSON key to your Kotlin property name
    @Json(name = "course_id")
    val courseId: String,

    @Json(name = "course_name")
    val courseName: String,

    @Json(name = "hole_count")
    val holeCount: Int,

    // If the JSON key and property name are identical, you don't need the @Json annotation
    val city: String,

    @Json(name = "is_pro_level")
    val isProLevel: Boolean
)
