package com.example.agribot.MainScreen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import com.example.agribot.R
import com.example.agribot.presentation.sign_in.isInternetAvailable

@Composable
fun ChatbotScreen() {
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat messages displayed at the top
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true // Ensures new messages appear at the bottom
        ) {
            items(messages.reversed()) { message ->
                Text(text = message, modifier = Modifier.padding(8.dp))
            }
        }

        // Input field at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                singleLine = true,


            )
            Spacer(modifier = Modifier.width(5.dp))
            OutlinedButton(
                onClick = {
                    if (userInput.text.isNotBlank()) {
                        messages = messages + userInput.text
                        userInput = TextFieldValue("") // Clear input field
                    }
                },
                shape = RoundedCornerShape(8.dp), // Rounded corners
                border = null,
                modifier = Modifier,
                contentPadding = PaddingValues(8.dp)
            ) {
                Image(
                    Icons.AutoMirrored.Filled.Send, // Ensure you have the image in res/drawable
                    contentDescription = "Send",
                    modifier = Modifier.size(24.dp) ,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    // Adjust the size as needed
                )
            }
        }
    }
}
