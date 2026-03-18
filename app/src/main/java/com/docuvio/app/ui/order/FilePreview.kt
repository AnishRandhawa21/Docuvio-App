package com.docuvio.app.ui.order

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/* -------------------------------------------------- */
/* ---------------- FILE PREVIEW -------------------- */
/* -------------------------------------------------- */

@Composable
fun FilePreview(
    file: File,
    modifier: Modifier = Modifier
) {
    when (file.extension.lowercase()) {
        "jpg", "jpeg", "png" -> {
            ImagePreview(file, modifier)
        }

        "pdf" -> {
            PdfPreview(file, modifier)
        }

        else -> {
            UnsupportedPreview(file, modifier)
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- IMAGE PREVIEW ------------------- */
/* -------------------------------------------------- */

@Composable
private fun ImagePreview(
    file: File,
    modifier: Modifier
) {
    AsyncImage(
        model = file,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/* -------------------------------------------------- */
/* ---------------- PDF PREVIEW --------------------- */
/* -------------------------------------------------- */

@Composable
private fun PdfPreview(
    file: File,
    modifier: Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file) {
        val renderedBitmap = withContext(Dispatchers.IO) {

            val descriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

            val renderer = PdfRenderer(descriptor)
            val page = renderer.openPage(0)

            val bmp = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )

            page.render(
                bmp,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            page.close()
            renderer.close()
            descriptor.close()

            bmp
        }

        bitmap = renderedBitmap
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

/* -------------------------------------------------- */
/* ---------------- FALLBACK PREVIEW ---------------- */
/* -------------------------------------------------- */

@Composable
private fun UnsupportedPreview(
    file: File,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = file.name,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
