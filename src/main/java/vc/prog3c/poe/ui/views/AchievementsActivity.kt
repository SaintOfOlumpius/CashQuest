package vc.prog3c.poe.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import vc.prog3c.poe.R
import vc.prog3c.poe.data.models.Achievement
import vc.prog3c.poe.data.models.AchievementCategory
import vc.prog3c.poe.data.models.QuestCoins
import vc.prog3c.poe.databinding.ActivityAchievementsBinding
import vc.prog3c.poe.ui.adapters.AchievementAdapter
import vc.prog3c.poe.ui.viewmodels.AchievementViewModel
import vc.prog3c.poe.core.utils.CurrencyFormatter
import vc.prog3c.poe.core.utils.LevelingHelper
import java.text.NumberFormat
import java.util.Locale

class AchievementsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binds: ActivityAchievementsBinding
    private lateinit var model: AchievementViewModel
    private lateinit var adapter: AchievementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBindings()
        setupLayoutUi()
        setupClickListeners()
        setupBottomNavigation()

        model = ViewModelProvider(this)[AchievementViewModel::class.java]
        observeViewModel()
    }

    private fun observeViewModel() {
        model.achievements.observe(this) { achievements ->
            adapter.updateAchievements(achievements)
        }

        model.questCoins.observe(this) { questCoins ->
            updateQuestCoinsDisplay(questCoins)
        }

        model.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binds.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateQuestCoinsDisplay(questCoins: QuestCoins) {
        binds.QuestCoinsValue.text = CurrencyFormatter.format(
            questCoins.availableBalance * QuestCoins.CONVERSION_RATE
        )

        val (progress, goal) = questCoins.progressToNextLevel
        binds.coinProgress.max = goal
        binds.coinProgress.progress = progress
        binds.levelLabel.text = "Level ${questCoins.level}"
    }

    private fun showAchievementDetails(achievement: Achievement) {
        val message = if (achievement.isCompleted) {
            "Completed on ${achievement.completedAt?.toString() ?: "Unknown date"}"
        } else {
            "Progress: ${achievement.progress}/${achievement.requiredProgress}"
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(achievement.title)
            setMessage("$message\n\n${achievement.description}")
            setPositiveButton("OK", null)
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
                    formatter.format(redeemAmount)
                }"
            )
            setPositiveButton("Redeem") { _, _ ->
                model.redeemQuestCoins()
            }
            setNegativeButton("Cancel", null)
        }.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
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
    }

    private fun setupToolbar() {
        setSupportActionBar(binds.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Achievements"
    }

    private fun setupRecyclerView() {
        adapter = AchievementAdapter(emptyList()) { achievement ->
            showAchievementDetails(achievement)
        }
        binds.achievementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AchievementsActivity)
            adapter = this@AchievementsActivity.adapter
        }
    }

    private fun setupTabLayout() {
        binds.achievementTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = when (tab?.position) {
                    0 -> null
                    1 -> AchievementCategory.USER_MILESTONES
                    2 -> AchievementCategory.CONSISTENCY_HABITS
                    3 -> AchievementCategory.SAVINGS_ACHIEVEMENTS
                    4 -> AchievementCategory.BUDGET_MANAGEMENT
                    5 -> AchievementCategory.FINANCIAL_INSIGHT
                    6 -> AchievementCategory.LEARNING_GROWTH
                    else -> null
                }
                adapter.filterByCategory(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
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
    }
}