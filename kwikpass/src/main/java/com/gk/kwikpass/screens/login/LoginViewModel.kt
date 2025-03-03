import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun validatePhone(phone: String): Boolean {
        return if (phone.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errors = mapOf("phone" to "Phone number is required")
            )
            false
        } else if (!phone.matches(Regex("^[0-9]{10}$"))) {
            _uiState.value = _uiState.value.copy(
                errors = mapOf("phone" to "Enter a valid phone number")
            )
            false
        } else {
            _uiState.value = _uiState.value.copy(errors = emptyMap())
            true
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) 