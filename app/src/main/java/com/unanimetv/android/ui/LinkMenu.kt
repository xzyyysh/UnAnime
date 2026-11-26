package com.unanimetv.android.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.unanimetv.android.R

@Composable
fun LinkMenu(
    url: String,
    modifier: Modifier = Modifier,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onReload: (() -> Unit)? = null,
    reloadLabel: String = "Recargar episodio"
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_tabler_dots_vertical),
                contentDescription = "Más opciones",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium
        ) {
            if (onReload != null) {
                DropdownMenuItem(
                    text = { Text(reloadLabel) },
                    onClick = {
                        expanded = false
                        onReload()
                    }
                )
            }
            if (isFavorite != null && onToggleFavorite != null) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos") },
                    onClick = {
                        expanded = false
                        onToggleFavorite()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Abrir en el navegador") },
                onClick = {
                    expanded = false
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Copiar enlace") },
                onClick = {
                    expanded = false
                    clipboard.setText(AnnotatedString(url))
                    Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
