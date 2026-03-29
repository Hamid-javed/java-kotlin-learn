package com.rameez.hel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SharedPref.appLaunched(this, true)

//
//        val hostFragment =supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
//        navController = hostFragment.findNavController()
//
//        if(SharedPref.getFilterScreenCancelled(this).not()) {
//            navController.navigate(R.id.carouselFragment)
//        }
    }

    override fun onStart() {
        super.onStart()

    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        SharedPref.appLaunched(this, false)
//    }
}