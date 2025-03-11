package com.example.agribot.mainScreen


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agribot.R

data class Post(
    val title: String,
    val description: String,
    val imageRes: Int
)

@Composable
fun HomeScreen() {
    var searchText by remember { mutableStateOf("") }

    val posts = listOf(
        Post("New Farming Techniques", "Learn about the latest farming innovations.", R.drawable._461929),
        Post("Organic Farming", "Discover the benefits of organic farming.", R.drawable._461929),
        Post("Climate & Crops", "How climate affects crop production.", R.drawable._461929)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomOutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = "Search",
            placeholder = "Search posts...",
            leadingIcon = Icons.Default.Search
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(posts) { post ->
                PostCard(post)
            }
        }
    }
}

@Composable
fun PostCard(post: Post) {
    var isLiked by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            AsyncImage(
                model = post.imageRes,
                contentDescription = "Post Image",
                contentScale = ContentScale.Crop, // Crop the image to fit the card
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Adjust this ratio as needed for best fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.description,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LikeButton(isLiked) { isLiked = !isLiked }
                ActionButton(Icons.Default.Add, "Comment")
                ActionButton(Icons.Default.Share, "Share")
            }
        }
    }
}

@Composable
fun LikeButton(isLiked: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) Color.Red else Color.Gray
        )
    }
}

@Composable
fun ActionButton(icon: ImageVector, contentDesc: String) {
    IconButton(onClick = { }) {
        Icon(imageVector = icon, contentDescription = contentDesc, tint = Color.Gray)
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 16.sp) },
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = Color.Gray) }
        },
        trailingIcon = trailingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = Color.Gray) }
        },
        isError = isError,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp),
        shape = RoundedCornerShape(12.dp),  // Rounded corners
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF4CAF50), // Green when focused
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color(0xFF4CAF50), // Cursor color
            focusedLabelColor = Color(0xFF4CAF50),
            unfocusedLabelColor = Color.Gray,
            errorBorderColor = Color.Red
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}
