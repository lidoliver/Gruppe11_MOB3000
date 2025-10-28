package my.application.data.remote.api

import my.application.data.remote.dto.CourseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface DiscGolfApiService {

    // This function will make a GET request to "https://api.example.com/v1/courses"
    @GET("courses")
    suspend fun getAllCourses(): List<CourseDto>

    // This function will make a GET request to something like "https://api.example.com/v1/course/dg-123"
    @GET("course/{id}")
    suspend fun getCourseById(
        @Path("id") courseId: String
    ): CourseDto
}
