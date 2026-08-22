package com.example.gasguard.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gasguard.domain.model.User
import com.example.gasguard.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.getCurrentUser().collect { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUserLoggedIn = user != null,
                        user = user
                    )
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validateLoginFields(email, password)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null, successMessage = null) }
            val result = authRepository.login(email, password)
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isUserLoggedIn = true,
                        successMessage = "Login successful"
                    ) 
                }
            }.onFailure { e ->
                val errorMsg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email. Please register first."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    else -> "Something went wrong. Please try again."
                }
                _uiState.update { it.copy(isLoading = false, generalError = errorMsg) }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPass: String) {
        if (!validateRegisterFields(name, email, password, confirmPass)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null, successMessage = null) }
            val result = authRepository.register(name, email, password)
            result.onSuccess {
                // Firebase automatically signs in after registration. 
                // We must sign out to satisfy the requirement: Splash -> Login for new users.
                authRepository.logout()
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRegistrationSuccess = true,
                        successMessage = "Registration successful! Please login."
                    ) 
                }
            }.onFailure { e ->
                val errorMsg = when (e) {
                    is FirebaseAuthUserCollisionException -> "This email is already registered. Please login instead."
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    else -> e.message ?: "Something went wrong. Please try again."
                }
                _uiState.update { it.copy(isLoading = false, generalError = errorMsg) }
            }
        }
    }

    private fun validateLoginFields(email: String, password: String): Boolean {
        var isValid = true
        var emailErr: String? = null
        var passErr: String? = null

        if (email.isBlank()) {
            emailErr = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErr = "Please enter a valid email address"
            isValid = false
        }

        if (password.isBlank()) {
            passErr = "Password is required"
            isValid = false
        }

        _uiState.update { it.copy(emailError = emailErr, passwordError = passErr) }
        return isValid
    }

    private fun validateRegisterFields(name: String, email: String, pass: String, confirmPass: String): Boolean {
        var isValid = true
        var nErr: String? = null
        var eErr: String? = null
        var pErr: String? = null
        var cpErr: String? = null

        if (name.isBlank()) {
            nErr = "Name is required"
            isValid = false
        }

        if (email.isBlank()) {
            eErr = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            eErr = "Please enter a valid email address"
            isValid = false
        }

        if (pass.isBlank()) {
            pErr = "Password is required"
            isValid = false
        } else if (pass.length < 6) {
            pErr = "Password must contain at least 6 characters"
            isValid = false
        }

        if (confirmPass.isBlank()) {
            cpErr = "Please confirm your password"
            isValid = false
        } else if (pass != confirmPass) {
            cpErr = "Passwords do not match"
            isValid = false
        }

        _uiState.update { 
            it.copy(
                nameError = nErr, 
                emailError = eErr, 
                passwordError = pErr, 
                confirmPasswordError = cpErr
            ) 
        }
        return isValid
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearState() {
        _uiState.update { 
            it.copy(
                generalError = null, 
                successMessage = null,
                isRegistrationSuccess = false,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            ) 
        }
    }
    
    fun clearErrors() {
        _uiState.update {
            it.copy(
                generalError = null,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isUserLoggedIn: Boolean? = null,
    val user: User? = null,
    val isRegistrationSuccess: Boolean = false,
    val generalError: String? = null,
    val successMessage: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)
