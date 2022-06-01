package com.mqv.vmess.activity

import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ActivityReportProblemBinding

class ReportProblemActivity : ToolbarActivity<AndroidViewModel, ActivityReportProblemBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.title_pref_report_problem)
    }

    override fun binding() {
        mBinding = ActivityReportProblemBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel>? = null

    override fun setupObserver() {
    }
}