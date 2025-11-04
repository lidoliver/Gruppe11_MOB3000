package my.application.ui.screen.myprofile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.application.data.remote.dto.ScorecardDto
import my.application.data.remote.dto.UserDto
import my.application.data.repository.UserRepository

class MyProfileViewModel(
    private val userRepository: UserRepository = UserRepository.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(MyProfileUiState())
        private set

    fun loadUserProfile(email: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val userResult = userRepository.getUser(email)
            val scorecardsResult = userRepository.getScorecards(email)

            uiState = when {
                userResult.isSuccess -> {
                    val user = userResult.getOrNull()
                    val scorecards = scorecardsResult.getOrNull() ?: emptyList()
                    uiState.copy(
                        user = user,
                        scorecards = scorecards,
                        isLoading = false,
                        error = null
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = userResult.exceptionOrNull()?.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    fun updateProfile(email: String, updatedUser: UserDto) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = userRepository.updateUser(email, updatedUser)

            uiState = when {
                result.isSuccess -> {
                    uiState.copy(
                        user = result.getOrNull(),
                        isLoading = false,
                        error = null
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to update profile"
                    )
                }
            }
        }
    }
}

data class MyProfileUiState(
    val user: UserDto? = null,
    val scorecards: List<ScorecardDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
