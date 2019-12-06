package edu.utexas.mpc.weatherandsteps


import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.style.LineHeightSpan
import edu.utexas.mpc.weatherandsteps.MainActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import android.widget.ImageView
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class Steps: AppCompatActivity(){

    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive

//    val age=intent.getStringExtra("age")
//    val gender=intent.getStringExtra("gender")
//    val height=intent.getStringExtra("height")
//    val weight=intent.getStringExtra("weight")
    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var textView2: TextView
    lateinit var textView3: TextView
    lateinit var syncButton: Button
    lateinit var publishButton: Button
    lateinit var retrieveButton: Button
    lateinit var retrieveCurrentButton: Button
    lateinit var switchNetwork: Button
    lateinit var imageView: ImageView
    lateinit var imageView1: ImageView

    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult
    lateinit var mostRecentWeatherResultForecast: WeatherResultForecast
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var locationGps: Location? = null

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
    var temp_min = 100.0
    var temp_max = 0.0
    val age = Global.getAge()
//    val gender = Global.getGender()
    val gender = Global.getGender()
    val height = Global.getHeight()
    val weight = Global.getWeight()





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.steps)

        println("ON CREATE")
//        var client = LocationServices.getFusedLocationProviderClient(this)




        textView1 = this.findViewById(R.id.text)
        textView = this.findViewById(R.id.text1)
        textView2 = this.findViewById(R.id.text2)
        textView3 = this.findViewById(R.id.text3)
        syncButton = this.findViewById(R.id.syncButton)
        println("Age = "+age)
        println("Gender = "+gender)
        println("Height = "+height)
        println("Weight = "+weight)

        // when the user presses the syncbutton, this method will get called
        syncButton.setOnClickListener({ syncWithPi() })
//        textView = this.findViewById(R.id.text)
//        retrieveButton = this.findViewById(R.id.retrieveButton)
        retrieveCurrentButton = this.findViewById(R.id.retrieveCurrentButton)
        publishButton = this.findViewById(R.id.publishButton)
        switchNetwork = this.findViewById(R.id.switchButton)
        imageView = this.findViewById(R.id.imageView)
        imageView1 = this.findViewById(R.id.imageView1)

        getLocation()

        // when the user presses the syncbutton, this method will get called
        retrieveCurrentButton.setOnClickListener({ requestcurrentWeather() })
//        retrieveButton.setOnClickListener({ requestWeather() })
        publishButton.setOnClickListener({publishWeather()})
        switchNetwork.setOnClickListener({ switchNetwork() })
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

