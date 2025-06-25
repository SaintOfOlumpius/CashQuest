package vc.prog3c.poe.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vc.prog3c.poe.core.utils.CurrencyFormatter
import vc.prog3c.poe.data.models.Account
import vc.prog3c.poe.databinding.ItemAccountBinding

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
                val account = getItem(adapterPosition)
                onItemClick(account)
            }
            binding.root.setOnLongClickListener {
                val account = getItem(adapterPosition)
                android.util.Log.d("AccountAdapter", "Long press detected for account: ${account.name}")
                onLongPress(account)
                true
            }
        }

        fun bind(account: Account) {
            binding.accountNameTextView.text = account.name
            binding.accountBalanceTextView.text = CurrencyFormatter.format(account.balance)
            
            // Set transaction count (placeholder for now)
            binding.accountTransactionsTextView.text = "0 transactions"
            
            // Set account icon based on account type (you can customize this)
            binding.accountIcon.setImageResource(
                when {
                    account.name.contains("Savings", ignoreCase = true) -> 
                        vc.prog3c.poe.R.drawable.ic_savings
                    account.name.contains("Credit", ignoreCase = true) -> 
                        vc.prog3c.poe.R.drawable.ic_credit_card
                    else -> vc.prog3c.poe.R.drawable.ic_account_balance
                }
            )
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
