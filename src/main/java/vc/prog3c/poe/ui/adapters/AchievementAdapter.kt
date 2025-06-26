package vc.prog3c.poe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.opsc6311.poe.R
import vc.prog3c.poe.data.models.Achievement
import vc.prog3c.poe.data.models.AchievementCategory

class AchievementAdapter(
    private var fullList: List<Achievement>,
    private val onAchievementClick: (Achievement) -> Unit
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    private var filteredList: List<Achievement> = fullList

    fun updateAchievements(newAchievements: List<Achievement>) {
        fullList = newAchievements
        filteredList = newAchievements
        notifyDataSetChanged()
    }

    fun filterByCategory(category: AchievementCategory?) {
        filteredList = if (category == null) {
            fullList
        } else {
            fullList.filter { it.category == category }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount() = filteredList.size

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.achievementIcon)
        private val title: TextView = itemView.findViewById(R.id.achievementTitle)
        private val description: TextView = itemView.findViewById(R.id.achievementDescription)
        private val progressIndicator: LinearProgressIndicator = itemView.findViewById(R.id.progressIndicator)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val boosterBucksReward: TextView = itemView.findViewById(R.id.boosterBucksReward)
        private val categoryChip: TextView = itemView.findViewById(R.id.categoryChip)
        private val completionIndicator: ImageView = itemView.findViewById(R.id.completionIndicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAchievementClick(filteredList[position])
                }
            }
        }

        fun bind(achievement: Achievement) {
            val context = itemView.context
            
            // Set basic information
            title.text = achievement.title
            description.text = achievement.description
            boosterBucksReward.text = achievement.boosterBucksReward.toString()

            // Calculate progress
            val progress = (achievement.progress.toFloat() / achievement.requiredProgress.toFloat() * 100).toInt()
            progressText.text = "${achievement.progress}/${achievement.requiredProgress}"
            progressIndicator.progress = progress

            // Set category
            categoryChip.text = getCategoryDisplayName(achievement.category)

            // Handle completion state
            if (achievement.isCompleted) {
                // Show completion indicator
                completionIndicator.visibility = View.VISIBLE
                
                // Set completed icon
                icon.setImageResource(R.drawable.ic_achievement_completed)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
                
                // Update progress bar color
                progressIndicator.setIndicatorColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
                
                // Update title color
                title.setTextColor(ContextCompat.getColor(context, R.color.primary))
                
            } else {
                // Hide completion indicator
                completionIndicator.visibility = View.GONE
                
                // Set default icon
                icon.setImageResource(R.drawable.ic_achievement)
                icon.clearColorFilter()
                
                // Update progress bar color based on progress
                val progressColor = when {
                    progress >= 80 -> R.color.primary
                    progress >= 50 -> R.color.orange
                    else -> R.color.progress_background
                }
                progressIndicator.setIndicatorColor(ContextCompat.getColor(context, progressColor))
                
                // Reset title color
                title.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }

        private fun getCategoryDisplayName(category: AchievementCategory): String {
            return when (category) {
                AchievementCategory.USER_MILESTONES -> "Milestone"
                AchievementCategory.CONSISTENCY_HABITS -> "Habit"
                AchievementCategory.SAVINGS_ACHIEVEMENTS -> "Savings"
                AchievementCategory.BUDGET_MANAGEMENT -> "Budget"
                AchievementCategory.FINANCIAL_INSIGHT -> "Insight"
                AchievementCategory.LEARNING_GROWTH -> "Learning"
            }
        }
    }
}
