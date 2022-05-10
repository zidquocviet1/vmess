package com.mqv.vmess.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mqv.vmess.ui.fragment.pager.AllMediaListFragment
import com.mqv.vmess.ui.fragment.pager.LinkListFragment
import com.mqv.vmess.ui.fragment.pager.MediaListFragment

class MediaAndFileStateAdapter(fm: FragmentManager, viewLifecycle: Lifecycle) : FragmentStateAdapter(fm, viewLifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MediaListFragment()
            1 -> LinkListFragment()
            else -> AllMediaListFragment()
        }
    }
}