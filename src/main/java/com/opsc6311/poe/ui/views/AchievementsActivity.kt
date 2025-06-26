package com.opsc6311.poe.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.opsc6311.poe.R
import com.opsc6311.poe.data.models.Achievement
import com.opsc6311.poe.data.models.AchievementCategory
import com.opsc6311.poe.data.models.QuestCoins
import com.opsc6311.poe.databinding.ActivityAchievementsBinding
import com.opsc6311.poe.ui.adapters.AchievementAdapter
import com.opsc6311.poe.ui.viewmodels.AchievementViewModel
import com.opsc6311.poe.core.utils.CurrencyFormatter
import com.opsc6311.poe.core.utils.SeedData
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.SearchView
import android.widget.Toast
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import com.opsc6311.poe.data.services.AchievementSeedService

class AchievementsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binds: ActivityAchievementsBinding
    private lateinit var model: AchievementViewModel
    private lateinit var adapter: AchievementAdapter
    private var currentFilter: AchievementCategory? = null
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBindings()
        setupLayoutUi()
        setupClickListeners()
        setupBottomNavigation()

        model = ViewModelProvider(this)[AchievementViewModel::class.java]

        observeViewModel()
        
        // Add entrance animation
        addEntranceAnimations()

        // Auto-evaluate achievements on page load
        autoEvaluateAchievements()
    }

    private fun addEntranceAnimations() {
        // Simple fade-in animation for the quest coins card
        binds.questCoinsCard.alpha = 0f
        binds.questCoinsCard.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(200L)
            .start()

        // Simple fade-in animation for stats cards
        val statsCards = listOf(
            binds.root.findViewById<View>(R.id.completedCount)?.parent as? View,
            binds.root.findViewById<View>(R.id.totalCount)?.parent as? View
        ).filterNotNull()

        statsCards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(400L + (index * 100L))
                .start()
        }

        // Simple fade-in animation for the tab layout
        binds.achievementTabs.alpha = 0f
        binds.achievementTabs.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(600L)
            .start()
    }

    private fun observeViewModel() {
        model.achievements.observe(this) { achievements ->
            filterAchievements()
            updateStatsCards(achievements)
        }

        model.userAchievements.observe(this) { userAchievements ->
            filterAchievements()
        }

        model.questCoins.observe(this) { questCoins ->
            updateQuestCoinsDisplay(questCoins)
        }

        model.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binds.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        model.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
            if (isLoading) {
                // You can add a loading indicator here if needed
            }
        }

        // Observe newly completed achievements for celebrations
        model.newAchievementUnlocked.observe(this) { achievement ->
            achievement?.let {
                showAchievementCelebration(it)
            }
        }

        // Observe level up events
        model.levelUp.observe(this) { level ->
            if (level > 0) {
                showLevelUpCelebration(level)
            }
        }
    }

    private fun updateQuestCoinsDisplay(questCoins: QuestCoins) {
        // Animate the currency value change
        val currentValue = binds.QuestCoinsValue.text.toString()
        val newValue = questCoins.availableBalance.toString()
        
        if (currentValue != newValue) {
            binds.QuestCoinsValue.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binds.QuestCoinsValue.text = newValue
                    binds.QuestCoinsValue.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }

        val (level, levelName, progressPercent) = calculateLevel(questCoins.availableBalance)

        // Animate level change
        val newLevelText = "Level $level $levelName"
        if (binds.levelChip.text != newLevelText) {
            binds.levelChip.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binds.levelChip.text = newLevelText
                    binds.levelChip.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }

        // Update progress text
        binds.progressText.text = "$progressPercent%"

        // Animate progress bar
        binds.coinProgress.animate()
            .setDuration(1000)
            .start()
        binds.coinProgress.progress = progressPercent
    }

    private fun updateStatsCards(achievements: List<Achievement>) {
        val completedCount = achievements.count { it.isCompleted }
        val totalCount = achievements.size

        // Animate completed count
        binds.completedCount.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binds.completedCount.text = completedCount.toString()
                binds.completedCount.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()

        // Animate total count
        binds.totalCount.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binds.totalCount.text = totalCount.toString()
                binds.totalCount.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    // Dynamic leveling system based on available balance
    private fun calculateLevel(balance: Int): Triple<Int, String, Int> {
        // Example leveling logic - you can customize the thresholds and names
        val levels = listOf(
            0 to "Novice",
            100 to "Apprentice",
            250 to "Adventurer",
            500 to "Hero",
            1000 to "Legend",
            2000 to "Mythic"
        )

        var currentLevel = 0
        var currentName = "Novice"
        var nextLevelThreshold = 100

        for (i in levels.indices) {
            if (balance >= levels[i].first) {
                currentLevel = i + 1
                currentName = levels[i].second
                nextLevelThreshold = if (i + 1 < levels.size) levels[i + 1].first else levels[i].first
            } else break
        }

        // Calculate progress percentage to next level
        val prevThreshold = if (currentLevel - 1 >= 0) levels[currentLevel - 1].first else 0
        val range = nextLevelThreshold - prevThreshold
        val progress = if (range > 0) ((balance - prevThreshold) * 100 / range) else 100

        return Triple(currentLevel, currentName, progress.coerceIn(0, 100))
    }

    private fun showAchievementDetails(achievement: Achievement) {
        val message = if (achievement.isCompleted) {
            "Completed on ${achievement.completedAt?.toString() ?: "Unknown date"}"
        } else {
            "Progress: ${achievement.progress}/${achievement.requiredProgress}"
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(achievement.title)
            setMessage("$message\n\n${achievement.description}\n\nReward: ${achievement.questCoinsReward} Quest Coins")
            setPositiveButton("OK", null)
            if (!achievement.isCompleted) {
                setNegativeButton("Track Progress") { _, _ ->
                    showProgressTrackingDialog(achievement)
                }
            }
        }.show()
    }

    private fun showProgressTrackingDialog(achievement: Achievement) {
        val currentProgress = achievement.progress
        val maxProgress = achievement.requiredProgress

        MaterialAlertDialogBuilder(this).apply {
            setTitle("Track Progress")
            setMessage("Current Progress: $currentProgress/$maxProgress\n\nWould you like to update your progress for '${achievement.title}'?")
            setPositiveButton("Update Progress") { _, _ ->
                showProgressUpdateDialog(achievement)
            }
            setNegativeButton("Cancel", null)
        }.show()
    }

    private fun showProgressUpdateDialog(achievement: Achievement) {
        val currentProgress = achievement.progress
        val maxProgress = achievement.requiredProgress

        val input = android.widget.EditText(this).apply {
            hint = "Enter new progress (0-$maxProgress)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentProgress.toString())
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle("Update Progress")
            setMessage("Enter your new progress for '${achievement.title}':")
            setView(input)
            setPositiveButton("Update") { _, _ ->
                val newProgress = input.text.toString().toIntOrNull() ?: currentProgress
                val clampedProgress = newProgress.coerceIn(0, maxProgress)
                
                model.updateAchievementProgress(achievement.id, clampedProgress)
                Snackbar.make(binds.root, "Progress updated!", Snackbar.LENGTH_SHORT).show()
            }
            setNegativeButton("Cancel", null)
        }.show()
    }

    private fun showRedeemDialog() {
        val questCoins = model.questCoins.value ?: return
        if (questCoins.availableBalance < QuestCoins.MIN_REDEMPTION) {
            Snackbar.make(
                binds.root,
                "You need at least ${QuestCoins.MIN_REDEMPTION} Quest Coins to redeem",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val redeemAmount = questCoins.availableBalance * QuestCoins.CONVERSION_RATE

        MaterialAlertDialogBuilder(this).apply {
            setTitle("Redeem Quest Coins")
            setMessage(
                "You can redeem ${questCoins.availableBalance} Quest Coins for ${
                    formatter.format(
                        redeemAmount
                    )
                }"
            )
            setPositiveButton("Redeem") { _, _ ->
                model.redeemQuestCoins()
            }
            setNegativeButton("Cancel", null)
        }.show()
    }

    private fun showAchievementCelebration(achievement: Achievement) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "🎉 Achievement Unlocked: ${achievement.title} (+${achievement.questCoinsReward} coins)",
            Snackbar.LENGTH_LONG
        )
        
        snackbar.setAction("View") {
            // Scroll to the achievement in the list
            val position = adapter.currentList.indexOfFirst { it.id == achievement.id }
            if (position != -1) {
                binds.achievementsRecyclerView.smoothScrollToPosition(position)
            }
        }
        
        snackbar.show()
        
        // Clear the notification
        model.clearNewAchievementNotification()
    }

    private fun showLevelUpCelebration(level: Int) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "🎊 Level Up! You're now level $level!",
            Snackbar.LENGTH_LONG
        )
        
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.colorPrimary))
        snackbar.show()
        
        // Clear the notification
        model.clearLevelUpNotification()
    }

    private fun autoEvaluateAchievements() {
        // Evaluate achievements on page load
        model.evaluateAchievements()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_evaluate_achievements -> {
                model.evaluateAchievements()
                Snackbar.make(binds.root, "Evaluating achievements...", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_refresh -> {
                refreshAchievements()
                true
            }
            R.id.action_seed_achievements -> {
                seedAchievements()
                true
            }
            R.id.action_filter_all -> {
                currentFilter = null
                filterAchievements()
                true
            }
            R.id.action_filter_milestones -> {
                currentFilter = AchievementCategory.USER_MILESTONES
                filterAchievements()
                true
            }
            R.id.action_filter_consistency -> {
                currentFilter = AchievementCategory.CONSISTENCY_HABITS
                filterAchievements()
                true
            }
            R.id.action_filter_savings -> {
                currentFilter = AchievementCategory.SAVINGS_ACHIEVEMENTS
                filterAchievements()
                true
            }
            R.id.action_filter_budget -> {
                currentFilter = AchievementCategory.BUDGET_MANAGEMENT
                filterAchievements()
                true
            }
            R.id.action_filter_insight -> {
                currentFilter = AchievementCategory.FINANCIAL_INSIGHT
                filterAchievements()
                true
            }
            R.id.action_filter_learning -> {
                currentFilter = AchievementCategory.LEARNING_GROWTH
                filterAchievements()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binds.redeemButton.id -> showRedeemDialog()
        }
    }

    private fun setupClickListeners() {
        binds.redeemButton.setOnClickListener(this)
        
        // Add refresh functionality
        binds.swipeRefreshLayout.setOnRefreshListener {
            refreshAchievements()
        }
        
        // Add manual evaluation button if you have one
        // binds.evaluateButton.setOnClickListener {
        //     model.evaluateAchievements()
        // }
    }

    private fun setupToolbar() {
        setSupportActionBar(binds.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🏆 Achievements"
        
        // Add seed button for development
        binds.toolbar.inflateMenu(R.menu.achievements_menu)
        binds.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_seed_achievements -> {
                    seedAchievements()
                    true
                }
                else -> false
            }
        }
    }

    private fun seedAchievements() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val seedService = AchievementSeedService()
                seedService.seedAchievements()
                Snackbar.make(binds.root, "Achievements seeded successfully!", Snackbar.LENGTH_SHORT).show()
                refreshAchievements()
            } catch (e: Exception) {
                Snackbar.make(binds.root, "Failed to seed achievements: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AchievementAdapter(
            onAchievementClick = { achievement ->
                showAchievementDetails(achievement)
            },
            onRedeemClick = { achievement ->
                redeemAchievement(achievement)
            }
        )
        binds.achievementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AchievementsActivity)
            adapter = this@AchievementsActivity.adapter
            // Add smooth scrolling
            setHasFixedSize(true)
            // Add item decoration for better spacing
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    outRect.top = 8
                    outRect.bottom = 8
                }
            })
        }
    }

    private fun setupTabLayout() {
        binds.achievementTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = when (tab?.position) {
                    0 -> null // All
                    1 -> AchievementCategory.USER_MILESTONES
                    2 -> AchievementCategory.CONSISTENCY_HABITS
                    3 -> AchievementCategory.SAVINGS_ACHIEVEMENTS
                    4 -> AchievementCategory.BUDGET_MANAGEMENT
                    5 -> AchievementCategory.FINANCIAL_INSIGHT
                    6 -> AchievementCategory.LEARNING_GROWTH
                    else -> null
                }
                currentFilter = category
                filterAchievements()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // No animation needed
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // No animation needed
            }
        })
    }

    private fun setupQuestCoinsCard() {
        binds.redeemButton.setOnClickListener {
            showRedeemDialog()
        }
    }

    private fun setupBottomNavigation() {
        binds.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardView::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoryManagementActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_achievements -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        binds.bottomNavigation.selectedItemId = R.id.nav_achievements
    }

    private fun setupBindings() {
        binds = ActivityAchievementsBinding.inflate(layoutInflater)
    }

    private fun setupLayoutUi() {
        setContentView(binds.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binds.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        setupQuestCoinsCard()
        setupSwipeRefresh()
        setupSearch()
    }

    private fun setupSwipeRefresh() {
        binds.swipeRefreshLayout.setOnRefreshListener {
            // Refresh achievements and quest coins
            model.refresh()
            
            // Stop refreshing after a delay
            binds.swipeRefreshLayout.postDelayed({
                binds.swipeRefreshLayout.isRefreshing = false
            }, 2000)
        }
    }

    private fun setupSearch() {
        binds.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchQuery = s?.toString() ?: ""
                filterAchievements()
            }
        })
    }

    private fun filterAchievements() {
        val allAchievements = model.achievements.value ?: emptyList()
        val userAchievements = model.userAchievements.value ?: emptyList()
        
        // Merge achievements with user progress
        val mergedAchievements = allAchievements.map { achievement ->
            val userAchievement = userAchievements.find { it.id == achievement.id }
            userAchievement ?: achievement.copy(
                progress = 0,
                isCompleted = false,
                completedAt = null
            )
        }
        
        // Apply filters
        val filteredAchievements = mergedAchievements.filter { achievement ->
            val matchesCategory = currentFilter == null || achievement.category == currentFilter
            val matchesSearch = searchQuery.isEmpty() || 
                achievement.title.contains(searchQuery, ignoreCase = true) ||
                achievement.description.contains(searchQuery, ignoreCase = true)
            
            matchesCategory && matchesSearch
        }
        
        adapter.submitList(filteredAchievements)
    }

    private fun evaluateAchievements() {
        model.evaluateAchievements()
        Toast.makeText(this, "Evaluating achievements...", Toast.LENGTH_SHORT).show()
    }

    private fun redeemAchievement(achievement: Achievement) {
        if (!achievement.isCompleted) {
            Toast.makeText(this, "Achievement not completed yet!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Redeem Achievement")
            .setMessage("You've earned ${achievement.questCoinsReward} quest coins for completing '${achievement.title}'!")
            .setPositiveButton("Claim") { _, _ ->
                // Achievement is already redeemed when completed
                Toast.makeText(this, "Achievement already claimed!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }

    private fun refreshAchievements() {
        model.refreshData()
        binds.swipeRefreshLayout.isRefreshing = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.achievements_menu, menu)
        return true
    }
}
