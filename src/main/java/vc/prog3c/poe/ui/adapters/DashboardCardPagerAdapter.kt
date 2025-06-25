package vc.prog3c.poe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import vc.prog3c.poe.R
import vc.prog3c.poe.core.utils.CurrencyFormatter
import vc.prog3c.poe.data.models.*
import vc.prog3c.poe.databinding.CardBudgetBinding
import vc.prog3c.poe.databinding.CardSavingsBinding
import vc.prog3c.poe.databinding.CardAccountsBinding
import java.util.*

class DashboardCardPagerAdapter(
    private val onBudgetCardClick: (() -> Unit)? = null,
    private val onSavingsCardClick: (() -> Unit)? = null,
    private val onAccountsCardClick: (() -> Unit)? = null,
    private val onManageGoalsClick: (() -> Unit)? = null,
    private val onContributeClick: (() -> Unit)? = null,
    private val onAddAccountClick: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_BUDGET = 0
        private const val VIEW_TYPE_SAVINGS = 1
        private const val VIEW_TYPE_ACCOUNTS = 2
    }

    private var budget: Budget? = null
    private var monthlyStats: MonthlyStats? = null
    private var savingsGoals: List<SavingsGoal> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var accountAdapter: AccountAdapter? = null

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_BUDGET
            1 -> VIEW_TYPE_SAVINGS
            2 -> VIEW_TYPE_ACCOUNTS
            else -> VIEW_TYPE_BUDGET
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BUDGET -> {
                val binding = CardBudgetBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                BudgetCardViewHolder(binding, onBudgetCardClick)
            }
            VIEW_TYPE_SAVINGS -> {
                val binding = CardSavingsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                SavingsCardViewHolder(binding, onSavingsCardClick, onManageGoalsClick, onContributeClick)
            }
            VIEW_TYPE_ACCOUNTS -> {
                val binding = CardAccountsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                binding.root.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                AccountsCardViewHolder(binding, onAccountsCardClick, onAddAccountClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BudgetCardViewHolder -> holder.bind(budget, monthlyStats)
            is SavingsCardViewHolder -> holder.bind(savingsGoals)
            is AccountsCardViewHolder -> holder.bind(accounts, accountAdapter)
        }
    }

    override fun getItemCount(): Int = 3

    fun updateBudgetData(budget: Budget?, stats: MonthlyStats?) {
        this.budget = budget
        this.monthlyStats = stats
        notifyItemChanged(0)
    }

    fun updateSavingsData(goals: List<SavingsGoal>) {
        this.savingsGoals = goals
        notifyItemChanged(1)
    }

    fun updateAccountsData(accounts: List<Account>, adapter: AccountAdapter?) {
        this.accounts = accounts
        this.accountAdapter = adapter
        notifyItemChanged(2)
    }

    class BudgetCardViewHolder(
        val binding: CardBudgetBinding,
        private val onCardClick: (() -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onCardClick?.invoke() }
        }

        fun bind(budget: Budget?, stats: MonthlyStats?) {
            val spent = stats?.totalExpenses ?: 0.0
            val max = budget?.max ?: 1.0
            val min = budget?.min ?: 0.0

            binding.budgetAmountText.text = CurrencyFormatter.format(budget?.max ?: 0.0)
            binding.budgetMonthText.text = budget?.let {
                try {
                    val monthName = java.time.Month.of(it.month)
                        .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                    "$monthName ${it.year}"
                } catch (_: Exception) {
                    ""
                }
            } ?: ""

            binding.budgetSpentText.text = CurrencyFormatter.format(spent)
            val percent = (spent / max * 100).toInt().coerceIn(0, 100)
            binding.budgetProgressBar.progress = percent
            binding.budgetProgressText.text = "$percent%"

            val spentColor = when {
                spent < min -> R.color.teal_200
                spent <= max -> R.color.white
                else -> R.color.red
            }
            binding.budgetSpentText.setTextColor(
                binding.root.context.getColor(spentColor)
            )
        }
    }

    class SavingsCardViewHolder(
        val binding: CardSavingsBinding,
        private val onCardClick: (() -> Unit)?,
        private val onManageGoalsClick: (() -> Unit)?,
        private val onContributeClick: (() -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onCardClick?.invoke() }
            binding.manageGoalsButton.setOnClickListener { onManageGoalsClick?.invoke() }
            binding.contributeButton.setOnClickListener { 
                android.util.Log.d("DashboardCard", "Contribute button clicked")
                onContributeClick?.invoke() 
            }
        }

        fun bind(goals: List<SavingsGoal>) {
            if (goals.isNotEmpty()) {
                val goal = goals[0]
                val progress = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
                val percent = (progress * 100).toInt().coerceIn(0, 100)

                binding.savingsGoalText.text = "${goal.name}: ${CurrencyFormatter.format(goal.savedAmount)} / ${CurrencyFormatter.format(goal.targetAmount)}"
                binding.currentSavingsText.text = CurrencyFormatter.format(goal.savedAmount)
                binding.maxSavingsText.text = CurrencyFormatter.format(goal.targetAmount)
                binding.savingsPercentageText.text = "$percent%"
                binding.savingsProgressBar.progress = percent

                binding.savingsGoalDate.text = goal.targetDate?.let {
                    val dateFormat = android.text.format.DateFormat.getMediumDateFormat(binding.root.context)
                    "Target date: ${dateFormat.format(it)}"
                } ?: ""

                // Always enable the contribute button for now
                binding.contributeButton.isEnabled = true
                android.util.Log.d("DashboardCard", "Contribute button enabled: ${binding.contributeButton.isEnabled}")
            } else {
                binding.savingsGoalText.text = binding.root.context.getString(R.string.no_savings_goals)
                binding.currentSavingsText.text = CurrencyFormatter.format(0)
                binding.maxSavingsText.text = CurrencyFormatter.format(0)
                binding.savingsPercentageText.text = "0%"
                binding.savingsProgressBar.progress = 0
                binding.savingsGoalDate.text = ""
                binding.contributeButton.isEnabled = false
            }
        }
    }

    class AccountsCardViewHolder(
        val binding: CardAccountsBinding,
        private val onCardClick: (() -> Unit)?,
        private val onAddAccountClick: (() -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onCardClick?.invoke() }
            binding.addAccountButton.setOnClickListener { onAddAccountClick?.invoke() }
        }

        fun bind(accounts: List<Account>, adapter: AccountAdapter?) {
            if (accounts.isNotEmpty()) {
                binding.noAccountsText.visibility = View.GONE
                binding.dashboardAccountsRecyclerView.visibility = View.VISIBLE
                
                adapter?.let { accountAdapter ->
                    binding.dashboardAccountsRecyclerView.apply {
                        this.adapter = accountAdapter
                        layoutManager = LinearLayoutManager(binding.root.context)
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = true
                        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                    }
                    accountAdapter.submitList(accounts)
                }
            } else {
                binding.noAccountsText.visibility = View.VISIBLE
                binding.dashboardAccountsRecyclerView.visibility = View.GONE
            }
        }
    }
} 