package com.example.agroX.postActivity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import com.example.agroX.presentation.sign_in.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


@RequiresApi(Build.VERSION_CODES.P)
suspend fun uploadImageToFirebase(
    uri: Uri,
    context: Context,
    title: String,
    description: String,
    navController: NavController,
    onProgress: (Float) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
        return
    }

    // ✅ Now correctly fetching Firestore user data
    val userData = getCurrentUserData()
    val userName = userData?.userName
    val profilePictureUrl = userData?.profilePictureUrl ?: ""

    val storageRef = FirebaseStorage.getInstance().reference.child("posts/${System.currentTimeMillis()}.jpg")

    try {
        // ✅ Get Image Dimensions
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        // ✅ Compress Image
        val compressedImage = compressImage(context, uri, 50)
        val uploadTask = storageRef.putBytes(compressedImage)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount.toFloat()
            onProgress(progress)
        }

        // ✅ Await the completion of the upload task
        uploadTask.await()

        // ✅ Get the download URL
        val imageUrl = storageRef.downloadUrl.await()

        val post = hashMapOf(
            "userId" to user.uid,
            "userName" to userName,
            "profilePictureUrl" to profilePictureUrl,
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl.toString(),
            "imageWidth" to imageWidth,
            "imageHeight" to imageHeight,
            "timestamp" to System.currentTimeMillis(),
            "likes" to 0,
            "likedUsers" to emptyList<String>()
        )

        FirebaseFirestore.getInstance().collection("posts").add(post).await()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Post uploaded!", Toast.LENGTH_SHORT).show()
            navController.navigate("home")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}


suspend fun getCurrentUserData(): UserData? {
    val user = FirebaseAuth.getInstance().currentUser ?: return null
    val firestore = FirebaseFirestore.getInstance()

    return try {
        val document = firestore.collection("users").document(user.uid).get().await()
        document.toObject(UserData::class.java)?.copy(
            fullName = user.displayName ?: "Unknown User",
            profilePictureUrl = user.photoUrl?.toString()
        )
    } catch (e: Exception) {
        null
    }
}


    fun compressImage(context: Context, uri: Uri, quality: Int): ByteArray {
    // Open input stream for EXIF metadata
    val inputStream1 = context.contentResolver.openInputStream(uri)!!
    val exif = ExifInterface(inputStream1)
    inputStream1.close() // Close after reading EXIF

    // Open a new input stream for bitmap decoding
    val inputStream2 = context.contentResolver.openInputStream(uri)!!
    val bitmap = BitmapFactory.decodeStream(inputStream2)
    inputStream2.close() // Close after decoding

    // Determine the correct rotation
    val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    // Rotate bitmap if needed
    val rotatedBitmap = if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }

    // Compress the correctly rotated bitmap
    val outputStream = ByteArrayOutputStream()
    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

    return outputStream.toByteArray()
}


@RequiresApi(Build.VERSION_CODES.P)
fun getCorrectedBitmap(context: Context, uri: Uri): Bitmap {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    val inputStream = context.contentResolver.openInputStream(uri)
    val exif = inputStream?.let { ExifInterface(it) }
    val rotation = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
    inputStream?.close()

    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
