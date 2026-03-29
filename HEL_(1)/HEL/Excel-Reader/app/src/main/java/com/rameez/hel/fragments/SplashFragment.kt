package com.rameez.hel.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rameez.hel.R
import com.rameez.hel.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private lateinit var mBinding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mBinding = FragmentSplashBinding.inflate(layoutInflater, container, false)

        val videoUri = Uri.parse("android.resource://" + requireActivity().packageName + "/" + R.raw.splash_gif)
        mBinding.videoView.setVideoURI(videoUri)
        mBinding.videoView.start()

        lifecycleScope.launch {
            delay(1000)
            findNavController().navigate(R.id.WIPListFragment)
        }

        return mBinding.root
    }


}