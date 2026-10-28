package ru.skinselector.app

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object CommunityDragonImageCache {
    private const val BASE = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default"
    private const val SUMMARY_URL = "$BASE/v1/champion-summary.json"

    class DownloadException(val url: String, message: String, cause: Throwable? = null) : IOException(message, cause)

    fun fileFor(context: Context, logicalPath: String): File =
        File(context.filesDir, "communitydragon/images/$logicalPath")

    suspend fun downloadChampion(
        context: Context,
        champion: Champion,
        includeSplashes: Boolean,
        onProgress: (Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Result.success(downloadChampionInternal(context, champion, includeSplashes, onProgress))
        } catch (error: DownloadException) {
            Result.failure(error)
        } catch (error: Throwable) {
            Result.failure(DownloadException(SUMMARY_URL, error.message ?: "Download failed", error))
        }
    }

    private fun downloadChampionInternal(
        context: Context,
        champion: Champion,
        includeSplashes: Boolean,
        onProgress: (Int) -> Unit
    ): Int {
        val championList = getJson(SUMMARY_URL) as JSONArray
        val record = (0 until championList.length())
            .map { championList.getJSONObject(it) }
            .firstOrNull {
                it.optString("alias").equals(champion.id, true) ||
                    it.optString("name").equals(champion.displayName(AppLanguage.ENGLISH), true)
            } ?: throw DownloadException(SUMMARY_URL, "Champion not found: ${champion.id}")
        val numericId = record.optString("id")
        val detailsUrl = "$BASE/v1/champions/$numericId.json"
        val details = getJson(detailsUrl) as JSONObject
        val skins = details.optJSONArray("skins") ?: JSONArray()
        var downloaded = 0
        val total = champion.skins.size.coerceAtLeast(1)

        champion.skins.forEachIndexed { index, skin ->
            val remote = skins.optJSONObject(index) ?: return@forEachIndexed
            val skinNumber = remote.optInt("id", index + 1) % 1000
            val splashBaseUrl = "https://cdn.communitydragon.org/latest/champion/$numericId/splash-art"
            val skinUrl = "$splashBaseUrl/centered/skin/$skinNumber"
            if (download(skinUrl, fileFor(context, "skins/${champion.id}/${skin.id}.png"))) downloaded++
            if (includeSplashes) {
                download("$splashBaseUrl/skin/$skinNumber", fileFor(context, "skins/${champion.id}/splashes/${skin.id}.jpg"))
            }
            val chromas = remote.optJSONArray("chromas") ?: JSONArray()
            skin.chromas.forEachIndexed { chromaIndex, chroma ->
                val remoteChroma = chromas.optJSONObject(chromaIndex)
                val chromaId = remoteChroma?.optInt("id") ?: 0
                if (chromaId != 0) {
                    val chromaUrl = "$BASE/v1/champion-chroma-images/$numericId/$chromaId.png"
                    if (download(chromaUrl, fileFor(context, "skins/${champion.id}/${skin.id}/${chroma.id}.png"))) downloaded++
                }
            }
            onProgress(((index + 1) * 100 / total).coerceIn(0, 100))
        }
        return downloaded
    }

    fun hasChampionImages(context: Context, champion: Champion): Boolean = champion.skins.any { skin ->
        val skinImage = skin.assetPath?.let { File(context.filesDir, "communitydragon/images/$it") }
            ?: fileFor(context, "skins/${champion.id}/${skin.id}.png")
        skinImage.isFile && BitmapFactory.decodeFile(skinImage.path) != null
    }

    fun clearChampion(context: Context, champion: Champion) {
        File(context.filesDir, "communitydragon/images/skins/${champion.id}").deleteRecursively()
    }

    fun clearExcept(context: Context, preservedChampionIds: Set<String>) {
        val skinsDirectory = File(context.filesDir, "communitydragon/images/skins")
        skinsDirectory.listFiles()?.filter { it.isDirectory && it.name !in preservedChampionIds }?.forEach { it.deleteRecursively() }
    }

    fun clearAll(context: Context) {
        File(context.filesDir, "communitydragon/images").deleteRecursively()
    }

    fun cacheSizeBytes(context: Context): Long {
        val root = File(context.filesDir, "communitydragon/images")
        return root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clear(context: Context) {
        File(context.filesDir, "communitydragon").deleteRecursively()
    }

    private fun getJson(url: String): Any = request(url) { connection ->
        connection.inputStream.bufferedReader().use { reader ->
            val body = reader.readText()
            if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)
        }
    }

    private fun download(url: String, target: File): Boolean {
        if (target.isFile && BitmapFactory.decodeFile(target.path) != null) return true
        target.parentFile?.mkdirs()
        val temporary = File(target.path + ".download")
        return request(url) { connection ->
            connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
            if (BitmapFactory.decodeFile(temporary.path) == null) {
                temporary.delete()
                throw DownloadException(url, "Downloaded file is not a valid image")
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            true
        }
    }

    private fun <T> request(url: String, block: (HttpURLConnection) -> T): T {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw DownloadException(url, "HTTP ${connection.responseCode}")
            }
            return block(connection)
        } catch (error: DownloadException) {
            throw error
        } catch (error: Throwable) {
            throw DownloadException(url, error.message ?: "Request failed", error)
        }
    }
}
