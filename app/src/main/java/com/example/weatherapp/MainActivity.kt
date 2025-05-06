package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.weatherapp.ui.theme.WeatherappTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

data class WeatherApiResponse(val location: Location, val current: Current, val forecast: Forecast)
data class Location(val name: String)
data class Current(val temp_f: Double, val wind_mph: Double, val humidity: Int, val condition: Condition)
data class Forecast(val forecastday: List<ForecastDay>)
data class ForecastDay(val date: String, val day: Day, val hour: List<Hour>)
data class Day(
    val maxtemp_f: Double,
    val mintemp_f: Double,
    val avgtemp_f: Double,
    val maxwind_mph: Double,
    val avghumidity: Double,
    val condition: Condition
)
data class Hour(val time: String, val temp_f: Double, val condition: Condition)
data class Condition(val text: String, val icon: String)

interface WeatherApiService {
    @GET("forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") city: String,
        @Query("days") days: Int = 7,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WeatherApiResponse
}

object WeatherApiClient {
    private const val BASE_URL = "https://api.weatherapi.com/v1/"
    val api: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}

class WeatherViewModel : ViewModel() {
    var weather by mutableStateOf<WeatherApiResponse?>(null)
        private set

    fun fetchWeather(city: String, days: Int = 7) {
        viewModelScope.launch {
            try {
                weather = WeatherApiClient.api.getForecast("85072698dab44eb8ae2180629252604", city, days)
            } catch (e: Exception) {
                println("API error: ${e.message}")
            }
        }
    }
}

@Composable
fun WeatherScreen(navController: NavHostController, viewModel: WeatherViewModel) {
    var city by remember { mutableStateOf("New York") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCDFF3))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Weather Wardrobe", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Enter City") },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.fetchWeather(city) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB38CB4))
        ) {
            Text("Get Weather", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        viewModel.weather?.let { data ->
            val current = data.current
            val upcomingHours = data.forecast.forecastday.firstOrNull()?.hour?.filter {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.time)?.after(Date()) == true
            }?.take(6) ?: emptyList()

            Text(city.trim(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("${current.temp_f}°F", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFCFF1), shape = RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                upcomingHours.forEach { hour ->
                    val time = SimpleDateFormat("h a", Locale.getDefault())
                        .format(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(hour.time)!!)
                    val emoji = when (hour.condition.text.lowercase()) {
                        "clear", "sunny" -> "☀️"
                        "partly cloudy" -> "🌤"
                        "cloudy", "overcast" -> "☁️"
                        "rain", "light rain", "showers" -> "🌧"
                        "snow" -> "❄️"
                        else -> "🌥"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(time, fontSize = 14.sp)
                        Text(emoji, fontSize = 20.sp)
                        Text("${hour.temp_f.toInt()}°", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard("💨", "Wind", "${current.wind_mph} mph")
                StatCard("💧", "Humidity", "${current.humidity}%")
                StatCard("☁", "Condition", current.condition.text)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFFFCFF1), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Clothing Suggestion", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        getClothingSuggestion(current.temp_f, current.wind_mph, current.humidity),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BottomNavigationBar(navController)
    }
}

@Composable
fun ForecastScreen(viewModel: WeatherViewModel = viewModel(), navController: NavHostController = rememberNavController()) {
    val weather = viewModel.weather

    if (weather == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFCDFF3)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading forecast...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        return
    }

    val forecast = weather.forecast.forecastday
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("EEEE M/d/yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFFCDFF3))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("7-Day Forecast", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        forecast.forEach { day ->
            val emoji = when (day.day.condition.text.lowercase()) {
                "clear", "sunny" -> "☀️"
                "partly cloudy" -> "🌤"
                "cloudy", "overcast" -> "☁️"
                "rain", "light rain", "showers" -> "🌧"
                "snow" -> "❄️"
                else -> "🌥"
            }

            val formattedDate = outputFormat.format(inputFormat.parse(day.date)!!)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFE6F7), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formattedDate, fontWeight = FontWeight.Bold)
                    Text("$emoji ${day.day.condition.text}")
                    Text("High: ${day.day.maxtemp_f.toInt()}°F | Low: ${day.day.mintemp_f.toInt()}°F")
                    Text("💨 Wind: ${day.day.maxwind_mph} mph")
                    Text("💧 Humidity: ${day.day.avghumidity.toInt()}%")
                    Text(
                        getClothingSuggestion(
                            (day.day.maxtemp_f + day.day.mintemp_f) / 2,
                            day.day.maxwind_mph,
                            day.day.avghumidity.toInt()
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        BottomNavigationBar(navController)
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { navController.navigate("main") }) {
            Icon(Icons.Default.Home, contentDescription = "Main")
        }
        IconButton(onClick = { navController.navigate("forecast") }) {
            Icon(Icons.Default.List, contentDescription = "Forecast")
        }
    }
}

@Composable
fun StatCard(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(Color(0xFFFFD9ED), shape = RoundedCornerShape(12.dp)).padding(12.dp)
    ) {
        Text(icon, fontSize = 22.sp)
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(value, fontSize = 14.sp)
    }
}

fun getClothingSuggestion(temp: Double, wind: Double, humidity: Int): String {
    return when {
        temp < 32 -> "❄️ Heavy coat, gloves, and boots recommended."
        temp in 32.0..50.0 -> "🧥 Light jacket and scarf. Maybe a hat!"
        temp in 50.0..70.0 -> "🧶 Hoodie or sweater. Great layering weather."
        else -> "🩳 Breathable clothing like shorts and a t-shirt."
    } + when {
        wind > 10 -> " 💨 Windbreaker recommended."
        humidity > 80 -> " 💦 Light fabrics due to humidity."
        else -> ""
    }
}

@Composable
fun WeatherAppNav() {
    val navController = rememberNavController()
    val viewModel: WeatherViewModel = viewModel()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { WeatherScreen(navController, viewModel) }
        composable("forecast") { ForecastScreen(viewModel, navController) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherappTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherAppNav()
                }
            }
        }
    }
}
