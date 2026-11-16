package org.youschool.app.ui.screens.auth

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.youschool.app.domain.repository.SchoolRepository

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class RequireCaptcha(val username: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class CaptchaJsBridge(private val onToken: (String) -> Unit) {
    @JavascriptInterface
    fun onTokenReceived(token: String) {
        onToken(token)
    }
}

class AuthViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        if (repository.isUserLoggedIn()) {
            _uiState.value = AuthUiState.Success
        }
    }

    fun login(loginText: String, passwordText: String, captchaToken: String? = null) {
        if (org.youschool.app.BuildConfig.DEBUG && loginText.trim().equals("demo", ignoreCase = true)) {
            loginDemo()
            return
        }

        if (loginText.isBlank() || passwordText.isBlank()) {
            _uiState.value = AuthUiState.Error("Введите логин и пароль")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(loginText.trim(), passwordText, captchaToken)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Не удалось войти"
                if (errorMsg.contains("капчу") || errorMsg.contains("код 3")) {
                    _uiState.value = AuthUiState.RequireCaptcha(loginText.trim())
                } else {
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            }
        }
    }

    fun loginDemo() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.loginDemo()
            _uiState.value = AuthUiState.Success
        }
    }

    fun dismissCaptcha() {
        if (_uiState.value is AuthUiState.RequireCaptcha) {
            _uiState.value = AuthUiState.Error("Вход отменен. Требуется подтвердить проверку.")
        }
    }
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    if (uiState is AuthUiState.RequireCaptcha) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCaptcha() },
            title = { Text("Подтвердите, что вы не робот") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                addJavascriptInterface(CaptchaJsBridge { token ->
                                    post {
                                        viewModel.login(username, password, token)
                                    }
                                }, "AndroidInterface")

                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                      <script src="https://www.google.com/recaptcha/api.js" async defer></script>
                                      <script>
                                        function onCaptchaSuccess(token) {
                                          if (window.AndroidInterface) {
                                            window.AndroidInterface.onTokenReceived(token);
                                          }
                                        }
                                      </script>
                                    </head>
                                    <body style="display:flex;justify-content:center;align-items:center;min-height:90vh;margin:0;background:#1e1e1e;">
                                      <div class="g-recaptcha" data-sitekey="6LfXOhYUAAAAAJ40iBnYX47lqtIkd2Wz-hAL9gNM" data-callback="onCaptchaSuccess"></div>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL("https://app.eschool.center", html, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCaptcha() }) {
                    Text("Отмена")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "YouSchool",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Быстрый дневник в стиле Material You",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Логин eSchool") },
                leadingIcon = {
                    Icon(Icons.Rounded.Person, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                leadingIcon = {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть" else "Показать"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.login(username, password)
                })
            )

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login(username, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Войти",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (org.youschool.app.BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.loginDemo()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти в демо-режиме (Debug)")
                }
            }
        }
    }
}
