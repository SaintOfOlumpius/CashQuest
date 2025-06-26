package com.opsc6311.poe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.opsc6311.poe.R
import com.opsc6311.poe.databinding.CardAccountsBinding
import com.opsc6311.poe.databinding.CardSavingsGoalsBinding
import com.opsc6311.poe.data.models.Account
import com.opsc6311.poe.data.models.SavingsGoal
import com.opsc6311.poe.core.utils.CurrencyFormatter

class CardsPagerAdapter(
    private val onSavingsGoalClick: (SavingsGoal) -> Unit = {},
    private val onManageGoalsClick: () -> Unit = {},
    private val onContributeClick: () -> Unit = {},
    private val onAccountClick: (Account) -> Unit = {},
    private val onAddAccountClick: () -> Unit = {},
    private val onViewAllAccountsClick: () -> Unit = {},
    private val onTransferClick: () -> Unit = {},
    private val onAccountLongPress: (Account) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SAVINGS = 0
        private const val VIEW_TYPE_ACCOUNTS = 1
    }

    private var savingsGoals: List<SavingsGoal> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var accountAdapter: AccountAdapter? = null

    fun updateSavingsGoals(goals: List<SavingsGoal>) {
        savingsGoals = goals
        notifyItemChanged(VIEW_TYPE_SAVINGS)
    }

    fun updateAccounts(accountsList: List<Account>) {
        accounts = accountsList
        accountAdapter?.submitList(accountsList)
        notifyItemChanged(VIEW_TYPE_ACCOUNTS)
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_SAVINGS
            1 -> VIEW_TYPE_ACCOUNTS
            else -> VIEW_TYPE_SAVINGS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SAVINGS -> {
                val binding = CardSavingsGoalsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SavingsGoalsViewHolder(binding)
            }
            VIEW_TYPE_ACCOUNTS -> {
                val binding = CardAccountsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AccountsViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SavingsGoalsViewHolder -> holder.bind(savingsGoals)
            is AccountsViewHolder -> holder.bind(accounts)
        }
    }

    override fun getItemCount(): Int = 2

    inner class SavingsGoalsViewHolder(
        private val binding: CardSavingsGoalsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goals: List<SavingsGoal>) {
            if (goals.isNotEmpty()) {
                val goal = goals[0]
                val progress = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
                val percent = (progress * 100).toInt().coerceIn(0, 100)

                binding.savingsGoalTitle.text = "Savings Goals"
                binding.savingsGoalText.text = "Track your progress"
                binding.currentSavingsText.text = CurrencyFormatter.format(goal.savedAmount)
                binding.maxSavingsText.text = CurrencyFormatter.format(goal.targetAmount)
                binding.savingsPercentageText.text = "$percent%"
                binding.savingsProgressBar.progress = percent

                binding.savingsGoalDate.text = goal.targetDate?.let {
                    val dateFormat = android.text.format.DateFormat.getMediumDateFormat(itemView.context)
                    "Target: ${dateFormat.format(it)}"
                } ?: ""

                binding.contributeButton.isEnabled = goal.savedAmount < goal.targetAmount
            } else {
                binding.savingsGoalTitle.text = "Savings Goals"
                binding.savingsGoalText.text = "No goals set"
                binding.currentSavingsText.text = CurrencyFormatter.format(0)
                binding.maxSavingsText.text = CurrencyFormatter.format(0)
                binding.savingsPercentageText.text = "0%"
                binding.savingsProgressBar.progress = 0
                binding.savingsGoalDate.text = ""
                binding.contributeButton.isEnabled = false
            }

            setupClickListeners()
        }

        private fun setupClickListeners() {
            binding.manageGoalsButton.setOnClickListener { onManageGoalsClick() }
            binding.contributeButton.setOnClickListener { onContributeClick() }
        }
    }

    inner class AccountsViewHolder(
        private val binding: CardAccountsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(accountsList: List<Account>) {
            val totalBalance = accountsList.sumOf { it.balance }
            binding.totalBalanceText.text = CurrencyFormatter.format(totalBalance)
            binding.accountsCountText.text = accountsList.size.toString()

            if (accountsList.isEmpty()) {
                binding.emptyAccountsState.visibility = View.VISIBLE
                binding.dashboardAccountsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyAccountsState.visibility = View.GONE
                binding.dashboardAccountsRecyclerView.visibility = View.VISIBLE
                
                // Setup RecyclerView if not already done
                if (accountAdapter == null) {
                    accountAdapter = AccountAdapter(
                        onItemClick = onAccountClick,
                        onLongPress = onAccountLongPress
                    )
                    
                    binding.dashboardAccountsRecyclerView.apply {
                        adapter = accountAdapter
                        layoutManager = LinearLayoutManager(itemView.context)
                        setHasFixedSize(true)
                    }
                }
                
                accountAdapter?.submitList(accountsList)
            }

            setupClickListeners()
        }

        private fun setupClickListeners() {
            binding.addAccountButton.setOnClickListener { onAddAccountClick() }
            binding.viewAllAccountsButton.setOnClickListener { onViewAllAccountsClick() }
            binding.transferButton.setOnClickListener { onTransferClick() }
        }
    }
} 