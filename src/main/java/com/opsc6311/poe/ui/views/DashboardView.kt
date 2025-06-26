package com.opsc6311.poe.ui.views

import android.content.*
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.opsc6311.poe.R
import com.opsc6311.poe.data.models.*
import com.opsc6311.poe.databinding.ActivityDashboardBinding
import com.opsc6311.poe.ui.adapters.AccountAdapter
import com.opsc6311.poe.ui.adapters.CategoryAdapter
import com.opsc6311.poe.ui.adapters.TransactionAdapter
import com.opsc6311.poe.ui.adapters.DashboardPagerAdapter
import com.opsc6311.poe.ui.adapters.CardsPagerAdapter
import com.opsc6311.poe.ui.viewmodels.AchievementViewModel
import com.opsc6311.poe.ui.viewmodels.DashboardUiState
import com.opsc6311.poe.ui.viewmodels.DashboardViewModel
import com.opsc6311.poe.core.utils.CurrencyFormatter
import java.util.*

class DashboardView : AppCompatActivity(), View.OnClickListener {

    private lateinit var binds: ActivityDashboardBinding
    lateinit var model: DashboardViewModel
    private lateinit var achievementViewModel: AchievementViewModel
    private lateinit var cardsPagerAdapter: CardsPagerAdapter

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
        setupViewPager()
        setupCardsViewPager()

        model = ViewModelProvider(this)[DashboardViewModel::class.java]
        achievementViewModel = ViewModelProvider(this)[AchievementViewModel::class.java]

        setupBottomNavigation()

        observeViewModel()
        model.refreshData()

        achievementViewModel.newlyCompleted.observe(this) { event ->
            event.getContentIfNotHandled()?.let { achievement ->
                showAchievementSnackbar(achievement)
            }
        }

        val filter = IntentFilter("PROFILE_PICTURE_UPDATED")
        registerReceiver(profilePictureReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Load profile picture on startup
        loadDashboardProfilePicture()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(profilePictureReceiver)
    }

