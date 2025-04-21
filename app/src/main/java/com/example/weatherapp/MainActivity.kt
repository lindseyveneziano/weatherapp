package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapp.ui.theme.WeatherappTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Data Model ---
data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
)

data class Main(val temp: Double, val humidity: Int)
data class Weather(val description: String)
data class Wind(val speed: Double)

// --- Retrofit API ---
interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "imperial"
    ): WeatherResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}

// --- ViewModel ---
class WeatherViewModel : ViewModel() {
    var weatherState = mutableStateOf<WeatherResponse?>(null)
        private set

    fun fetchWeather(city: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWeather(city, "a5f7bbb4cdc589ebf7147da6a57204e1")
                weatherState.value = response
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}

// --- Composables ---
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = viewModel()) {
    var city by remember { mutableStateOf("New York") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Enter City") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { viewModel.fetchWeather(city) }) {
            Text("Get Weather")
        }

        Spacer(modifier = Modifier.height(16.dp))

        viewModel.weatherState.value?.let { weather ->
            Text("Temperature: ${weather.main.temp}°F", style = MaterialTheme.typography.headlineSmall)
            Text("Humidity: ${weather.main.humidity}%", style = MaterialTheme.typography.bodyMedium)
            Text("Wind Speed: ${weather.wind.speed} mph", style = MaterialTheme.typography.bodyMedium)
            Text("Condition: ${weather.weather.first().description}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))
            Text("Clothing Suggestion: ${getClothingSuggestion(weather.main.temp, weather.wind.speed, weather.main.humidity)}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherScreenPreview() {
    WeatherappTheme {
        WeatherScreen()
    }
}

fun getClothingSuggestion(temp: Double, wind: Double, humidity: Int): String {
    return when {
        temp < 32 -> "Heavy coat, gloves, and boots."
        temp in 32.0..50.0 -> "Light jacket and scarf."
        temp in 50.0..70.0 -> "Hoodie or sweater."
        else -> "Breathable clothing."
    } + when {
        wind > 10 -> " Windbreaker recommended."
        humidity > 80 -> " Light fabrics due to humidity."
        else -> ""
    }
}

// --- MainActivity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}
