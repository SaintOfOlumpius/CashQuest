package com.opsc6311.poe.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.opsc6311.poe.data.models.Account
import com.opsc6311.poe.databinding.ItemAccountBinding
import com.opsc6311.poe.core.utils.CurrencyFormatter

class AccountAdapter(
    private val onItemClick: (Account) -> Unit,
    private val onLongPress: (Account) -> Unit
) : ListAdapter<Account, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        holder.bind(account)
    }

    inner class AccountViewHolder(private val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val account = getItem(position)
                    onItemClick(account)
                }
            }
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val account = getItem(position)
                    onLongPress(account)
                }
                true
            }
            
            // Add click listener for the view button
            binding.viewAccountButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val account = getItem(position)
                    onItemClick(account)
                }
            }
        }

        fun bind(account: Account) {
            binding.accountNameTextView.text = account.name
            binding.accountBalanceTextView.text = CurrencyFormatter.format(account.balance)
            binding.accountTransactionsTextView.text = "${account.transactionsCount} transactions"
            
            // Set account type icon based on account type
            val iconRes = when (account.type.lowercase()) {
                "checking" -> com.opsc6311.poe.R.drawable.ic_account_balance
                "savings" -> com.opsc6311.poe.R.drawable.ic_savings
                "credit card" -> com.opsc6311.poe.R.drawable.ic_credit_card
                "investment" -> com.opsc6311.poe.R.drawable.ic_investing
                "cash" -> com.opsc6311.poe.R.drawable.ic_money
                else -> com.opsc6311.poe.R.drawable.ic_account_balance
            }
            binding.accountIcon.setImageResource(iconRes)
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }
}
