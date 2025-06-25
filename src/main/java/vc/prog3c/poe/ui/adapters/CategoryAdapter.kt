package vc.prog3c.poe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vc.prog3c.poe.R
import vc.prog3c.poe.data.models.Category
import vc.prog3c.poe.data.models.CategoryType
import vc.prog3c.poe.core.utils.CurrencyFormatter
import java.util.Locale
import de.hdodenhof.circleimageview.CircleImageView

class CategoryAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit,
    private val isDashboard: Boolean = false
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var categoryTotals: Map<String, Double> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateCategoryTotals(totals: Map<String, Double>) {
        categoryTotals = totals
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.categoryName)
        private val typeTextView: TextView = itemView.findViewById(R.id.categoryType)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.categoryDescription)
        private val minBudgetTextView: TextView = itemView.findViewById(R.id.categoryMinBudget)
        private val maxBudgetTextView: TextView = itemView.findViewById(R.id.categoryMaxBudget)
        private val iconImageView: CircleImageView = itemView.findViewById(R.id.categoryIcon)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(category: Category) {
            nameTextView.text = category.name
            typeTextView.text = category.type.name
            descriptionTextView.text = category.description
            descriptionTextView.visibility = if (category.description.isNullOrEmpty()) View.GONE else View.VISIBLE
            
            // Format budget amounts with R currency
            minBudgetTextView.text = "Min: R${String.format("%.2f", category.minBudget)}"
            maxBudgetTextView.text = "Max: R${String.format("%.2f", category.maxBudget)}"

            // Show budget info only if budgets are set
            val hasBudget = category.minBudget > 0 || category.maxBudget > 0
            minBudgetTextView.visibility = if (category.minBudget > 0) View.VISIBLE else View.GONE
            maxBudgetTextView.visibility = if (category.maxBudget > 0) View.VISIBLE else View.GONE

            // Set icon based on category type
            val iconResId = when (category.type) {
                CategoryType.SAVINGS -> R.drawable.ic_savings
                CategoryType.INCOME -> R.drawable.ic_income
                CategoryType.EXPENSE -> R.drawable.ic_expense
                CategoryType.UTILITIES -> R.drawable.ic_utilities
                CategoryType.FOOD -> R.drawable.ic_food
                CategoryType.TRANSPORT -> R.drawable.ic_transport
                CategoryType.SHOPPING -> R.drawable.ic_shopping
                CategoryType.ENTERTAINMENT -> R.drawable.ic_entertainment
                CategoryType.HEALTH -> R.drawable.ic_health
                CategoryType.EMERGENCY -> R.drawable.ic_emergency
                CategoryType.INVESTING -> R.drawable.ic_investing
                else -> R.drawable.ic_category
            }
            iconImageView.setImageResource(iconResId)

            // Set color based on category type
            val colorResId = when (category.type) {
                CategoryType.SAVINGS -> R.color.colorGreen
                CategoryType.INCOME -> R.color.colorGreen
                CategoryType.EXPENSE -> R.color.colorRed
                CategoryType.UTILITIES -> R.color.colorBlue
                CategoryType.FOOD -> R.color.colorOrange
                CategoryType.TRANSPORT -> R.color.colorBlue
                CategoryType.SHOPPING -> R.color.colorPurple
                CategoryType.ENTERTAINMENT -> R.color.colorPurple
                CategoryType.HEALTH -> R.color.colorGreen
                CategoryType.EMERGENCY -> R.color.colorRed
                CategoryType.INVESTING -> R.color.colorGreen
                else -> R.color.colorGreen
            }
            iconImageView.circleBackgroundColor = ContextCompat.getColor(itemView.context, colorResId)

            // Set status indicator color
            statusIndicator.setBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (category.isActive) R.color.status_success else R.color.status_error
                )
            )

            // Set edit and delete button visibility
            editButton.visibility = if (!isDashboard && category.isEditable) View.VISIBLE else View.GONE
            deleteButton.visibility = if (!isDashboard && category.isEditable) View.VISIBLE else View.GONE

            editButton.setOnClickListener { onEditClick(category) }
            deleteButton.setOnClickListener { onDeleteClick(category) }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
