package vc.prog3c.poe.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.opsc6311.poe.databinding.FragmentAccountsBinding
import vc.prog3c.poe.ui.adapters.AccountAdapter
import vc.prog3c.poe.ui.viewmodels.DashboardViewModel

class AccountsFragment : Fragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private lateinit var accountAdapter: AccountAdapter
    private lateinit var model: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        model = (activity as DashboardView).model // Get the ViewModel from the activity

        setupRecyclerView()
        observeAccounts()

        return binding.root
    }

    private fun setupRecyclerView() {
        accountAdapter = AccountAdapter(
            onItemClick = { account ->
                // Handle account click
            },
            onLongPress = { account ->
                // Handle long press (e.g., show delete dialog)
            }
        )
        binding.dashboardAccountsRecyclerView.apply {
            adapter = accountAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun observeAccounts() {
        model.accounts.observe(viewLifecycleOwner) { accounts ->
            accountAdapter.submitList(accounts)
            binding.noAccountsText.visibility = if (accounts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
