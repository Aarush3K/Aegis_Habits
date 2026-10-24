package com.nullvector.aegishabits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.nullvector.aegishabits.data.AppDatabase
import com.nullvector.aegishabits.repository.HabitRepository
import com.nullvector.aegishabits.ui.theme.AegisHabitsTheme
import com.nullvector.aegishabits.viewmodel.HabitViewModel
import com.nullvector.aegishabits.viewmodel.HabitViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Initializing Room Database
        val database = AppDatabase.getDatabase(this)

        //Initializing Repository with DAOs
        val repository = HabitRepository(
            habitDao = database.habitDao(),
            completionDao = database.completionDao()
        )

        //Creating ViewModel using our custom Factory
        val factory = HabitViewModelFactory(repository)
        val habitViewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        setContent {
            AegisHabitsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pass habitViewModel to your main UI screen/composable here
                    Greeting(
                        name = "Aegis Habits",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AegisHabitsTheme {
        Greeting("Android")
    }
}