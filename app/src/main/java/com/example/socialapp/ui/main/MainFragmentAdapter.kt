package com.example.socialapp.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.socialapp.ui.downloads.DownloadsFragment
import com.example.socialapp.ui.home.HomeFragment
import com.example.socialapp.ui.popular.PopularFragment
import com.example.socialapp.ui.topics.TopicsFragment
import timber.log.Timber

class MainFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                HomeFragment()
            }

            1 -> {
                PopularFragment()
            }

            2 -> {
                TopicsFragment()
            }

            else -> {
                DownloadsFragment()
            }
        }
    }
}