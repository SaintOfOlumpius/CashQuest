package com.opsc6311.poe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.opsc6311.poe.R
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.data.models.TransactionType
import com.opsc6311.poe.core.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: ((Transaction) -> Unit)? = null
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.tv_cost)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(position))
                    true
                } else {
                    false
                }
            }
        }

        fun bind(transaction: Transaction) {
            // Set description with fallback
            descriptionTextView.text = transaction.description.ifEmpty { "No description" }
            
            // Format amount with currency
            amountTextView.text = CurrencyFormatter.format(transaction.amount)
            
            // Format date nicely
            dateTextView.text = dateFormat.format(transaction.date.toDate())
            
            // Set category with fallback
            categoryTextView.text = transaction.category.ifEmpty { "Uncategorized" }

            // Set text color based on transaction type
            amountTextView.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (transaction.type == TransactionType.INCOME) R.color.income_color else R.color.expense_color
                )
            )
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
} 
