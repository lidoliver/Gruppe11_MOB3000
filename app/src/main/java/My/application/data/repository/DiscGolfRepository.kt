package my.application.data.repository

import My.application.data.remote.DiscGolfApi
import My.application.data.remote.dto.CourseDto

class DiscGolfRepository {
    private val api = DiscGolfApi.service

    suspend fun getAllCourses(): Result<List<CourseDto>> {
        return try {
            val courses = api.getAllCourses()
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseById(courseId: String): Result<CourseDto> {
        return try {
            val course = api.getCourseById(courseId)
            Result.success(course)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
