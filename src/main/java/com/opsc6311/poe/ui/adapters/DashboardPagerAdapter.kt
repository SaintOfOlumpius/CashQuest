// DashboardPagerAdapter.kt
package com.opsc6311.poe.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.opsc6311.poe.ui.views.SavingsGoalsFragment
import com.opsc6311.poe.ui.views.AccountsFragment

class DashboardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2 // Two pages: Savings Goals and Accounts

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SavingsGoalsFragment() // Fragment for Savings Goals
            1 -> AccountsFragment() // Fragment for Accounts
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
