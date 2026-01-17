package com.ramphal.uploadfiletestapp

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAnyFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            pickFileLauncher.launch("*/*")
        }
    }

    // 1. Initialize Client with OkHttp and Logging
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            // For large files (100MB+), 60 seconds is NOT enough.
            // We set it to 10 minutes here.
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 600_000
        }

        install(Logging) {
            // Optimization: Use HEADERS.
            // LogLevel.ALL tries to cache the file in memory to log it,
            // which WILL crash your app on large videos.
            level = LogLevel.HEADERS
            logger = Logger.DEFAULT
        }
    }

    private fun uploadAnyFile(fileUri: Uri) {
        val fileName = getFileName(fileUri) ?: "upload_${System.currentTimeMillis()}"
        val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.submitFormWithBinaryData(
                    url = "https://api.escuelajs.co/api/v1/files/upload",
                    formData = formData {
                        append("file", InputProvider {
                            // Keep this exactly as you had it—it's the most stable way
                            contentResolver.openInputStream(fileUri)?.asInput()
                                ?: throw IOException("File not found")
                        }, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            append(HttpHeaders.ContentType, mimeType)
                        })
                    }
                ) {
                    onUpload { sent, total ->
                        // Optimization: Only log every 1% to avoid blocking the main thread
                        // with thousands of log messages during a 1GB upload.
                        if (total != null && total > 0) {
                            val progress = (sent.toFloat() / total) * 100
                            if (sent % (total / 100 + 1) == 0L) {
                                Log.d("PROGRESS", "Uploading $fileName: ${progress.toInt()}%")
                            }
                        }
                    }
                }

                // A 201 status is a SUCCESS.
                if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                    val body = response.bodyAsText()
                    Log.d("RESPONSE", "Upload successful: $body")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Upload Finished!", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("ERROR", "Failed to upload $fileName: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Upload Failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Helper function to get the actual filename from a Uri
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}



    val ktor_version = "3.0.1" // Use 3.0.1 or your specific 3.x version

    // The core engine logic
    implementation("io.ktor:ktor-client-core:$ktor_version")

    // The "Muscles": OkHttp engine (Native Android optimized)
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")

    // The "Eyes": To see the response in Logcat (What you asked for)
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    // The "Bridge": Required for .asInput() to stream from Android URIs
    implementation("io.ktor:ktor-utils:$ktor_version")
