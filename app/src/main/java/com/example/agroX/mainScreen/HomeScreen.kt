package com.example.agroX.mainScreen


import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.agroX.postActivity.EditPostBottomSheet
import com.example.agroX.postActivity.Post
import com.example.agroX.postActivity.deletePost
import com.example.agroX.postActivity.updatePost
import com.example.agroX.postActivity.uploadImageToFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


@Composable
fun HomeScreen(navController: NavController, onUploadClick: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showOnlyMyPosts by remember { mutableStateOf(false) } // State to toggle between all posts and user's posts

    val listState = rememberLazyListState()
    val isScrollingUp by remember { derivedStateOf { listState.firstVisibleItemScrollOffset < 10 } }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isScrollingUp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!isSearchExpanded) {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    } else {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search posts...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                IconButton(onClick = { isSearchExpanded = false; query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    }
                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("My Posts") },
                                onClick = {
                                    showOnlyMyPosts = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("All Posts") },
                                onClick = {
                                    showOnlyMyPosts = false
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onUploadClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Post")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

        ) {
            PostList(query, listState, showOnlyMyPosts)
        }
    }
}

@Composable
fun PostList(query: String, listState: LazyListState, showOnlyMyPosts: Boolean) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(query, showOnlyMyPosts) {
        val baseQuery = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val finalQuery = if (showOnlyMyPosts && currentUser != null) {
            baseQuery.whereEqualTo("userId", currentUser.uid)
        } else {
            baseQuery
        }

        val listener = finalQuery.addSnapshotListener { snapshots, e ->
            if (e != null) {
                isLoading = false
                return@addSnapshotListener
            }

            val fetchedPosts = snapshots?.documents?.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(
                    id = doc.id,
                    likedUsers = doc["likedUsers"] as? List<String> ?: emptyList()
                )
            }?.filter { it.title.contains(query, ignoreCase = true) } ?: emptyList()

            posts = fetchedPosts
            isLoading = false
        }

        onDispose {
            listener.remove()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (posts.isEmpty()) {
            Text(
                text = "No posts available.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    PostItem(post, onDelete = { deletedPost ->
                        db.collection("posts").document(deletedPost.id).delete()
                    })
                }
            }
        }
    }
}



