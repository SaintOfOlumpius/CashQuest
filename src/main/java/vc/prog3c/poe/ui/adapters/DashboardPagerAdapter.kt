// DashboardPagerAdapter.kt
package vc.prog3c.poe.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import vc.prog3c.poe.ui.views.SavingsGoalsFragment
import vc.prog3c.poe.ui.views.AccountsFragment

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