    override fun onResume() {
        super.onResume()
        model.refreshData()
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
            is DashboardUiState.Default -> binds.swipeRefreshLayout.isRefreshing = false
            is DashboardUiState.Loading -> binds.swipeRefreshLayout.isRefreshing = true
            is DashboardUiState.Failure -> {
                binds.swipeRefreshLayout.isRefreshing = false
                Snackbar.make(binds.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is DashboardUiState.Updated -> {
                state.savingsGoals?.let { 
                    updateSavingsGoalUI(it)
                    cardsPagerAdapter.updateSavingsGoals(it)
                }
                state.accounts?.let { accounts ->
                    cardsPagerAdapter.updateAccounts(accounts)
                }
                updateBudgetUI(state.budget, state.statistics)
                binds.swipeRefreshLayout.isRefreshing = false
                
                // Evaluate achievements when dashboard data is updated
                achievementViewModel.evaluateAchievements()
            }
        }
    }

    private fun setupViewPager() {
        binds.viewPager.adapter = DashboardPagerAdapter(this)
    }

    private fun setupCardsViewPager() {
        cardsPagerAdapter = CardsPagerAdapter(
            onSavingsGoalClick = { goal ->
                val intent = Intent(this, ManageGoalsActivity::class.java)
                startActivity(intent)
            },
            onManageGoalsClick = {
                startActivity(Intent(this, ManageGoalsActivity::class.java))
            },
            onContributeClick = {
                // Deprecated, use per-goal click below
            },
            onAccountClick = { account ->
                try {
                    val intent = Intent(this, AccountDetailsView::class.java)
                    intent.putExtra("account_id", account.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onAddAccountClick = {
                showAddAccountDialog()
            },
            onViewAllAccountsClick = {
                startActivity(Intent(this, AccountsView::class.java))
            },
            onTransferClick = {
                showTransferDialog()
            },
            onAccountLongPress = { account ->
                showDeleteAccountDialog(account)
            }
        )

        // Set per-goal contribute click
        cardsPagerAdapter.setOnContributeToGoalClickListener { goal ->
            showContributeToGoalDialog(goal)
        }

        binds.cardsViewPager.adapter = cardsPagerAdapter
        binds.cardsViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // Setup tab indicators
        setupTabIndicators()
    }

    private fun setupTabIndicators() {
        binds.savingsTab.setOnClickListener {
            binds.cardsViewPager.currentItem = 0
            updateTabSelection(0)
        }

        binds.accountsTab.setOnClickListener {
            binds.cardsViewPager.currentItem = 1
            updateTabSelection(1)
        }

        binds.cardsViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabSelection(position)
            }
        })

        // Set initial selection
        updateTabSelection(0)
    }

    private fun updateTabSelection(position: Int) {
        when (position) {
            0 -> {
                binds.savingsTab.apply {
                    setTextColor(resources.getColor(R.color.primary, theme))
                    setBackgroundResource(R.drawable.bg_category_type_chip)
                }
                binds.accountsTab.apply {
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    setBackgroundResource(android.R.color.transparent)
                }
            }
            1 -> {
                binds.accountsTab.apply {
                    setTextColor(resources.getColor(R.color.primary, theme))
                    setBackgroundResource(R.drawable.bg_category_type_chip)
                }
                binds.savingsTab.apply {
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    setBackgroundResource(android.R.color.transparent)
                }
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

    private fun updateSavingsGoalUI(goals: List<SavingsGoal>) {
        if (goals.isNotEmpty()) {
            val goal = goals[0]
            val progress = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
            val percent = (progress * 100).toInt().coerceIn(0, 100)

            val dateFormat = java.text.SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val dateText = goal.targetDate?.let { "Target: ${dateFormat.format(it)}" } ?: "No target date"

            // This will be handled by the CardsPagerAdapter
            cardsPagerAdapter.updateSavingsGoals(goals)
        }
    }

    private fun updateBudgetUI(budget: Budget?, stats: MonthlyStats?) {
        if (budget != null && stats != null) {
            val spent = stats.totalExpenses
            val budgetAmount = budget.max
            val progress = if (budgetAmount > 0) (spent / budgetAmount).coerceIn(0.0, 1.0) else 0.0
            val percentage = (progress * 100).toInt()

            binds.budgetAmountText.text = CurrencyFormatter.format(budgetAmount)
            binds.budgetSpentText.text = CurrencyFormatter.format(spent)
            binds.budgetProgressText.text = "$percentage%"
            binds.budgetProgressBar.progress = percentage

            val progressColor = when {
                spent <= budget.min -> resources.getColor(R.color.income_green, theme)
                spent <= budget.max -> resources.getColor(R.color.primary, theme)
                else -> resources.getColor(R.color.expense_red, theme)
            }
            binds.budgetProgressBar.setIndicatorColor(progressColor)

            val periodText = if (budget.month > 0 && budget.year > 0) {
                val monthNum = budget.month % 100
                val monthName = try {
                    java.time.Month.of(monthNum).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                } catch (e: Exception) {
                    ""
                }
                "$monthName ${budget.year}"
            } else {
                "Current Period"
            }
            binds.budgetPeriodText.text = periodText
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binds.profileImage.id -> startActivity(Intent(this, ProfileActivity::class.java))
            binds.fab.id -> {
                // Navigate to add transaction page
                val intent = Intent(this, TransactionUpsertActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setupClickListeners() {
        binds.profileImage.setOnClickListener(this)
        binds.fab.setOnClickListener(this)
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
        val message = "🎉 Achievement Unlocked: ${achievement.title} (+${achievement.questCoinsReward} Quest Coins)"
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

    // MARK: - Dialog Implementations

    private fun showContributeToGoalDialog(goal: SavingsGoal) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_contribute_savings, null)
        val amountInput = dialogView.findViewById<TextInputLayout>(R.id.amountInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Contribute to ${goal.name}")
            .setView(dialogView)
            .setPositiveButton("Contribute") { _, _ ->
                val amountText = amountInput.editText?.text.toString()
                if (amountText.isNotEmpty()) {
                    try {
                        val amount = amountText.toDouble()
                        if (amount > 0) {
                            model.contributeToSavingsGoal(goal.id, amount)
                            Toast.makeText(this, "Successfully contributed R$amount to ${goal.name}", Toast.LENGTH_SHORT).show()
                            model.refreshData()
                        } else {
                            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddAccountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_account, null)
        val nameInput = dialogView.findViewById<TextInputLayout>(R.id.nameInput)
        val typeInput = dialogView.findViewById<TextInputLayout>(R.id.typeInput)
        val balanceInput = dialogView.findViewById<TextInputLayout>(R.id.balanceInput)

        // Setup account type dropdown
        val accountTypes = arrayOf("Checking", "Savings", "Credit Card", "Investment", "Cash")
        val typeAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accountTypes)
        val typeDropdown = typeInput.editText as? android.widget.AutoCompleteTextView
        typeDropdown?.setAdapter(typeAdapter)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Account")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = nameInput.editText?.text.toString()
                val type = typeDropdown?.text.toString()
                val balanceStr = balanceInput.editText?.text.toString()

                if (name.isBlank()) {
                    nameInput.error = "Account name is required"
                    return@setPositiveButton
                }

                if (type.isBlank()) {
                    typeInput.error = "Account type is required"
                    return@setPositiveButton
                }

                val balance = balanceStr.toDoubleOrNull() ?: 0.0

                val account = Account(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    balance = balance
                )

                model.addAccount(account)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog(account: Account) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete '${account.name}'? This action cannot be undone and will also delete all associated transactions.")
            .setPositiveButton("Delete") { dialog, _ ->
                model.deleteAccount(account.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTransferDialog() {
        val accounts = model.accounts.value
        if (accounts.isNullOrEmpty() || accounts.size < 2) {
            Toast.makeText(this, "You need at least 2 accounts to make transfers", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_transfer, null)
        val fromAccountInput = dialogView.findViewById<TextInputLayout>(R.id.fromAccountInput)
        val toAccountInput = dialogView.findViewById<TextInputLayout>(R.id.toAccountInput)
        val amountInput = dialogView.findViewById<TextInputLayout>(R.id.amountInput)

        // Setup account dropdowns
        val accountNames = accounts.map { it.name }
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accountNames)
        val fromDropdown = fromAccountInput.editText as? android.widget.AutoCompleteTextView
        val toDropdown = toAccountInput.editText as? android.widget.AutoCompleteTextView
        fromDropdown?.setAdapter(adapter)
        toDropdown?.setAdapter(adapter)

        MaterialAlertDialogBuilder(this)
            .setTitle("Transfer Between Accounts")
            .setView(dialogView)
            .setPositiveButton("Transfer") { dialog, _ ->
                val fromAccountName = fromDropdown?.text.toString()
                val toAccountName = toDropdown?.text.toString()
                val amountStr = amountInput.editText?.text.toString()

                if (fromAccountName.isBlank() || toAccountName.isBlank()) {
                    Toast.makeText(this, "Please select both accounts", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (fromAccountName == toAccountName) {
                    Toast.makeText(this, "Cannot transfer to the same account", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val fromAccount = accounts.find { it.name == fromAccountName }
                val toAccount = accounts.find { it.name == toAccountName }

                if (fromAccount == null || toAccount == null) {
                    Toast.makeText(this, "Invalid account selection", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (fromAccount.balance < amount) {
                    Toast.makeText(this, "Insufficient balance in source account", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Perform transfer using account IDs
                model.transferBetweenAccounts(fromAccount.id, toAccount.id, amount)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTransactionDialog() {
        val accounts = model.accounts.value
        if (accounts.isNullOrEmpty()) {
            Toast.makeText(this, "Please add an account first", Toast.LENGTH_LONG).show()
            return
        }

        if (accounts.size == 1) {
            // If only one account, go directly to transaction activity
            val intent = Intent(this, TransactionUpsertActivity::class.java)
            intent.putExtra("account_id", accounts.first().id)
            startActivity(intent)
        } else {
            // Show account selection dialog
            val accountNames = accounts.map { it.name }.toTypedArray()
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Select Account")
                .setItems(accountNames) { dialog, which ->
                    val selectedAccount = accounts[which]
                    val intent = Intent(this, TransactionUpsertActivity::class.java)
                    intent.putExtra("account_id", selectedAccount.id)
                    startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
