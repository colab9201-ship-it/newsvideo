package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OutputType
import com.example.data.model.Project
import com.example.data.model.Resolution
import com.example.ui.theme.NewsRed
import com.example.ui.theme.NewsRedDark
import com.example.ui.theme.NewsWhite
import com.example.ui.viewmodel.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProjectViewModel,
    onNavigateToEditor: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val projects by viewModel.projectsList.collectAsState()
    var showCreateDialog by varOf(false)
    
    // Create Project Form Values
    var projectName by varOf("News Video Project")
    var selectedType by varOf(OutputType.LANDSCAPE)
    var selectedRes by varOf(Resolution.P1080)
    var selectedFps by varOf(30)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRO STUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NewsRed,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text(
                            text = "AI News ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Editor",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = NewsRed,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
                
                // Profile/Settings sphere button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF262626))
                        .clickable { onNavigateToSettings() }
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color(0xFF171717)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NewsRed, Color(0xFFF87171))
                                    )
                                )
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A0A0A))
                .padding(16.dp)
        ) {
            // Hero Brand Title / Live Editor Console banner with premium generated banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_news_banner),
                    contentDescription = "News Studio Backdrop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                // Dark gradient scrim overlay for visual contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        "Broadcast News Desk",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Compile high-end news bulletin reels, shorts, or landscapes with tickers, audio speech & live graphics overlays.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Actions Buttons (Sleek Grid UI layout)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("new_project_card")
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = NewsRed.copy(alpha = 0.5f),
                        spotColor = NewsRed.copy(alpha = 0.8f)
                    )
                    .clickable { showCreateDialog = true },
                colors = CardDefaults.cardColors(containerColor = NewsRed),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Decorative white radial grid dot background pattern
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val columns = 12
                        val rows = 6
                        val spacingX = size.width / (columns + 1)
                        val spacingY = size.height / (rows + 1)
                        for (i in 1..columns) {
                            for (j in 1..rows) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = 2.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(i * spacingX, j * spacingY)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Project",
                                tint = NewsRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "New Project",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "Start from scratch",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Navigation Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Templates Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clickable {
                            viewModel.createProject(
                                name = "Breaking News Template",
                                outputType = OutputType.REEL,
                                resolution = Resolution.P1080,
                                fps = 30
                            ) { project ->
                                viewModel.applyNewsTemplate("BREAKING")
                                onNavigateToEditor(project.id)
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262626))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Templates",
                                tint = NewsRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Templates",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                // Settings Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clickable { onNavigateToSettings() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262626))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NewsRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Settings",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Projects List Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "RECENT PROJECTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.2.sp
                )
                Text(
                    "View All",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NewsRed,
                    modifier = Modifier.clickable { /* No-op, visual aesthetic */ }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "No Projects",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No projects found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            "Tap 'New Project' above to create one.",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(projects) { project ->
                        ProjectItemRow(
                            project = project,
                            onOpen = { onNavigateToEditor(project.id) },
                            onDelete = { viewModel.deleteProject(project.id) },
                            onRename = { newName -> viewModel.renameProject(project.id, newName) }
                        )
                    }
                }
            }
        }
    }

    // CREATE PROJECT CONFIGURATION DIALOG
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = {
                Text(
                    "Configure News Project",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NewsRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Output Format Ratio", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutputTypeButton("Reels (9:16)", OutputType.REEL, selectedType) { selectedType = it }
                        OutputTypeButton("Shorts (9:16)", OutputType.SHORTS, selectedType) { selectedType = it }
                        OutputTypeButton("Landscape (16:9)", OutputType.LANDSCAPE, selectedType) { selectedType = it }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Resolution Quality", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResolutionButton("720P", Resolution.P720, selectedRes) { selectedRes = it }
                        ResolutionButton("1080P FULL HD", Resolution.P1080, selectedRes) { selectedRes = it }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Frames Per Second", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FpsButton("30 FPS", 30, selectedFps) { selectedFps = it }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        viewModel.createProject(
                            name = projectName,
                            outputType = selectedType,
                            resolution = selectedRes,
                            fps = selectedFps
                        ) { project ->
                            onNavigateToEditor(project.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                    modifier = Modifier.testTag("submit_create_project")
                ) {
                    Text("Create Project", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
fun RowScope.OutputTypeButton(
    label: String,
    type: OutputType,
    current: OutputType,
    onClick: (OutputType) -> Unit
) {
    val isSelected = type == current
    OutlinedButton(
        onClick = { onClick(type) },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) NewsRed.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) NewsRed else Color.Gray
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                colors = listOf(if (isSelected) NewsRed else Color.Gray, if (isSelected) NewsRed else Color.Gray)
            )
        )
    ) {
        Text(label, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun RowScope.ResolutionButton(
    label: String,
    res: Resolution,
    current: Resolution,
    onClick: (Resolution) -> Unit
) {
    val isSelected = res == current
    OutlinedButton(
        onClick = { onClick(res) },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) NewsRed.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) NewsRed else Color.Gray
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                colors = listOf(if (isSelected) NewsRed else Color.Gray, if (isSelected) NewsRed else Color.Gray)
            )
        )
    ) {
        Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun RowScope.FpsButton(
    label: String,
    fps: Int,
    current: Int,
    onClick: (Int) -> Unit
) {
    val isSelected = fps == current
    OutlinedButton(
        onClick = { onClick(fps) },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) NewsRed.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) NewsRed else Color.Gray
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                colors = listOf(if (isSelected) NewsRed else Color.Gray, if (isSelected) NewsRed else Color.Gray)
            )
        )
    ) {
        Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProjectItemRow(
    project: Project,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateText = sdf.format(Date(project.createdAt))
    var expandedMenu by varOf(false)
    var showRenameDialog by varOf(false)
    var renameValue by varOf(project.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF262626), Color(0xFF1A1A1A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (project.resolution == Resolution.P1080) "1080P" else "720P",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NewsRed
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${project.outputType} • ${if (project.resolution == Resolution.P1080) "1080P" else "720P"}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "|  $dateText",
                        fontSize = 11.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Project Options", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E24))
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Project", color = Color.White) },
                        onClick = {
                            expandedMenu = false
                            onOpen()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, "Open", tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Project", color = Color.White) },
                        onClick = {
                            expandedMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, "Rename", tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Project", color = NewsRed) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = NewsRed) }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = { Text("Rename Project", color = Color.White, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NewsRed,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialog = false
                        onRename(renameValue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewsRed)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}

// Convenient Kotlin state delegate helper
@Composable
fun <T> varOf(initialValue: T) = remember { mutableStateOf(initialValue) }
