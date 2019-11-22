package edu.utexas.mpc.weatherandsteps

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import android.widget.ImageView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive

    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var textView2: TextView
    lateinit var textView3: TextView
    lateinit var syncButton: Button
    lateinit var publishButton: Button
    lateinit var retrieveButton: Button
    lateinit var retrieveCurrentButton: Button
    lateinit var imageView: ImageView
    lateinit var imageView1: ImageView

    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult
    lateinit var mostRecentWeatherResultForecast: WeatherResultForecast

    // I'm doing a late init here because I need this to be an instance variable but I don't
    // have all the info I need to initialize it yet
    lateinit var mqttAndroidClient: MqttAndroidClient

    // you may need to change this depending on where your MQTT broker is running
    val serverUri = "tcp://192.168.4.1:1883"
    // you can use whatever name you want to here
    val clientId = "EmergingTechMQTTClient"

    //these should "match" the topics on the "other side" (i.e, on the Raspberry Pi)
    val subscribeTopic = "steps"
    val publishTopic = "weather"
    var temp_main=""
    var tempe=""
    var temp_high = ""
    var temp_low= ""
    var precipitation = ""
    var prec=0.0
    var tomorrow_temp_min = 0.0
    var tomorrow_temp_max = 0.0
    var number_of_tomorrow_objects = 0.0
    var tomorrow_precipitation = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView1 = this.findViewById(R.id.text)
        textView = this.findViewById(R.id.text1)
        textView2 = this.findViewById(R.id.text2)
        textView3 = this.findViewById(R.id.text3)
        syncButton = this.findViewById(R.id.syncButton)

        // when the user presses the syncbutton, this method will get called
        syncButton.setOnClickListener({ syncWithPi() })
