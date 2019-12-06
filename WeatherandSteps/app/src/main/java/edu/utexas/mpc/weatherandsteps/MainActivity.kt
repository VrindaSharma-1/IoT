package edu.utexas.mpc.weatherandsteps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import android.widget.Button
import android.widget.TextView
import android.widget.Toast


import edu.utexas.mpc.weatherandsteps.Global
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.steps.view.*

private const val PERMISSION_REQUEST = 10

class MainActivity : AppCompatActivity() {

    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive

    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var textView2: TextView
    lateinit var textView3: TextView
    lateinit var nextButton: Button
    lateinit var cancelButton: Button


    // I'm doing a late init here because I need this to be an instance variable but I don't
    // have all the info I need to initialize it yet

    private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission(permissions)) {
                requestPermissions(permissions, PERMISSION_REQUEST)
            }
        }
//        textView1 = this.findViewById(R.id.text)
//        textView = this.findViewById(R.id.text1)
//        textView2 = this.findViewById(R.id.text2)
//        textView3 = this.findViewById
        nextButton = this.findViewById(R.id.nextButton)
        cancelButton = this.findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener{
            println("Age = "+age)
            println("Gender = "+gender)
            println("Height = "+height)
            println("Weight = "+weight)
            val intent = Intent(this, Steps()::class.java)
            this.startActivity(intent)


        }
     nextButton.setOnClickListener{
         var age = age_text.text.toString()
         var gender =gender_text.text.toString()
         var height = height_text.text.toString()
         var weight = weight_text.text.toString()
         Global.setAge(age)
         Global.setGender(gender)
         Global.setHeight(height)
         Global.setWeight(weight)
         println("Age = "+age)
         println("Gender = "+gender)
         println("Height = "+height)
         println("Weight = "+weight)

//         (activity as NavigationHost).navigateTo(Steps(), false)
         val intent = Intent(this, Steps()::class.java)
         this.startActivity(intent)

//         val intent = Intent(this@MainActivity,Steps::class.java)
//         intent.putExtra("age",R.id.age)
//
//         intent.putExtra("gender",R.id.gender)
//         intent.putExtra("height",R.id.height)
//         intent.putExtra("weight",R.id.weight)
//         startActivity(intent)
     }
//

    }

    private fun checkPermission(permissionArray: Array<String>): Boolean {
        var allSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allSuccess = false
        }
        return allSuccess
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            var allSuccess = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allSuccess = false
                    val requestAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(permissions[i])
                    if (requestAgain) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Go to settings and enable the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }
}


