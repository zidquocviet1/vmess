package com.mqv.vmess.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.PhoneContactViewModel
import com.mqv.vmess.databinding.FragmentPhoneContactBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.PhoneContactAdapter
import com.mqv.vmess.ui.permissions.Permission
import com.mqv.vmess.util.NetworkStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneContactFragment : BaseFragment<PhoneContactViewModel, FragmentPhoneContactBinding>() {
    private lateinit var mAdapter: PhoneContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(mBinding) {
            mAdapter = PhoneContactAdapter()
            mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler{
                override fun onListItemSizeChanged(size: Int) {
                    if (size == 0) {
                        textViewEmpty.visibility = View.VISIBLE
                        imageEmpty.visibility = View.VISIBLE
                        recyclerPhoneContact.visibility = View.GONE
                    } else {
                        textViewEmpty.visibility = View.GONE
                        imageEmpty.visibility = View.GONE
                        recyclerPhoneContact.visibility = View.VISIBLE
                    }
                }
            })
            mAdapter.registerOnButtonClickListener { pos ->
                mViewModel.triggerButtonClick(pos, mAdapter.currentList[pos])
            }
            recyclerPhoneContact.adapter = mAdapter
            recyclerPhoneContact.layoutManager = LinearLayoutManager(requireContext())

            if (requireContext().checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                buttonGrant.visibility = View.GONE
                textView.visibility = View.GONE
                imageContact.visibility = View.GONE
                recyclerPhoneContact.visibility = View.VISIBLE

                mViewModel.getPhoneContactList(requireContext())
            } else {
                buttonGrant.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
                imageContact.visibility = View.VISIBLE
                recyclerPhoneContact.visibility = View.GONE
            }

            buttonGrant.setOnClickListener {
                Permission.with(this@PhoneContactFragment, mPermissionLauncher)
                    .ifNecessary()
                    .request(Manifest.permission.READ_CONTACTS)
                    .onAllGranted {
                        mViewModel.getPhoneContactList(requireContext())

                        buttonGrant.visibility = View.GONE
                        textView.visibility = View.GONE
                        imageContact.visibility = View.GONE
                    }
                    .withRationaleDialog(
                        getString(R.string.msg_permission_use_contact_rationale),
                        R.drawable.ic_round_contacts
                    )
                    .withPermanentDenialDialog(
                        getString(R.string.msg_permission_permanent_denial_contact_title),
                        getString(R.string.msg_permission_permanent_denial_contact_message),
                        getString(
                            R.string.msg_permission_settings_construction,
                            getString(R.string.title_contacts)
                        )
                    )
                    .execute()
            }
        }
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentPhoneContactBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<PhoneContactViewModel> =
        PhoneContactViewModel::class.java

    override fun setupObserver() {
        mViewModel.phoneContacts.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it.status) {
                    NetworkStatus.LOADING -> {
                        with(mBinding) {
                            progressBarLoading.show()
                            recyclerPhoneContact.visibility = View.GONE
                        }
                    }
                    NetworkStatus.SUCCESS -> {
                        with(mBinding) {
                            progressBarLoading.hide()
                            recyclerPhoneContact.visibility = View.VISIBLE

                            mAdapter.submitList(result.success)
                        }
                    }
                    else -> {
                        with(mBinding) {
                            progressBarLoading.hide()
                            recyclerPhoneContact.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        mViewModel.friendRequestResult.observe(viewLifecycleOwner) { map ->
            map.onEachIndexed { index, entry ->
                val result = entry.value
                when (result.status) {
                    NetworkStatus.LOADING -> mAdapter.notifyItemChanged(index, PhoneContactAdapter.PAYLOAD_LOADING)
                    NetworkStatus.SUCCESS -> mAdapter.notifyItemChanged(index, PhoneContactAdapter.PAYLOAD_STOP_LOADING)
                    else -> mAdapter.notifyItemChanged(index, PhoneContactAdapter.PAYLOAD_STOP_LOADING)
                }
            }
        }
    }

    companion object {
        val TAG: String = PhoneContactFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PhoneContactFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}