package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NewsRed
import com.example.ui.theme.NewsWhite
import com.example.ui.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val darkMode by viewModel.darkModeState.collectAsState()
    val defaultLogo by viewModel.defaultLogoState.collectAsState()
    val defaultFont by viewModel.defaultFontState.collectAsState()

    var logoInput by varOf(defaultLogo)
    var fontInput by varOf(defaultFont)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Switcher Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, "Theme", tint = NewsRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Dark Theme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Enables high-contrast studio theme", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NewsRed, checkedTrackColor = NewsRed.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            // Default Brand Channel Logo Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, "Logo", tint = NewsRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Default Watermark Logo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = logoInput,
                        onValueChange = { logoInput = it },
                        label = { Text("Logo File Path / URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NewsRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_logo_path_input")
                    )
                }
            }

            // Default Text and Subtitle Fonts
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, "Font", tint = NewsRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Default Broadcast Font", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fontInput,
                        onValueChange = { fontInput = it },
                        label = { Text("Font Family (English & Bangla)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NewsRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_font_input")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Settings configuration changes Button
            Button(
                onClick = {
                    viewModel.setDefaultLogo(logoInput)
                    viewModel.setDefaultFont(fontInput)
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("settings_save_button")
            ) {
                Icon(Icons.Default.Save, "Save Settings", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
