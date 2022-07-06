package com.mqv.vmess.activity

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ActivityLoginDemoBinding
import com.mqv.vmess.ui.fragment.QrCodeScannerFragment

class LoginDemoActivity : BaseActivity<AndroidViewModel, ActivityLoginDemoBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.findNavController()
        navController.setGraph(
            R.navigation.nav_demo_login, bundleOf(
                QrCodeScannerFragment.KEY_SCAN_FOR_LOGIN to true
            )
        )
    }

    override fun binding() {
        mBinding = ActivityLoginDemoBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel>? = null

    override fun setupObserver() {
    }
}