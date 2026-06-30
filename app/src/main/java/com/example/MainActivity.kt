package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.repository.ProjectRepository
import com.example.ui.navigation.NewsNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ProjectViewModel
import com.example.ui.viewmodel.ProjectViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var viewModel: ProjectViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Local Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "news_studio_editor.db"
        )
        .fallbackToDestructiveMigration()
        .build()

        // 2. Initialize Repositories and Settings Managers
        repository = ProjectRepository(database.projectDao())
        dataStoreManager = DataStoreManager(applicationContext)

        // 3. Initialize ViewModels via Factory
        val factory = ProjectViewModelFactory(repository, dataStoreManager)
        viewModel = ViewModelProvider(this, factory)[ProjectViewModel::class.java]

        setContent {
            val darkMode by viewModel.darkModeState.collectAsState()

            MyApplicationTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A)),
                    color = Color.Unspecified
                ) {
                    val navController = rememberNavController()
                    NewsNavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
