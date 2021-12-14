package ru.ac.bd.knowweather

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    private val LOCATION_PERMISSION_REQ_CODE = 1000;

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btGetLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        // checking location permission
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // request permission
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE);

            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                // getting the last known or current location
                latitude = location.latitude
                longitude = location.longitude
                var cityName = getCity(latitude,longitude)
                var countryName = getCountry(latitude,longitude)


                tvCity.text = "City: ${cityName}"
                tvCountry.text = "Country: ${countryName}"

                getWeather(cityName)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed on getting current location",
                    Toast.LENGTH_SHORT).show()
            }



    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQ_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                } else {
                    // permission denied
                    Toast.makeText(this, "You need to grant permission to access location",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun getCity(lat: Double ,lng: Double):String{
        var geocoder = Geocoder(this)
        var  list = geocoder.getFromLocation(lat,lng,1)
        return list[0].locality
    }
    private fun getCountry(lat: Double ,lng: Double):String{
        var geocoder = Geocoder(this)
        var  list = geocoder.getFromLocation(lat,lng,1)
        return list[0].countryName
    }



    private fun getWeather(cityName : String)
    {
        var url = "https://api.openweathermap.org/data/2.5/weather?q=${cityName}&units=metric&appid=137dab9a594d177067f155d6bcf1dd19"

        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response)
            {
                var jsonString = response.body?.string()
                ParseJson(jsonString)

            }
        })
    }
    private fun ParseJson(jsonString: String?) {

        // get JSONObject from JSON file
        val obj = JSONObject(jsonString)

        Log.d("TAG", obj.toString())
        //Log.d("TAG", "ParseJson: "+obj.getString("name"))

        // fetch JSONObject named weather
        val jsonArray: JSONArray = obj.getJSONArray("weather")
        val weatherJsonObject: JSONObject = jsonArray.getJSONObject(0)
        val weatherDescription: String = weatherJsonObject.getString("description")



        //id to get icon from https://openweathermap.org/img/wn/<iconID>@2x.png
        // full icon list: https://openweathermap.org/weather-conditions#Icon-list
        val weatherIcon: String = weatherJsonObject.getString("icon")

        val mainJSONObject: JSONObject = obj.getJSONObject("main")
        val temp: String = (mainJSONObject.getString("temp"))
        val feelsLike: String = (mainJSONObject.getString("feels_like"))
        val humidity: String = mainJSONObject.getString("humidity") // %

        val windJSONObject: JSONObject = obj.getJSONObject("wind")
        val windSpeed: String = windJSONObject.getString("speed") // meter/sec

        runOnUiThread{
            val weatherImage : ImageView = findViewById<ImageView>(R.id.weather_icon)
            Picasso.get().load("https://openweathermap.org/img/wn/${weatherIcon}@2x.png").into(weatherImage)
            weatherImage.visibility = View.VISIBLE
        }



        Tvweather.text = "Weather Status : ${weatherDescription}"
        Tvtemp.text = "Temparature : ${temp}°C"
        Tvhumidity.text = "Humidity : ${humidity}%"
        Tvfeels.text = "Feels Like : ${feelsLike}°C"


    }


}