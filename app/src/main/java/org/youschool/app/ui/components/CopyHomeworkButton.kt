package org.youschool.app.ui.components

import android.os.Build
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

@Composable
fun CopyHomeworkButton(text: String) {
    if (text.isBlank()) return

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    IconButton(onClick = {
        clipboard.setText(AnnotatedString(text))
        // Android 13+ shows its own clipboard confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Текст ДЗ скопирован", Toast.LENGTH_SHORT).show()
        }
    }) {
        Icon(Icons.Rounded.ContentCopy, contentDescription = "Скопировать текст ДЗ")
    }
}