@Composable
fun PostItem(post: Post, onDelete: (Post) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    var liked by rememberSaveable { mutableStateOf(post.likedUsers.contains(currentUser?.uid)) }
    var likeCount by rememberSaveable { mutableStateOf(post.likes) }


    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    val scale = remember { Animatable(1f) }// ✅ Persist edit state
    var showHeart by remember { mutableStateOf(false) }




    DisposableEffect(post.id) {
        val listener = db.collection("posts").document(post.id)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    liked = (snapshot["likedUsers"] as? List<*>)?.contains(currentUser?.uid) == true
                    likeCount = snapshot.getLong("likes")?.toInt() ?: 0
                }
            }
        onDispose { listener.remove() }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                AsyncImage(
                    model = post.profilePictureUrl,
                    contentDescription = "User Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.width(8.dp))


                Text(
                    text = post.userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )


                if (currentUser?.uid == post.userId) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Post Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit", color = MaterialTheme.typography.bodyMedium.color) },
                                onClick = {
                                    menuExpanded = false
                                    showEditDialog = true // Trigger the edit bottom sheet/dialog
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.typography.bodyMedium.color) },
                                onClick = {
                                    menuExpanded = false
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }


            Text(
                text = post.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 **Post Image**
            if (post.imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (post.imageHeight > 0 && post.imageWidth > 0) {
                                (post.imageHeight * (1f / post.imageWidth) * LocalConfiguration.current.screenWidthDp).dp
                            } else {
                                200.dp
                            }
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    val userId = currentUser?.uid ?: return@detectTapGestures
                                    if (liked) return@detectTapGestures // Ignore if already liked

                                    liked = true
                                    likeCount += 1
                                    showHeart = true  // Show heart on double tap

                                    coroutineScope.launch {
                                        scale.animateTo(1.5f, animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ))
                                        scale.animateTo(1f, animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ))
                                    }

                                    coroutineScope.launch {
                                        delay(500)
                                        showHeart = false
                                    }

                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val postRef = db.collection("posts").document(post.id)
                                            db.runTransaction { transaction ->
                                                val snapshot = transaction.get(postRef)
                                                val likedUsers = snapshot.get("likedUsers") as? List<*> ?: emptyList<String>()
                                                if (!likedUsers.contains(userId)) {
                                                    transaction.update(postRef, "likes", FieldValue.increment(1))
                                                    transaction.update(postRef, "likedUsers", FieldValue.arrayUnion(userId))
                                                }
                                            }.await()
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                liked = false
                                                likeCount -= 1
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.imageUrl)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Post Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showHeart) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Liked",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(scale.value * 48.dp)
                        )

                    }
                }
            }


            Spacer(modifier = Modifier.height(8.dp))


            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Column {
                    val shouldShowMore = post.description.length > 50

                    Text(
                        text = if (expanded) post.description else post.description.take(50) + if (shouldShowMore) "..." else "",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (shouldShowMore) {
                        Text(
                            text = if (expanded) "Show less" else "Show more",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { expanded = !expanded }
                        )
                    }
                }
            }

            // 🔹 **Like Button Section**
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    ,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    var isProcessingLike by rememberSaveable { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            if (isProcessingLike) return@IconButton
                            val userId = currentUser?.uid ?: return@IconButton

                            liked = !liked
                            likeCount = if (liked) likeCount + 1 else likeCount - 1
                            isProcessingLike = true

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val postRef = db.collection("posts").document(post.id)
                                    db.runTransaction { transaction ->
                                        val snapshot = transaction.get(postRef)
                                        val likedUsers = snapshot.get("likedUsers") as? List<*> ?: emptyList<String>()
                                        val hasLiked = likedUsers.contains(userId)

                                        transaction.update(postRef, "likes", if (hasLiked) FieldValue.increment(-1) else FieldValue.increment(1))
                                        transaction.update(postRef, "likedUsers", if (hasLiked) FieldValue.arrayRemove(userId) else FieldValue.arrayUnion(userId))
                                    }.await()
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        liked = !liked
                                        likeCount = if (liked) likeCount + 1 else likeCount - 1
                                    }
                                } finally {
                                    isProcessingLike = false
                                }
                            }
                        },
                        enabled = !isProcessingLike
                    ) {
                        Icon(
                            imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                            contentDescription = "Like Button",
                            tint = if (liked) Color.Red else Color.Gray
                        )
                    }

                    Text(text = "$likeCount Likes", fontSize = 14.sp, color = Color.Gray)
                }

                Column {
                    IconButton(onClick = { /* Handle Share Button Click */ }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Button")
                    }
                }

                IconButton(onClick = { /* Handle Comment Button Click */ }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment Button")
                }
            }
        }
    }

    if(showEditDialog) {
        EditPostBottomSheet(
            post = post,
            onDismiss = { showEditDialog = false },
            onUpdatePost = { newTitle, newDescription, newImageUri, newWidth, newHeight ->
                CoroutineScope(Dispatchers.IO).launch {
                    updatePost(
                        postId = post.id,
                        newTitle = newTitle,
                        newDescription = newDescription,
                        newImageUri = newImageUri,
                        newWidth = newWidth,
                        newHeight = newHeight,
                        oldImageUrl = post.imageUrl,
                        context = context,
                        onComplete = { success ->
                            CoroutineScope(Dispatchers.Main).launch {
                                if (success) {
                                    Log.d("EditPost", "Post updated successfully")
                                    showEditDialog = false // ✅ Close after successful update
                                } else {
                                    Log.e("EditPost", "Failed to update post")
                                }
                            }
                        }
                    )
                }
            }
        )


    }


    // 🔹 **Delete Confirmation Dialog**
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    deletePost(post, context, onDelete)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadBottomSheet(
    navController: NavController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    val focusManager = LocalFocusManager.current
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }



    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    val painter = rememberAsyncImagePainter(imageUri)
    val painterState = painter.state

    // Dynamically get the image size after it's loaded
    val imageSize = remember { mutableStateOf(DpSize(200.dp, 200.dp)) } // Default size for visibility

    LaunchedEffect(painterState) {
        if (painterState is AsyncImagePainter.State.Success) {
            val size = painterState.painter.intrinsicSize
            if (size.width > 0 && size.height > 0) {
                imageSize.value = DpSize(size.width.dp, size.height.dp)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Make it scrollable
                .padding(16.dp)

        ) {
            Column {
                Text(
                    text = "Upload Post",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Enter title...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester1),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequester2.requestFocus() }
                    )
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Enter description...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester2),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Image Selection Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (imageUri != null) Modifier.wrapContentHeight() else Modifier.height(
                                200.dp
                            )
                        )
                        .clickable { if (!isUploading) launcher.launch("image/*") }
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.outline),
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
                        Text("Tap to select an image", color = Color.Gray)
                    }
                }



                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val currentUploadingState by rememberUpdatedState(isUploading)

                    Button(
                        onClick = {
                            if (!currentUploadingState) { // Ensures instant UI change
                                imageUri?.let { uri ->
                                    isUploading = true  // ✅ UI updates immediately
                                    coroutineScope.launch(Dispatchers.IO) { // Run on background thread
                                        try {
                                            uploadImageToFirebase(
                                                uri, context, title, description, navController
                                            ) { progress -> uploadProgress = progress }
                                        } finally {
                                            isUploading = false // Reset UI after upload
                                            onDismiss()
                                        }
                                    }
                                } ?: Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = title.isNotBlank() && description.isNotBlank() && imageUri != null && !currentUploadingState
                    ) {
                        if (currentUploadingState) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading ${(uploadProgress * 100).toInt()}%...")
                            }
                        } else {
                            Text("Upload Post")
                        }
                    }

                }

                    Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