//        textView = this.findViewById(R.id.text)
        retrieveButton = this.findViewById(R.id.retrieveButton)
        retrieveCurrentButton = this.findViewById(R.id.retrieveCurrentButton)
        publishButton = this.findViewById(R.id.publishButton)
        imageView = this.findViewById(R.id.imageView)
        imageView1 = this.findViewById(R.id.imageView1)


        // when the user presses the syncbutton, this method will get called
        retrieveCurrentButton.setOnClickListener({ requestcurrentWeather() })
        retrieveButton.setOnClickListener({ requestWeather() })
        publishButton.setOnClickListener({publishWeather(tempe,temp_main)})
        queue = Volley.newRequestQueue(this)
        gson = Gson()

        // initialize the paho mqtt client with the uri and client id
        mqttAndroidClient = MqttAndroidClient(getApplicationContext(), serverUri, clientId);

        // when things happen in the mqtt client, these callbacks will be called
        mqttAndroidClient.setCallback(object: MqttCallbackExtended {

            // when the client is successfully connected to the broker, this method gets called
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                println("Connection Complete!!")
                // this subscribes the client to the subscribe topic
                mqttAndroidClient.subscribe(subscribeTopic, 0)


                val message = MqttMessage()
                message.payload = "Hello".toByteArray()
            }




            // this method is called when a message is received that fulfills a subscription
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                println(message)
                textView.text = "Step Count : "+message.toString()
            }

            override fun connectionLost(cause: Throwable?) {
                println("Connection Lost")
            }

            // this method is called when the client succcessfully publishes to the broker
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                println("Delivery Complete")
            }
        })

    }


    fun requestcurrentWeather(){


//        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?id=4254010&appid=0eec983777956761c513884e4255097a").toString()
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?q=Austin,us&appid=0eec983777956761c513884e4255097a&units=metric").toString()

        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->

                    mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)

                    val iconUrl = "https://openweathermap.org/img/w/"+mostRecentWeatherResult.weather.get(0).icon+".png"

                    if (mostRecentWeatherResult.rain !== null) {
                        prec = mostRecentWeatherResult.rain.toString().split("=")[1].replace("}", "").toDouble()
                        println("raining")
                    } else {
                        println("not raining")
                    }
                    textView2.text = mostRecentWeatherResult.weather.get(0).main+"\n"+"Temperature - " +mostRecentWeatherResult.main.temp.toString()+"°F"+"\nPrecipitation: "+prec


                    tempe = mostRecentWeatherResult.main.temp.toString()+"°F"
                    temp_high=mostRecentWeatherResult.main.temp_max.toString()+"°F"
                    temp_low=mostRecentWeatherResult.main.temp_min.toString()+"°F"

                    temp_main = mostRecentWeatherResult.weather.get(0).main
                    println(temp_high)
                    Picasso.with(this).load(iconUrl).into(imageView1)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }


    fun requestWeather(){

        val forecast_url = StringBuilder("http://api.openweathermap.org/data/2.5/forecast?q=Austin,us&appid=0eec983777956761c513884e4255097a&units=metric").toString()
        val stringRequest2 = object : StringRequest(com.android.volley.Request.Method.GET, forecast_url,
                com.android.volley.Response.Listener<String> { response ->
                    mostRecentWeatherResultForecast = gson.fromJson(response, WeatherResultForecast::class.java)
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val tomorrow = calendar.getTime()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val tomorrowAsString = dateFormat.format(tomorrow)
                    var tomorrow_temp_min = 0.0
                    var tomorrow_temp_max = 0.0
                    var number_of_tomorrow_objects = 0.0
                    var tomorrow_precipitation = 0.0
                    for (item in mostRecentWeatherResultForecast.list) {
                        if (tomorrowAsString.toString() in item.dt_txt) {
                            number_of_tomorrow_objects += 1.0
                            tomorrow_temp_max += item.main.temp_max
                            tomorrow_temp_min += item.main.temp_min
                            if (item.rain !== null) {
                                var current_precipitation = item.rain.toString().split("=")[1].replace("}", "").toDouble()
                                tomorrow_precipitation += current_precipitation
                            }
                        }
                    }
                    tomorrow_temp_min /= number_of_tomorrow_objects
                    tomorrow_temp_max /= number_of_tomorrow_objects
                    tomorrow_precipitation /= number_of_tomorrow_objects
                    println("tomorrow temp min = ${tomorrow_temp_min} temp max = ${tomorrow_temp_max} precipitation = ${tomorrow_precipitation} ")
                    textView3.text = "Weather Forecast: \nMax Temp: " + "%.2f".format(tomorrow_temp_min) + ("\nMin Temp: ") + "%.2f".format(tomorrow_temp_max) + ("\nPrecipitation: ") + "%.2f".format(tomorrow_precipitation)
                    val iconUrl = "http://openweathermap.org/img/wn/"+mostRecentWeatherResultForecast.list.get(2).weather.get(0).icon+".png"
//
                    Picasso.with(this).load(iconUrl).into(imageView)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest2)
    }


    fun publishWeather(tempe:String,temp_main:String) {
        val message = MqttMessage()
        // this publishes a message to the publish topic

        message.payload = temp_high.toByteArray() + (",").toByteArray() + temp_low.toByteArray() + (",").toByteArray() + prec.toString().toByteArray()+",".toByteArray()+tomorrow_temp_min.toString().toByteArray() + (",").toByteArray() + tomorrow_temp_max.toString().toByteArray() + (",").toByteArray() + tomorrow_precipitation.toString().toByteArray()
        mqttAndroidClient.publish(publishTopic, message)

        textView2.text = "Weather Forecast: " + message.toString()
    }
        // this method just connects the paho mqtt client to the broker

    fun syncWithPi(){
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()
    }

}

class WeatherResult(val id: Int, val name: String, val cod: Int, val coord: Coordinates, val main: WeatherMain, val weather: Array<Weather>,val rain:Any)
class Coordinates(val lon: Double, val lat: Double)

class WeatherMain(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double)
class WeatherResultForecast(val id: Int, val name: String, val rain: Any, val cod: Int, val coord: Coordinates, val main: WeatherMain, val list: Array<WeatherForecast>,val weather: Array<Weather>)
class WeatherForecast(val dt: Int, val main: WeatherMain, val rain: Any, val dt_txt: String,val weather: Array<Weather>)
class Weather(val id: Int, val main: String, val description: String, val icon: String)