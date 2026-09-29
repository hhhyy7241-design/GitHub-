package com.example.data.moodle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.security.SecurityShield
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class MoodleUploadResult(
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val itemId: String,
    val isRemoteSynced: Boolean
)

data class EvidenceUploadResult(
    val evidenceId: String,
    val url: String,
    val fileName: String,
    val fileSize: Long
)

data class EvidenceItem(
    val id: String,
    val name: String,
    val description: String = "",
    val files: List<String> = emptyList()
)

/**
 * Network client for remote file transfer, session management,
 * and JSON messaging sync with progress reporting.
 */
class UcfMoodleClient(
    private var host: String = SecurityShield.SECURE_HOST,
    private var repoId: Int = 4
) {
    companion object {
        const val TAG = "ChatProNetworkClient"
        const val MAX_FILE_SIZE_BYTES = 4 * 1024 * 1024L // 4MB maximum allowed
    }

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val key = url.host
            val currentList = cookieStore.getOrPut(key) { mutableListOf() }
            for (cookie in cookies) {
                currentList.removeAll { it.name == cookie.name }
                currentList.add(cookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val httpClient: OkHttpClient = SecurityShield.applyAntiSniffing(
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
    ).build()

    private var sesskey: String? = null
    private var isSessionActive = false
    private var activeUsername: String? = SecurityShield.SECURE_DEFAULT_USER
    private var activePassword: String? = SecurityShield.SECURE_DEFAULT_PASS
    private var activeToken: String? = null
    private var userId: String = "4"

    fun configure(moodleHost: String, repositoryId: Int) {
        this.host = if (moodleHost.endsWith("/")) moodleHost else "$moodleHost/"
        this.repoId = repositoryId
    }

    suspend fun login(user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        if (user.isBlank() || pass.isBlank()) return@withContext false
        activeUsername = user
        activePassword = pass

        try {
            // 1. Fetch login page to retrieve logintoken
            val loginPageUrl = "${host}login/index.php"
            val getReq = Request.Builder()
                .url(loginPageUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val getResp = httpClient.newCall(getReq).execute()
            val getHtml = getResp.body?.string() ?: ""

            val pattern = Pattern.compile("name=[\"']logintoken[\"']\\s+value=[\"']([^\"']+)[\"']")
            val matcher = pattern.matcher(getHtml)
            val loginToken = if (matcher.find()) matcher.group(1) else ""

            // 2. Post login credentials
            val formBuilder = FormBody.Builder()
                .add("username", user)
                .add("password", pass)

            if (!loginToken.isNullOrEmpty()) {
                formBuilder.add("logintoken", loginToken)
            }

            val postReq = Request.Builder()
                .url(loginPageUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .post(formBuilder.build())
                .build()

            val postResp = httpClient.newCall(postReq).execute()
            val postHtml = postResp.body?.string() ?: ""

            // 3. Extract sesskey from dashboard or home page
            val sessPattern = Pattern.compile("[\"']sesskey[\"']\\s*:\\s*[\"']([^\"']+)[\"']|sesskey=([a-zA-Z0-9]+)")
            val sessMatcher = sessPattern.matcher(postHtml)
            if (sessMatcher.find()) {
                sesskey = sessMatcher.group(1) ?: sessMatcher.group(2)
            }

            // Extract userId
            val userPattern = Pattern.compile("user/profile\\.php\\?id=([0-9]+)|data-userid=[\"']([0-9]+)[\"']")
            val userMatcher = userPattern.matcher(postHtml)
            if (userMatcher.find()) {
                userId = userMatcher.group(1) ?: userMatcher.group(2) ?: "4"
            }

            isSessionActive = sesskey != null
            isSessionActive
        } catch (e: Exception) {
            Log.e(TAG, "Moodle login error: ${e.message}")
            false
        }
    }

    private suspend fun ensureAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        if (isSessionActive && sesskey != null) return@withContext true
        val u = activeUsername
        val p = activePassword
        if (!u.isNullOrEmpty() && !p.isNullOrEmpty()) {
            return@withContext login(u, p)
        }
        false
    }

    /**
     * Automatic image compression to guarantee <= 4MB size before uploading.
     */
    fun compressImageIfNeeded(bytes: ByteArray): ByteArray {
        if (bytes.size <= MAX_FILE_SIZE_BYTES) return bytes
        try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            var quality = 90
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            // Reduce quality until strictly under 3.8 MB
            while (outputStream.size() > (3.8 * 1024 * 1024) && quality > 20) {
                outputStream.reset()
                quality -= 15
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            if (outputStream.size() <= MAX_FILE_SIZE_BYTES) {
                return outputStream.toByteArray()
            }

            // Downscale dimensions if still over 4MB
            val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
            outputStream.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            return outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Image compression error: ${e.message}")
            return bytes
        }
    }

    /**
     * Uploads file to remote server with real progress reporting.
     * Enforces the size limit strictly.
     * DOES NOT use fake network simulation on error.
     */
    suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String = "application/octet-stream",
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<MoodleUploadResult> = withContext(Dispatchers.IO) {
        // Auto compress if image
        val finalBytes = if (mimeType.startsWith("image")) {
            compressImageIfNeeded(fileBytes)
        } else {
            fileBytes
        }

        val totalSize = finalBytes.size.toLong()
        if (totalSize > MAX_FILE_SIZE_BYTES) {
            val sizeMb = String.format("%.2f", totalSize / (1024.0 * 1024.0))
            return@withContext Result.failure(
                IllegalArgumentException("El archivo excede el límite permitido de 4.0 MB (pesa $sizeMb MB).")
            )
        }

        ensureAuthenticated()

        val currentSesskey = sesskey
        if (currentSesskey == null) {
            return@withContext Result.failure(
                IOException("No hay sesión activa en Moodle. Por favor configura o verifica tu usuario en el panel.")
            )
        }

        try {
            val itemPostId = (System.currentTimeMillis() % 10000000).toString()
            val uploadUrl = "${host}repository/repository_ajax.php?action=upload"
            val mediaType = mimeType.toMediaTypeOrNull()

            val countingBody = CountingRequestBody(
                RequestBody.create(mediaType, finalBytes)
            ) { bytesWritten, contentLength ->
                onProgress(bytesWritten, contentLength)
            }

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", fileName)
                .addFormDataPart("author", activeUsername ?: "Nexus User")
                .addFormDataPart("license", "allrightsreserved")
                .addFormDataPart("itemid", itemPostId)
                .addFormDataPart("repo_id", repoId.toString())
                .addFormDataPart("p", "")
                .addFormDataPart("page", "")
                .addFormDataPart("env", "filemanager")
                .addFormDataPart("sesskey", currentSesskey)
                .addFormDataPart("client_id", "nexus_${UUID.randomUUID().toString().take(6)}")
                .addFormDataPart("maxbytes", MAX_FILE_SIZE_BYTES.toString())
                .addFormDataPart("areamaxbytes", MAX_FILE_SIZE_BYTES.toString())
                .addFormDataPart("ctx_id", "1")
                .addFormDataPart("savepath", "/")
                .addFormDataPart("repo_upload_file", fileName, countingBody)

            val request = Request.Builder()
                .url(uploadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .post(multipartBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            val respText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Error del servidor Moodle: HTTP ${response.code}")
                )
            }

            if (respText.contains("\"error\"") && !respText.contains("\"url\"")) {
                val errorMsg = try {
                    JSONObject(respText).optString("error", "Error al procesar archivo en Moodle")
                } catch (e: Exception) {
                    "Error al subir archivo a Moodle"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            var finalUrl = "${host}draftfile.php/$userId/user/draft/$itemPostId/$fileName"
            var isRealRemote = false

            if (respText.contains("\"url\"")) {
                val jsonObj = JSONObject(respText)
                val rawUrl = jsonObj.optString("url", finalUrl).replace("\\", "")
                finalUrl = if (activeToken != null) {
                    rawUrl.replace("pluginfile.php/", "webservice/pluginfile.php/") + "?token=$activeToken"
                } else {
                    rawUrl
                }
                isRealRemote = true
            }

            // Commit to Moodle "Archivos privados" (/user/files.php)
            // This ensures the uploaded status / media is permanently visible under "Archivos privados" on Moodle!
            try {
                val commitUrl = "${host}user/files.php"
                val commitBody = FormBody.Builder()
                    .add("sesskey", currentSesskey)
                    .add("_qf__user_files_form", "1")
                    .add("files_filemanager", itemPostId)
                    .add("submitbutton", "Guardar cambios")
                    .build()

                val commitReq = Request.Builder()
                    .url(commitUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .post(commitBody)
                    .build()

                val commitResp = httpClient.newCall(commitReq).execute()
                commitResp.close()
            } catch (commitEx: Exception) {
                Log.w(TAG, "Commit to user private files note: ${commitEx.message}")
            }

            Result.success(
                MoodleUploadResult(
                    url = finalUrl,
                    fileName = fileName,
                    fileSize = totalSize,
                    itemId = itemPostId,
                    isRemoteSynced = isRealRemote
                )
            )
        } catch (networkEx: Exception) {
            Log.e(TAG, "Moodle upload network error: ${networkEx.message}")
            Result.failure(networkEx)
        }
    }

    /**
     * Uploads JSON payload for sync.
     */
    suspend fun uploadJsonSync(
        fileName: String,
        jsonString: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        val uploadRes = uploadFile(bytes, fileName, "application/json")
        uploadRes.map { it.url }
    }

    /**
     * Downloads JSON string from remote URL.
     */
    suspend fun fetchJsonFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Respuesta HTTP ${resp.code} al obtener JSON"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkServerConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureAuthenticated()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Uploads file to Moodle Evidence (admin/tool/lp/user_evidence_edit.php)
     * as implemented in venezue95-dev/Et.
     * Returns the evidenceId, fileUrl, and metadata.
     */
    suspend fun uploadToEvidence(
        evidenceName: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String = "application/octet-stream",
        description: String = ""
    ): Result<EvidenceUploadResult> = withContext(Dispatchers.IO) {
        val finalBytes = if (mimeType.startsWith("image")) {
            compressImageIfNeeded(fileBytes)
        } else {
            fileBytes
        }

        val totalSize = finalBytes.size.toLong()
        if (totalSize > MAX_FILE_SIZE_BYTES) {
            return@withContext Result.failure(
                IllegalArgumentException("El archivo excede el límite permitido de 4.0 MB.")
            )
        }

        ensureAuthenticated()
        val currentSesskey = sesskey
            ?: return@withContext Result.failure(IOException("No se pudo autenticar sesión en Moodle."))

        try {
            // 1. Request user_evidence_edit to get form parameters and itemid
            val editUrl = "${host}admin/tool/lp/user_evidence_edit.php?userid=$userId"
            val editReq = Request.Builder()
                .url(editUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val editResp = httpClient.newCall(editReq).execute()
            val editHtml = editResp.body?.string() ?: ""

            val itemPattern = Pattern.compile("itemid=([0-9]+)")
            val itemMatcher = itemPattern.matcher(editHtml)
            val draftItemId = if (itemMatcher.find()) itemMatcher.group(1) else (System.currentTimeMillis() % 10000000).toString()

            val ctxPattern = Pattern.compile("ctx_id=([0-9]+)")
            val ctxMatcher = ctxPattern.matcher(editHtml)
            val ctxId = if (ctxMatcher.find()) ctxMatcher.group(1) else "1"

            val clientPattern = Pattern.compile("client_id=([a-zA-Z0-9_]+)")
            val clientMatcher = clientPattern.matcher(editHtml)
            val clientId = if (clientMatcher.find()) clientMatcher.group(1) else "chatpro_${UUID.randomUUID().toString().take(6)}"

            // 2. Upload file to repository_ajax.php
            val uploadUrl = "${host}repository/repository_ajax.php?action=upload"
            val mediaType = mimeType.toMediaTypeOrNull()
            val fileBody = RequestBody.create(mediaType, finalBytes)

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", fileName)
                .addFormDataPart("author", activeUsername ?: "ChatPro")
                .addFormDataPart("license", "allrightsreserved")
                .addFormDataPart("itemid", draftItemId)
                .addFormDataPart("repo_id", repoId.toString())
                .addFormDataPart("p", "")
                .addFormDataPart("page", "")
                .addFormDataPart("env", "filemanager")
                .addFormDataPart("sesskey", currentSesskey)
                .addFormDataPart("client_id", clientId)
                .addFormDataPart("maxbytes", MAX_FILE_SIZE_BYTES.toString())
                .addFormDataPart("areamaxbytes", "-1")
                .addFormDataPart("ctx_id", ctxId)
                .addFormDataPart("savepath", "/")
                .addFormDataPart("repo_upload_file", fileName, fileBody)

            val uploadReq = Request.Builder()
                .url(uploadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .post(multipartBuilder.build())
                .build()

            val uploadResp = httpClient.newCall(uploadReq).execute()
            val uploadRespText = uploadResp.body?.string() ?: ""

            var fileUrl = "${host}draftfile.php/$userId/user/draft/$draftItemId/$fileName"
            if (uploadRespText.contains("\"url\"")) {
                val jsonObj = JSONObject(uploadRespText)
                fileUrl = jsonObj.optString("url", fileUrl).replace("\\", "")
            }

            // 3. Save evidence form with the draft itemid so it's permanently committed to Evidence
            val saveEvidenceUrl = "${host}admin/tool/lp/user_evidence_edit.php?id=&userid=$userId&return="
            val savePayload = FormBody.Builder()
                .add("userid", userId)
                .add("sesskey", currentSesskey)
                .add("_qf__tool_lp_form_user_evidence", "1")
                .add("name", evidenceName)
                .add("description[text]", description)
                .add("description[format]", "1")
                .add("url", "")
                .add("files", draftItemId)
                .add("submitbutton", "Guardar cambios")
                .build()

            val saveReq = Request.Builder()
                .url(saveEvidenceUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .post(savePayload)
                .build()

            val saveResp = httpClient.newCall(saveReq).execute()
            val finalRedirectUrl = saveResp.request.url.toString()
            val savedHtml = saveResp.body?.string() ?: ""

            val evidPattern = Pattern.compile("[?&]id=([0-9]+)")
            val evidMatcher = evidPattern.matcher(finalRedirectUrl)
            var evidenceId = if (evidMatcher.find()) evidMatcher.group(1) else ""

            if (evidenceId.isEmpty()) {
                val pagePattern = Pattern.compile("user_evidence_edit\\.php\\?id=([0-9]+)")
                val pageMatcher = pagePattern.matcher(savedHtml)
                if (pageMatcher.find()) {
                    evidenceId = pageMatcher.group(1)
                }
            }

            if (evidenceId.isEmpty()) {
                evidenceId = draftItemId
            }

            Result.success(
                EvidenceUploadResult(
                    evidenceId = evidenceId,
                    url = fileUrl,
                    fileName = fileName,
                    fileSize = totalSize
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "uploadToEvidence error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Deletes evidence from Moodle (admin/tool/lp) using core_competency_delete_user_evidence.
     */
    suspend fun deleteEvidence(evidenceId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (evidenceId.isBlank()) return@withContext Result.success(true)
        ensureAuthenticated()
        val currentSesskey = sesskey
            ?: return@withContext Result.failure(IOException("No hay sesión activa para eliminar evidencia."))

        try {
            val deleteUrl = "${host}lib/ajax/service.php?sesskey=$currentSesskey&info=core_competency_delete_user_evidence,tool_lp_data_for_user_evidence_list_page"
            val idNum = evidenceId.toIntOrNull() ?: 0

            val jsonArray = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("index", 0)
                    put("methodname", "core_competency_delete_user_evidence")
                    put("args", JSONObject().apply { put("id", idNum) })
                })
                put(JSONObject().apply {
                    put("index", 1)
                    put("methodname", "tool_lp_data_for_user_evidence_list_page")
                    put("args", JSONObject().apply { put("userid", userId.toIntOrNull() ?: 4) })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = RequestBody.create(mediaType, jsonArray.toString())
            val req = Request.Builder()
                .url(deleteUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .post(body)
                .build()

            val resp = httpClient.newCall(req).execute()
            resp.close()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "deleteEvidence error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Gets all evidences listed in Moodle for the user.
     */
    suspend fun getEvidences(): Result<List<EvidenceItem>> = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        try {
            val listUrl = "${host}admin/tool/lp/user_evidence_list.php?userid=$userId"
            val req = Request.Builder()
                .url(listUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val resp = httpClient.newCall(req).execute()
            val html = resp.body?.string() ?: ""

            val items = mutableListOf<EvidenceItem>()
            val nodePattern = Pattern.compile("user_evidence_edit\\.php\\?id=([0-9]+)[^>]*>([^<]+)</a>")
            val matcher = nodePattern.matcher(html)
            while (matcher.find()) {
                val evId = matcher.group(1) ?: ""
                val evName = matcher.group(2) ?: ""
                items.add(EvidenceItem(id = evId, name = evName.trim()))
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Request body wrapper that reports actual upload progress.
 */
class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength(): Long {
        return try {
            delegate.contentLength()
        } catch (e: IOException) {
            -1L
        }
    }

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, contentLength(), onProgress)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private class CountingSink(
        delegate: okio.Sink,
        private val totalBytes: Long,
        private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : ForwardingSink(delegate) {
        private var bytesWritten = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            onProgress(bytesWritten, totalBytes)
        }
    }
}
