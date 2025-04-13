package com.example.agroX.mainScreen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.agroX.presentation.sign_in.UserData
import com.example.agroX.productPage.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

@Composable
fun ProductRecommendation(navController: NavController) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val products = remember { mutableStateListOf<Product>() }
    var expandedProductId by remember { mutableStateOf<String?>(null) }
    var gridColumn by remember { mutableStateOf(2) }
    var expanded by remember { mutableStateOf(false) }
    var productToUpdate by remember { mutableStateOf<Product?>(null) }
    var currentUserData by remember { mutableStateOf<UserData?>(null) }

    // Fetch products from Firestore on launch
    LaunchedEffect(Unit) {
        fetchProductsFromFirebase(products)
    }

    // Fetch current user data from Firestore using current user UID
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserData = fetchUserDataFromFirestore(currentUser.uid) ?: UserData()
        } else {
            currentUserData = UserData()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grid layout options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Grid Options")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("1 Column") }, onClick = {
                            gridColumn = 1
                            expanded = false
                        })
                        DropdownMenuItem(text = { Text("2 Columns") }, onClick = {
                            gridColumn = 2
                            expanded = false
                        })
                    }
                }
            }

            // Display product grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumn),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ensure we have the currentUserData to pass its userId
                items(products) { product ->
                    ProductCard(
                        product = product,
                        currentUserId = currentUserData?.userId ?: "",
                        isExpanded = expandedProductId == product.id,
                        gridColumn = gridColumn,
                        onClick = {
                            expandedProductId = if (expandedProductId == product.id) null else product.id
                        },
                        onUpdate = { productToUpdate = it },
                        onDelete = { prod ->
                            deleteProductFromFirebase(prod) {
                                products.remove(prod)
                            }
                        }
                    )
                }
            }
        }

        // Add Product Dialog (for new product)
        if (showDialog && currentUserData != null) {
            AddProductDialog(
                userData = currentUserData!!,
                onDismiss = { showDialog = false },
                onAdd = { newProduct ->
                    addProductToFirebase(newProduct) { productWithId ->
                        products.add(productWithId)
                    }
                    showDialog = false
                }
            )
        }

        // Update Product Dialog
        if (productToUpdate != null) {
            UpdateProductDialog(
                product = productToUpdate!!,
                onDismiss = { productToUpdate = null },
                onUpdate = { updatedProduct ->
                    updateProductInFirebase(updatedProduct) { p ->
                        val index = products.indexOfFirst { it.id == p.id }
                        if (index != -1) {
                            products[index] = p
                        }
                        productToUpdate = null
                    }
                }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    currentUserId: String,
    isExpanded: Boolean,
    gridColumn: Int,
    onClick: () -> Unit,
    onUpdate: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    val textSize = if (gridColumn == 1) 18.sp else 16.sp
    val truncatedName = if (gridColumn == 2 && product.name.length > 12) {
        product.name.take(10) + "..."
    } else product.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(text = truncatedName, fontSize = textSize, fontWeight = FontWeight.Bold)
                Text(text = "₹${product.price}", fontSize = textSize, color = Color(0xFF2E7D32))
                Text(
                    text = "By: ${product.user.fullName} (@${product.user.userName})",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                if (isExpanded && gridColumn < 3) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Category: ${product.category}", fontSize = 13.sp)
                    Text(text = "In stock: ${product.quantity}", fontSize = 13.sp)
                    // Only show update and delete buttons if this product belongs to the current user.
                    if (product.user.userId == currentUserId) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onUpdate(product) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Update Product")
                            }
                            IconButton(onClick = { onDelete(product) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Product")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Suspend function to fetch user details from Firestore "users" collection using the provided userId.
 */
suspend fun fetchUserDataFromFirestore(userId: String): UserData? {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .await() // Requires kotlinx-coroutines-play-services dependency
        snapshot.toObject(UserData::class.java)
    } catch (e: Exception) {
        Log.e("UserData", "Error fetching user data", e)
        null
    }
}

/**
 * For simplicity, this function returns basic data from FirebaseAuth.
 * Replace this with a real Firestore fetch if you store additional user details.
 */


fun createTempImageUri(context: Context): Uri? {
    return try {
        val storageDir: File = context.cacheDir
        val tempFile = File.createTempFile("temp_image_", ".jpg", storageDir)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AddProductDialog(
    userData: UserData,
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    // We'll use a state to hold the selected image URI (from gallery or camera)
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Launcher for selecting an image from the gallery.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri.value = uri
    }

    // Launcher for taking a photo with the camera.
    // We create a temporary URI and then launch the camera.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        // If photo capture is successful, the selectedImageUri state is already set.
        if (!success) {
            selectedImageUri.value = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Buttons to select an image either via gallery or camera.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("Select from Gallery")
                    }
                    Button(onClick = {
                        // Create a temporary file URI for the camera capture.
                        val tempUri = createTempImageUri(context)
                        selectedImageUri.value = tempUri
                        tempUri?.let { cameraLauncher.launch(it) }
                    }) {
                        Text("Take Photo")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Preview the selected/taken image
                selectedImageUri.value?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    // Upload the image to Firebase Storage and retrieve its download URL.
                    val downloadUrl = selectedImageUri.value?.let { uri ->
                        uploadProductImage(uri, userData.userId)
                    } ?: ""
                    val product = Product(
                        name = name,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        category = category,
                        quantity = quantity.toIntOrNull() ?: 0,
                        imageUrl = downloadUrl,
                        user = userData
                    )
                    onAdd(product)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Suspend function to upload an image file to Firebase Storage.
 * It creates a folder "product_images" with a subfolder for userID.
 * Returns the download URL as a String.
 */
suspend fun uploadProductImage(fileUri: Uri, userId: String): String? {
    return try {
        val storageRef = FirebaseStorage.getInstance().reference
        // Generate a unique file name (here using the current time in millis)
        val fileName = "${System.currentTimeMillis()}.jpg"
        // Create a reference in "product_images/{userId}/{fileName}"
        val imageRef = storageRef.child("product_images/$userId/$fileName")
        // Upload the file to this reference
        imageRef.putFile(fileUri).await()
        // Return the download URL of the uploaded file
        imageRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun UpdateProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onUpdate: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var category by remember { mutableStateOf(product.category) }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val updatedProduct = product.copy(
                    name = name,
                    description = description,
                    price = price.toDoubleOrNull() ?: product.price,
                    category = category,
                    quantity = quantity.toIntOrNull() ?: product.quantity,
                    imageUrl = imageUrl
                )
                onUpdate(updatedProduct)
            }) {
                Text("Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Update Product") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") })
            }
        }
    )
}

fun addProductToFirebase(product: Product, onSuccess: (Product) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("products").document()
    val productWithId = product.copy(id = docRef.id)
    docRef.set(productWithId)
        .addOnSuccessListener { onSuccess(productWithId) }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Failed to upload product", e)
        }
}

fun updateProductInFirebase(product: Product, onSuccess: (Product) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("products").document(product.id)
        .set(product)
        .addOnSuccessListener { onSuccess(product) }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error updating product", e)
        }
}

fun deleteProductFromFirebase(product: Product, onSuccess: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val productDoc = db.collection("products").document(product.id)

    // Check if there's an image URL and delete the image from Storage first.
    if (product.imageUrl.isNotBlank()) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(product.imageUrl)
        storageRef.delete()
            .addOnSuccessListener {
                // After the image is deleted, delete the Firestore document.
                productDoc.delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error deleting product document", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error deleting product image", e)
            }
    } else {
        // If there's no image URL, simply delete the Firestore document.
        productDoc.delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error deleting product document", e)
            }
    }
}


