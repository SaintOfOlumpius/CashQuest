package com.opsc6311.poe.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.opsc6311.poe.data.models.SavingsGoal
import com.opsc6311.poe.databinding.ItemSavingsGoalBinding

class SavingsGoalAdapter : ListAdapter<SavingsGoal, SavingsGoalAdapter.SavingsGoalViewHolder>(SavingsGoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsGoalViewHolder {
        val binding = ItemSavingsGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavingsGoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavingsGoalViewHolder, position: Int) {
        val savingsGoal = getItem(position)
        holder.bind(savingsGoal)
    }

    inner class SavingsGoalViewHolder(private val binding: ItemSavingsGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(savingsGoal: SavingsGoal) {
            binding.goalNameTextView.text = savingsGoal.name
            binding.goalAmountTextView.text = "Goal: ${savingsGoal.targetAmount}"
            binding.currentAmountTextView.text = "Current: ${savingsGoal.savedAmount}"
            binding.progressBar.progress = (savingsGoal.savedAmount / savingsGoal.targetAmount * 100).toInt()
        }
    }

    class SavingsGoalDiffCallback : DiffUtil.ItemCallback<SavingsGoal>() {
        override fun areItemsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean {
            return oldItem == newItem
        }
    }
}
