package com.example.agroX.postActivity


import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun deletePost(post: Post, context: Context, onDelete: (Post) -> Unit) {

    val storage = FirebaseStorage.getInstance()

    // Check if post ID is valid
    if (post.id.isEmpty()) {
        Log.e("DeletePost", "Invalid post ID. Cannot delete.")
        Toast.makeText(context, "Error: Post ID is missing", Toast.LENGTH_SHORT).show()
        return
    }

    // Check if image URL is valid before trying to delete
    if (post.imageUrl.isNotEmpty() && post.imageUrl.startsWith("https://")) {
        val imageRef = storage.getReferenceFromUrl(post.imageUrl)

        imageRef.delete()
            .addOnSuccessListener {
                Log.d("DeletePost", "Image deleted successfully.")
                deletePostFromFirestore(post, context, onDelete)
            }
            .addOnFailureListener { e ->
                Log.e("DeletePost", "Error deleting image: ${e.message}", e)
                Toast.makeText(context, "Error deleting image: ${e.message}", Toast.LENGTH_LONG).show()

                // Still try to delete the post from Firestore
                deletePostFromFirestore(post, context, onDelete)
            }
    } else {
        Log.e("DeletePost", "Invalid image URL: '${post.imageUrl}'")
        deletePostFromFirestore(post, context, onDelete)
    }
}


private fun deletePostFromFirestore(post: Post, context: Context, onDelete: (Post) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val postRef = db.collection("posts").document(post.id)

    // Check if post exists before trying to delete
    postRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                postRef.delete()
                    .addOnSuccessListener {
                        Log.d("DeletePost", "Post deleted successfully.")
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                        onDelete(post)
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeletePost", "Error deleting post: ${e.message}", e)
                        Toast.makeText(context, "Error deleting post: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Log.e("DeletePost", "Post does not exist in Firestore.")
                Toast.makeText(context, "Post already deleted or not found", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Log.e("DeletePost", "Error fetching post: ${e.message}", e)
            Toast.makeText(context, "Error finding post: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

suspend fun updatePost(
    postId: String,
    newTitle: String,
    newDescription: String,
    newImageUri: Uri?,
    newWidth: Int,
    newHeight: Int,
    oldImageUrl: String, // ✅ Old image URL
    context: Context,
    onComplete: (Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val postRef = db.collection("posts").document(postId)

    try {
        var imageUrl = oldImageUrl // Default to old image

        // 🔹 If new image is selected, upload it
        if (newImageUri != null) {
            val storageRef = storage.reference.child("posts/${System.currentTimeMillis()}.jpg")
            val compressedImage = compressImage(context, newImageUri, 50) // ✅ Compress image

            val uploadTask = storageRef.putBytes(compressedImage).await()
            imageUrl = storageRef.downloadUrl.await().toString()

            // 🔹 Delete old image only after new one is uploaded successfully
            if (oldImageUrl.isNotEmpty()) {
                val oldImageRef = storage.getReferenceFromUrl(oldImageUrl)
                oldImageRef.delete().addOnFailureListener { e ->
                    Log.e("UpdatePost", "Failed to delete old image: ${e.message}")
                }
            }
        }

        // 🔹 Update Firestore post entry
        postRef.update(
            mapOf(
                "title" to newTitle,
                "description" to newDescription,
                "imageUrl" to imageUrl,
                "imageWidth" to newWidth,
                "imageHeight" to newHeight
            )
        ).await()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Post updated!", Toast.LENGTH_SHORT).show()
            onComplete(true)
        }

    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("UpdatePost", "Error: ${e.message}")
            onComplete(false)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    onUpdatePost: (String, String, Uri?, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf(post.title) }
    var description by remember { mutableStateOf(post.description) }
    var imageUri by remember { mutableStateOf<Uri?>(null) } // New Image URI
    var imageWidth by remember { mutableStateOf(post.imageWidth) }
    var imageHeight by remember { mutableStateOf(post.imageHeight) }
    var isUploading by remember { mutableStateOf(false) } // ✅ Track upload status

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            imageWidth = bitmap.width
            imageHeight = bitmap.height
        }
    }

    ModalBottomSheet(onDismissRequest = { if (!isUploading) onDismiss() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Edit Post", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
                )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Image Selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { if (!isUploading) launcher.launch("image/*") }
                    .border(1.dp, Color.Gray)
                    .then(
                        if (imageUri != null) Modifier.wrapContentHeight() else Modifier.height(
                            200.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Current Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Indicator
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Uploading...", textAlign = TextAlign.Center, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { if (!isUploading) onDismiss() }) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    isUploading = true
                    onUpdatePost(title, description, imageUri, imageWidth, imageHeight)
                }) {
                    Text("Update")
                }
            }
        }
    }
}
