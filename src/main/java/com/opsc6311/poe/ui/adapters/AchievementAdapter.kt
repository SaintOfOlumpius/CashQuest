package com.opsc6311.poe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.opsc6311.poe.R
import com.opsc6311.poe.data.models.Achievement
import com.opsc6311.poe.data.models.AchievementCategory
import java.text.SimpleDateFormat
import java.util.*

class AchievementAdapter(
    private val onAchievementClick: (Achievement) -> Unit,
    private val onRedeemClick: (Achievement) -> Unit
) : ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.achievementTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.achievementDescription)
        private val categoryText: TextView = itemView.findViewById(R.id.achievementCategory)
        private val coinsText: TextView = itemView.findViewById(R.id.questCoinsReward)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val completedDateText: TextView = itemView.findViewById(R.id.completedDateText)
        private val redeemButton: TextView = itemView.findViewById(R.id.redeemButton)
        private val achievementCard: View = itemView.findViewById(R.id.achievementCard)
        private val categoryIcon: TextView = itemView.findViewById(R.id.categoryIcon)

        fun bind(achievement: Achievement) {
            titleText.text = achievement.title
            descriptionText.text = achievement.description
            categoryText.text = getCategoryDisplayName(achievement.category)
            coinsText.text = "${achievement.questCoinsReward} coins"
            
            // Set category icon
            categoryIcon.text = getCategoryIcon(achievement.category)
            
            // Handle progress display
            if (achievement.requiredProgress != null && achievement.requiredProgress > 1) {
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                
                val progress = achievement.progress
                val maxProgress = achievement.requiredProgress
                
                progressBar.max = maxProgress
                progressBar.progress = progress
                progressText.text = "$progress/$maxProgress"
                
                // Update progress bar color based on completion
                val progressColor = if (achievement.isCompleted) {
                    ContextCompat.getColor(itemView.context, R.color.colorSuccess)
                } else if (progress > 0) {
                    ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.colorGray)
                }
                progressBar.progressTintList = ContextCompat.getColorStateList(itemView.context, progressColor)
                
            } else {
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
            }
            
            // Handle status and completion
            when {
                achievement.isCompleted -> {
                    statusText.text = "Completed"
                    statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSuccess))
                    redeemButton.visibility = View.VISIBLE
                    completedDateText.visibility = View.VISIBLE
                    
                    achievement.completedAt?.let { timestamp ->
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        completedDateText.text = "Completed on ${dateFormat.format(timestamp.toDate())}"
                    }
                    
                    // Apply completed styling
                    achievementCard.setBackgroundResource(R.drawable.achievement_completed_background)
                    
                }
                achievement.progress > 0 -> {
                    statusText.text = "In Progress"
                    statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
                    redeemButton.visibility = View.GONE
                    completedDateText.visibility = View.GONE
                    
                    // Apply in-progress styling
                    achievementCard.setBackgroundResource(R.drawable.achievement_in_progress_background)
                    
                }
                else -> {
                    statusText.text = "Locked"
                    statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorGray))
                    redeemButton.visibility = View.GONE
                    completedDateText.visibility = View.GONE
                    
                    // Apply locked styling
                    achievementCard.setBackgroundResource(R.drawable.achievement_locked_background)
                }
            }
            
            // Set up click listeners
            itemView.setOnClickListener {
                onAchievementClick(achievement)
            }
            
            redeemButton.setOnClickListener {
                onRedeemClick(achievement)
            }
            
            // Add animation for newly completed achievements
            if (achievement.isCompleted && achievement.completedAt != null) {
                val timeSinceCompletion = System.currentTimeMillis() - achievement.completedAt.toDate().time
                if (timeSinceCompletion < 24 * 60 * 60 * 1000) { // Within 24 hours
                    itemView.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(200)
                        .withEndAction {
                            itemView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                }
            }
        }
        
        private fun getCategoryDisplayName(category: AchievementCategory): String {
            return when (category) {
                AchievementCategory.USER_MILESTONES -> "User Milestones"
                AchievementCategory.CONSISTENCY_HABITS -> "Consistency & Habits"
                AchievementCategory.SAVINGS_ACHIEVEMENTS -> "Savings Achievements"
                AchievementCategory.BUDGET_MANAGEMENT -> "Budget Management"
                AchievementCategory.FINANCIAL_INSIGHT -> "Financial Insight"
                AchievementCategory.LEARNING_GROWTH -> "Learning & Growth"
            }
        }
        
        private fun getCategoryIcon(category: AchievementCategory): String {
            return when (category) {
                AchievementCategory.USER_MILESTONES -> "🎯"
                AchievementCategory.CONSISTENCY_HABITS -> "📅"
                AchievementCategory.SAVINGS_ACHIEVEMENTS -> "💰"
                AchievementCategory.BUDGET_MANAGEMENT -> "📊"
                AchievementCategory.FINANCIAL_INSIGHT -> "🔍"
                AchievementCategory.LEARNING_GROWTH -> "📚"
            }
        }
    }

    private class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
}
