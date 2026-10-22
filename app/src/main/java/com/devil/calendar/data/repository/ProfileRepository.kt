package com.devil.calendar.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.data.prefs.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
) {
    val profile: Flow<UserProfile> = prefs.profile

    suspend fun setName(value: String) = prefs.setName(value)
    suspend fun setGoal(value: String) = prefs.setGoal(value)

    /** Center-crops [uri] to a square, downscales to 256x256 and saves a JPEG. */
    suspend fun saveAvatar(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeSquare(uri) ?: return@withContext null
        val file = File(context.filesDir, "avatar.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        prefs.setAvatarPath(file.absolutePath)
        file.absolutePath
    }

    suspend fun clearAvatar() {
        File(context.filesDir, "avatar.jpg").delete()
        prefs.setAvatarPath(null)
    }

    private suspend fun decodeSquare(uri: Uri): Bitmap? {
        val src = runCatching {
            context.imageLoader.execute(
                ImageRequest.Builder(context).data(uri).allowHardware(false).build(),
            ).drawable?.toBitmap()
        }.getOrNull() ?: runCatching {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrNull() ?: return null

        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val cropped = Bitmap.createBitmap(src, x, y, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true)
        if (cropped !== src) cropped.recycle()
        if (scaled !== src) src.recycle()
        return scaled
    }
}
