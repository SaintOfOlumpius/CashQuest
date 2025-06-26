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
            }
        }
    }

    private fun setupViewPager() {
        binds.viewPager.adapter = DashboardPagerAdapter(this)
    }

    private fun setupCardsViewPager() {
        cardsPagerAdapter = CardsPagerAdapter(
            onSavingsGoalClick = { goal ->
                // Handle savings goal click
            },
            onManageGoalsClick = {
                startActivity(Intent(this, ManageGoalsActivity::class.java))
            },
            onContributeClick = {
                // Handle contribute click
                Toast.makeText(this, "Contribute to savings", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Transfer between accounts", Toast.LENGTH_SHORT).show()
            },
            onAccountLongPress = { account ->
                showDeleteAccountDialog(account)
            }
        )

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
        // This method is now handled by the CardsPagerAdapter
        // Keeping it for backward compatibility
    }

    private fun updateBudgetUI(budget: Budget?, stats: MonthlyStats?) {
        val spent = stats?.totalExpenses ?: 0.0
        val max = budget?.max ?: 1.0
        val min = budget?.min ?: 0.0

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
            binds.fab.id -> {
                // Handle FAB click for adding transactions
                Toast.makeText(this, "Add transaction", Toast.LENGTH_SHORT).show()
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

    private fun showAddAccountDialog() {
        Toast.makeText(this, "Add Account Dialog Placeholder", Toast.LENGTH_SHORT).show()
        // Implement your dialog logic here
    }

    private fun showDeleteAccountDialog(account: Account) {
        Toast.makeText(this, "Delete Account Dialog Placeholder", Toast.LENGTH_SHORT).show()
        // Implement your delete confirmation dialog here
    }
}
