package vc.prog3c.poe.ui.views

import android.content.*
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import vc.prog3c.poe.R
import vc.prog3c.poe.core.utils.CurrencyFormatter
import vc.prog3c.poe.data.models.*
import vc.prog3c.poe.databinding.ActivityDashboardBinding
import vc.prog3c.poe.ui.adapters.AccountAdapter
import vc.prog3c.poe.ui.adapters.DashboardCardPagerAdapter
import vc.prog3c.poe.ui.adapters.DashboardPagerAdapter
import vc.prog3c.poe.ui.viewmodels.AchievementViewModel
import vc.prog3c.poe.ui.viewmodels.DashboardUiState
import vc.prog3c.poe.ui.viewmodels.DashboardViewModel
import java.util.*

class DashboardView : AppCompatActivity(), View.OnClickListener {

    private lateinit var binds: ActivityDashboardBinding
    lateinit var model: DashboardViewModel
    private lateinit var achievementViewModel: AchievementViewModel
    private lateinit var dashboardAccountAdapter: AccountAdapter
    private lateinit var dashboardCardPagerAdapter: DashboardCardPagerAdapter

    private val profilePictureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "PROFILE_PICTURE_UPDATED") {
                val base64Image = intent.getStringExtra("profile_picture")
                if (base64Image != null) {
                    updateProfilePicture(base64Image)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBindings()
        setupLayoutUi()
        setupClickListeners()
        setupCardPager()

        model = ViewModelProvider(this)[DashboardViewModel::class.java]
        achievementViewModel = ViewModelProvider(this)[AchievementViewModel::class.java]

        setupBottomNavigation()
        setupDashboardAccountsRecyclerView()
        setupSwipeRefresh()

        observeViewModel()
        model.refreshData()

        achievementViewModel.newlyCompleted.observe(this) { event ->
            event.getContentIfNotHandled()?.let { achievement ->
                showAchievementSnackbar(achievement)
            }
        }

        val filter = IntentFilter("PROFILE_PICTURE_UPDATED")
        registerReceiver(profilePictureReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(profilePictureReceiver)
    }

    override fun onResume() {
        super.onResume()
        model.refreshData()
    }

    private fun setupCardPager() {
        dashboardCardPagerAdapter = DashboardCardPagerAdapter(
            onBudgetCardClick = { /* Handle budget card click */ },
            onSavingsCardClick = { /* Handle savings card click */ },
            onAccountsCardClick = { /* Handle accounts card click */ },
            onManageGoalsClick = { startActivity(Intent(this, ManageGoalsActivity::class.java)) },
            onContributeClick = { 
                // Handle contribute click - show contribution dialog
                showContributeDialog()
            },
            onAddAccountClick = { showAddAccountDialog() }
        )

        binds.dashboardCardsViewPager.apply {
            adapter = dashboardCardPagerAdapter
            orientation = androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
            // Remove page transformer for smoother swiping
            
            // Configure for nested scrolling
            (getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                isNestedScrollingEnabled = true
            }
        }

        // Setup page indicator
        TabLayoutMediator(binds.cardPageIndicator, binds.dashboardCardsViewPager) { _, _ ->
            // This lambda is called for each tab, but we don't need to set text
        }.attach()
    }

    private fun updateProfilePicture(base64Image: String) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binds.profileImage.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun observeViewModel() = model.uiState.observe(this) { state ->
        when (state) {
            is DashboardUiState.Default -> {
                binds.swipeRefreshLayout.isRefreshing = false
            }
            is DashboardUiState.Loading -> {
                binds.swipeRefreshLayout.isRefreshing = true
            }
            is DashboardUiState.Failure -> {
                binds.swipeRefreshLayout.isRefreshing = false
                Snackbar.make(binds.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is DashboardUiState.Updated -> {
                // Update legacy elements for backward compatibility
                state.savingsGoals?.let { updateSavingsGoalUI(it) }
                updateBudgetUI(state.budget, state.statistics)
                
                // Update card adapter with new data
                dashboardCardPagerAdapter.updateBudgetData(state.budget, state.statistics)
                dashboardCardPagerAdapter.updateSavingsData(state.savingsGoals ?: emptyList())
                dashboardCardPagerAdapter.updateAccountsData(
                    model.accounts.value ?: emptyList(), 
                    dashboardAccountAdapter
                )
                
                // Stop refreshing
                binds.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupBottomNavigation() {
        binds.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoryManagementActivity::class.java))
                    finish(); true
                }
                R.id.nav_achievements -> {
                    startActivity(Intent(this, AchievementsActivity::class.java))
                    finish(); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish(); true
                }
                else -> false
            }
        }
        binds.bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    private fun setupDashboardAccountsRecyclerView() {
        dashboardAccountAdapter = AccountAdapter(
            onItemClick = { account ->
                try {
                    val intent = Intent(this, AccountDetailsView::class.java)
                    intent.putExtra("account_id", account.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onLongPress = { account ->
                android.util.Log.d("DashboardView", "Long press callback triggered for account: ${account.name}")
                showDeleteAccountDialog(account)
            }
        )

        // This will be used in the accounts card
        // The actual RecyclerView setup will be done in the card adapter
    }

    private fun updateSavingsGoalUI(goals: List<SavingsGoal>) {
        if (goals.isNotEmpty()) {
            val goal = goals[0]
            val progress = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
            val percent = (progress * 100).toInt().coerceIn(0, 100)

            // Update legacy elements for backward compatibility
            binds.savingsGoalTitle.text = "Savings Goal"
            binds.savingsGoalText.text = "${goal.name}: ${CurrencyFormatter.format(goal.savedAmount)} / ${CurrencyFormatter.format(goal.targetAmount)}"
            binds.currentSavingsText.text = CurrencyFormatter.format(goal.savedAmount)
            binds.maxSavingsText.text = CurrencyFormatter.format(goal.targetAmount)
            binds.savingsPercentageText.text = "$percent%"
            binds.savingsProgressBar.progress = percent

            binds.savingsGoalDate.text = goal.targetDate?.let {
                val dateFormat = android.text.format.DateFormat.getMediumDateFormat(this)
                "Target date: ${dateFormat.format(it)}"
            } ?: ""

            binds.contributeButton.isEnabled = goal.savedAmount < goal.targetAmount
        } else {
            binds.savingsGoalTitle.text = "Savings Goal"
            binds.savingsGoalText.text = getString(R.string.no_savings_goals)
            binds.currentSavingsText.text = CurrencyFormatter.format(0)
            binds.maxSavingsText.text = CurrencyFormatter.format(0)
            binds.savingsPercentageText.text = "0%"
            binds.savingsProgressBar.progress = 0
            binds.savingsGoalDate.text = ""
        }
    }

    private fun updateBudgetUI(budget: Budget?, stats: MonthlyStats?) {
        val spent = stats?.totalExpenses ?: 0.0
        val max = budget?.max ?: 1.0
        val min = budget?.min ?: 0.0

        // Update legacy elements for backward compatibility
        binds.budgetAmountText.text = CurrencyFormatter.format(budget?.max ?: 0.0)
        binds.budgetMonthText.text = budget?.let {
            try {
                val monthName = java.time.Month.of(it.month)
                    .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                "$monthName ${it.year}"
            } catch (_: Exception) {
                ""
            }
        } ?: ""

        binds.budgetSpentText.text = CurrencyFormatter.format(spent)
        val percent = (spent / max * 100).toInt().coerceIn(0, 100)
        binds.budgetProgressBar.progress = percent
        binds.budgetProgressText.text = "$percent%"

        val spentColor = when {
            spent < min -> R.color.teal_200
            spent <= max -> R.color.white
            else -> R.color.red
        }
        binds.budgetSpentText.setTextColor(resources.getColor(spentColor, theme))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binds.profileImage.id -> startActivity(Intent(this, ProfileActivity::class.java))
            binds.manageGoalsButton.id -> startActivity(Intent(this, ManageGoalsActivity::class.java))
            binds.addAccountButton.id -> showAddAccountDialog()
        }
    }

    private fun setupClickListeners() {
        binds.profileImage.setOnClickListener(this)
        binds.manageGoalsButton.setOnClickListener(this)
        binds.addAccountButton.setOnClickListener(this)
    }

    private fun setupBindings() {
        binds = ActivityDashboardBinding.inflate(layoutInflater)
    }

    private fun setupLayoutUi() {
        setContentView(binds.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binds.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupStatusBar()
    }

    private fun setupStatusBar() {
        window.statusBarColor = getColor(R.color.primary)
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }

    private fun showAchievementSnackbar(achievement: Achievement) {
        val message = "🎉 Achievement Unlocked: ${achievement.title} (+${achievement.boosterBucksReward} BB)"
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAction("View") {
                showAchievementDetailsDialog(achievement)
            }
            .show()
    }

    private fun showAchievementDetailsDialog(achievement: Achievement) {
        MaterialAlertDialogBuilder(this)
            .setTitle(achievement.title)
            .setMessage("${achievement.description}\n\nCompleted on: ${achievement.completedAt?.toDate() ?: "N/A"}")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadDashboardProfilePicture() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val profilePictureBase64 = document.getString("profilePicture")
                if (!profilePictureBase64.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(profilePictureBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binds.profileImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        binds.profileImage.setImageResource(R.drawable.ic_profile)
                    }
                } else {
                    binds.profileImage.setImageResource(R.drawable.ic_profile)
                }
            }
            .addOnFailureListener {
                binds.profileImage.setImageResource(R.drawable.ic_profile)
            }
    }

    private fun setupSwipeRefresh() {
        binds.swipeRefreshLayout.setOnRefreshListener {
            model.refreshData()
        }
    }

    private fun showAddAccountDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null)
        val accountNameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.accountNameEditText)
        val accountTypeInput = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.accountTypeAutoCompleteTextView)
        val saveButton = dialogView.findViewById<android.widget.Button>(R.id.saveAccountButton)
        
        // Setup account type dropdown
        val accountTypes = arrayOf("Savings", "Checking", "Credit Card", "Investment")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accountTypes)
        accountTypeInput.setAdapter(adapter)
        accountTypeInput.setText(accountTypes[0], false) // Set default value
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Add New Account")
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Handle save button click
        saveButton.setOnClickListener {
            val accountName = accountNameInput.text.toString().trim()
            val accountType = accountTypeInput.text.toString().trim()
            
            if (accountName.isEmpty()) {
                Toast.makeText(this, "Please enter an account name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Create new account with 0 balance initially
            val newAccount = Account(
                id = java.util.UUID.randomUUID().toString(),
                name = accountName,
                balance = 0.0,
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            )
            
            // Add account to database
            model.addAccount(newAccount)
            Toast.makeText(this, "Account added successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showDeleteAccountDialog(account: Account) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete '${account.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                model.deleteAccount(account.id)
                Toast.makeText(this, "Account deleted successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showContributeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contribute_savings, null)
        val amountInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.amountInput)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Contribute to Savings")
            .setView(dialogView)
            .setPositiveButton("Contribute") { dialog, _ ->
                val amountText = amountInput.text.toString().trim()
                if (amountText.isEmpty()) {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val amount = try {
                    amountText.toDoubleOrNull() ?: 0.0
                } catch (e: NumberFormatException) {
                    0.0
                }
                
                if (amount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Get the first savings goal and contribute to it
                val goals = model.savingsGoals.value
                if (goals?.isNotEmpty() == true) {
                    val goal = goals[0]
                    model.contributeToSavingsGoal(goal.id, amount)
                    Toast.makeText(this, "Contribution added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No savings goal found", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.show()
    }
}