fun fetchProductsFromFirebase(productList: SnapshotStateList<Product>) {
    val db = FirebaseFirestore.getInstance()
    db.collection("products").get()
        .addOnSuccessListener { documents ->
            productList.clear()
            for (doc in documents) {
                doc.toObject<Product>()?.let { productList.add(it) }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error fetching products", e)
        }
}

//@Composable
//fun ProductCard(
//    product: Product,
//    isExpanded: Boolean,
//    gridColumn: Int,
//    onClick: () -> Unit,
//    onUpdate: (Product) -> Unit,
//    onDelete: (Product) -> Unit
//) {
//    val textSize = if (gridColumn == 1) 18.sp else 16.sp
//    val truncatedName = if (gridColumn == 2 && product.name.length > 12) {
//        product.name.take(10) + "..."
//    } else product.name
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() },
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Column(modifier = Modifier.padding(8.dp)) {
//            AsyncImage(
//                model = product.imageUrl,
//                contentDescription = product.name,
//                contentScale = ContentScale.Crop,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(150.dp)
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
//                Text(text = truncatedName, fontSize = textSize, fontWeight = FontWeight.Bold)
//                Text(text = "₹${product.price}", fontSize = textSize, color = Color(0xFF2E7D32))
//                Text(
//                    text = "By: ${product.user.fullName} (@${product.user.userName})",
//                    fontSize = 13.sp,
//                    color = Color.Gray
//                )
//                if (isExpanded && gridColumn < 3) {
//                    Spacer(modifier = Modifier.height(6.dp))
//                    Text(
//                        text = product.description,
//                        fontSize = 14.sp,
//                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(text = "Category: ${product.category}", fontSize = 13.sp)
//                    Text(text = "In stock: ${product.quantity}", fontSize = 13.sp)
//                    // Action buttons for update and delete
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.End
//                    ) {
//                        IconButton(onClick = { onUpdate(product) }) {
//                            Icon(Icons.Default.Edit, contentDescription = "Update Product")
//                        }
//                        IconButton(onClick = { onDelete(product) }) {
//                            Icon(Icons.Default.Delete, contentDescription = "Delete Product")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