//                publishWeather()
                val message = MqttMessage()
                message.payload = "Hello".toByteArray()
            }

            // this method is called when a message is received that fulfills a subscription
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                println(message)
                textView.text = "Today's Goal : "+message.toString()
            }

            override fun connectionLost(cause: Throwable?) {
                println("Connection Lost")
            }

            // this method is called when the client successfully publishes to the broker
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                println("Delivery Complete")
            }
        })

    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        try {
//            retrieveCurrentButton.isEnabled = false
            retrieveCurrentButton.isClickable = false
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (hasGps) {
                println("i has GPS power")
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F, object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if (location != null) {
                            locationGps = location
                            longitude = locationGps!!.longitude
                            latitude = locationGps!!.latitude
                            println("GPS lat :  ${latitude}")
                            println("GPS long :  ${longitude}")
                            // stops the device from continuously listening for location
                            locationManager.removeUpdates(this)
                            retrieveCurrentButton.isClickable = true
                        }

                    }

                    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

                    }

                    override fun onProviderEnabled(p0: String?) {

                    }

                    override fun onProviderDisabled(p0: String?) {

                    }
                })
                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null)
                    //println("had a location at the start. Probably won't happen for us ")
                    locationGps = localGpsLocation
                    longitude = locationGps!!.longitude
                    latitude = locationGps!!.latitude
                    println("GPS lat :  ${latitude}")
                    println("GPS long :  ${longitude}")
                    // stops the device from continuously listening for location
                    retrieveCurrentButton.isClickable = true
            }

        } catch (e: Exception) {
            println(e.toString())
        }
    }


    fun requestcurrentWeather(){


//        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?id=4254010&appid=0eec983777956761c513884e4255097a").toString()
        // hard coded city name
//        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?q=Austin,us&appid=0eec983777956761c513884e4255097a&units=imperial").toString()
        // lat/long coordinates
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?lat=${latitude}&lon=${longitude}&appid=0eec983777956761c513884e4255097a&units=imperial").toString()


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

                    tempe = mostRecentWeatherResult.main.temp.toString()+"°F"
                    temp_high=mostRecentWeatherResult.main.temp_max.toString()+"°F"
                    temp_low=mostRecentWeatherResult.main.temp_min.toString()+"°F"
                    textView2.text = "Weather Today:\nMax Temp: " +temp_high+"\nMin Temp: " +temp_low+"\nPrecipitation: "+prec
                    temp_main = mostRecentWeatherResult.weather.get(0).main
                    println(temp_high)
                    Picasso.with(this).load(iconUrl).into(imageView1)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
        requestWeather()
    }


    fun requestWeather(){

        // hard coded city
       // val forecast_url = StringBuilder("http://api.openweathermap.org/data/2.5/forecast?q=Austin,us&appid=0eec983777956761c513884e4255097a&units=imperial").toString()
        // lat/long coordinates
        val forecast_url = StringBuilder("http://api.openweathermap.org/data/2.5/forecast?lat=${latitude}&lon=${longitude}&appid=0eec983777956761c513884e4255097a&units=imperial").toString()
        val stringRequest2 = object : StringRequest(com.android.volley.Request.Method.GET, forecast_url,
                com.android.volley.Response.Listener<String> { response ->
                    mostRecentWeatherResultForecast = gson.fromJson(response, WeatherResultForecast::class.java)
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val tomorrow = calendar.getTime()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val tomorrowAsString = dateFormat.format(tomorrow)
                    temp_min = 100.0
                    temp_max = 0.0
                    tomorrow_temp_min = 0.0
                    tomorrow_temp_max = 0.0
                    number_of_tomorrow_objects = 0.0
                    tomorrow_precipitation = 0.0
                    for (item in mostRecentWeatherResultForecast.list) {
                        if (tomorrowAsString.toString() in item.dt_txt) {

                            if (item.main.temp_max > temp_max)
                            {
                                temp_max = item.main.temp_max
                            }
                            if( item.main.temp_min < temp_min)
                            {
                                temp_min = item.main.temp_min
                            }
                            number_of_tomorrow_objects += 1.0
//                            tomorrow_temp_max += item.main.temp_max
//                            tomorrow_temp_min += item.main.temp_min
                            if (item.rain !== null) {
                                var current_precipitation = item.rain.toString().split("=")[1].replace("}", "").toDouble()
                                tomorrow_precipitation += current_precipitation
                            }
                        }
                    }
//                    tomorrow_temp_min /= number_of_tomorrow_objects
//                    tomorrow_temp_max /= number_of_tomorrow_objects
//                    tomorrow_precipitation /= number_of_tomorrow_objects
                    println("tomorrow temp min = ${temp_min} temp max = ${temp_max} precipitation = ${tomorrow_precipitation} ")
                    textView3.text = "Weather Forecast: \nMax Temp: " + "%.2f".format(temp_max)+"°F" + ("\nMin Temp: ") + "%.2f".format(temp_min)+"°F" + ("\nPrecipitation: ") + "%.2f".format(tomorrow_precipitation)
                    val iconUrl = "http://openweathermap.org/img/wn/"+mostRecentWeatherResultForecast.list.get(2).weather.get(0).icon+".png"
//
                    Picasso.with(this).load(iconUrl).into(imageView)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest2)
    }


    fun publishWeather() {
        val message = MqttMessage()
        // this publishes a message to the publish topic
        println("Age = "+age)
        println("Gender = "+gender)
        println("Height = "+height)
        println("Weight = "+weight)

        message.payload = temp_high.toByteArray() + (",").toByteArray() + temp_low.toByteArray() + (",").toByteArray() + prec.toString().toByteArray()+",".toByteArray()+"%.2f".format(temp_max).toByteArray() + (",").toByteArray() + "%.2f".format(temp_min).toByteArray() + (",").toByteArray() + "%.2f".format(tomorrow_precipitation).toByteArray()+",".toByteArray()+ age.toString().toByteArray(Charsets.UTF_8) + (",").toByteArray(Charsets.UTF_8) + gender.toString().toByteArray(Charsets.UTF_8) + (",").toByteArray() +height.toString().toByteArray(Charsets.UTF_8) + (",").toByteArray() +weight.toString().toByteArray(Charsets.UTF_8)+(",").toByteArray()+temp_main.toByteArray(Charsets.UTF_8)
        mqttAndroidClient.publish(publishTopic, message)

//        textView2.text = "Weather Forecast: " + message.toString()
    }
    // this method just connects the paho mqtt client to the broker

    fun syncWithPi(){
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()
    }

    fun switchNetwork(){
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS));
        basicAlert();
        showNetwork()
    }

    val positiveButtonClick = {dialog:DialogInterface,which:Int ->Toast.makeText(applicationContext,"You're Connected to IOT-MIS-20",Toast.LENGTH_SHORT).show()}
    val negativeButtonClick = {dialog:DialogInterface,which:Int ->switchNetwork()}
    fun basicAlert(){

        val builder = AlertDialog.Builder(this)


        with(builder)
        {
            setTitle("Network Alert")
            setMessage("Did you switch your wiFi network to IOT-MIS-20?")
            setPositiveButton("Yes", DialogInterface.OnClickListener(function = positiveButtonClick))
            setNegativeButton("No", negativeButtonClick)
//            setNeutralButton("Maybe", neutralButtonClick)
            show()

        }


    }
    fun showNetwork()
    {

        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnected == true
        if ((activeNetwork?.extraInfo).toString()=="IOT-MIS-20")
        {println(activeNetwork?.extraInfo)}
        else println("Damn!")
//        var cnf=""
//        if(activeNetwork?.extraInfo.equals("IOT-MIS-20"))
//        {
//            cnf = "True"
//        }
//        else
//            switchNetwork()
//        return cnf

//        println(isConnected)
//        println(activeNetwork?.extraInfo)
//        textView1.text = "You are connected to "+activeNetwork?.extraInfo

    }



}



class WeatherResult(val id: Int, val name: String, val cod: Int, val coord: Coordinates, val main: WeatherMain, val weather: Array<Weather>,val rain:Any)
class Coordinates(val lon: Double, val lat: Double)

class WeatherMain(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double)
class WeatherResultForecast(val id: Int, val name: String, val rain: Any, val cod: Int, val coord: Coordinates, val main: WeatherMain, val list: Array<WeatherForecast>,val weather: Array<Weather>)
class WeatherForecast(val dt: Int, val main: WeatherMain, val rain: Any, val dt_txt: String,val weather: Array<Weather>)
class Weather(val id: Int, val main: String, val description: String, val icon: String)