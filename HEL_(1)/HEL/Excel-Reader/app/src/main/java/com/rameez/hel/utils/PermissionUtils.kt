package com.rameez.hel.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class PermissionUtils(fragment: Fragment) {
    var onPermissionResult: OnPermissionResult? = null
    private var permissionsResultLauncher: ActivityResultLauncher<Array<String>> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { success ->
            if (success.isEmpty().not()) {
                if (success.containsValue(false)) {
                    onPermissionResult?.onPermissionDenied()
                } else {
                    onPermissionResult?.onPermissionGranted()
                }
            }
        }


    fun requestPermissions(permissions: Array<String>): PermissionUtils {
        permissionsResultLauncher.launch(permissions)
        return this
    }

    interface OnPermissionResult {
        fun onPermissionGranted()
        fun onPermissionDenied(neverAskAgain: Boolean = false)
    }
}