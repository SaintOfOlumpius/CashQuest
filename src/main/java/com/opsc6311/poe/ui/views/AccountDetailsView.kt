package com.opsc6311.poe.ui.views

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.opsc6311.poe.R
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.data.models.TransactionType
import com.opsc6311.poe.databinding.ActivityAccountDetailsBinding
import com.opsc6311.poe.ui.viewmodels.AccountDetailsViewModel
import com.opsc6311.poe.core.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class AccountDetailsView : AppCompatActivity(), View.OnClickListener {

    private lateinit var binds: ActivityAccountDetailsBinding
    private lateinit var model: AccountDetailsViewModel


    // --- Lifecycle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("AccountDetailsView", "onCreate started")
        
        try {
            val accountId = intent.getStringExtra("account_id")
            android.util.Log.d("AccountDetailsView", "Received account_id: $accountId")
            android.util.Log.d("AccountDetailsView", "Intent extras: ${intent.extras?.keySet()?.joinToString()}")

            if (accountId.isNullOrEmpty()) {
                android.util.Log.e("AccountDetailsView", "Account ID is null or empty, finishing activity")
                Toast.makeText(this, "Account ID not provided", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            setupBindings()
            setupLayoutUi()
            setupClickListeners()

            // Ensure user is authenticated
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            android.util.Log.d("AccountDetailsView", "User UID: $uid")
            
            if (uid.isNullOrEmpty()) {
                android.util.Log.e("AccountDetailsView", "User not authenticated, finishing activity")
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            model = ViewModelProvider(this)[AccountDetailsViewModel::class.java]

            observeViewModel()

            binds.timePeriodChipGroup.check(R.id.chip1Month) // Default

            // Load details
            android.util.Log.d("AccountDetailsView", "Loading account details for ID: $accountId")
            model.loadAccountDetails(uid, accountId)
            
            setupBottomNavigation()
            android.util.Log.d("AccountDetailsView", "onCreate completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("AccountDetailsView", "Exception in onCreate", e)
            Toast.makeText(this, "Error loading account details: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupBottomNavigation() {
        android.util.Log.d("AccountDetailsView", "Setting up bottom navigation")
        binds.bottomNavigation.setOnItemSelectedListener { item ->
            android.util.Log.d("AccountDetailsView", "Bottom navigation item selected: ${item.itemId}")
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    android.util.Log.d("AccountDetailsView", "Navigating to Dashboard")
                    startActivity(Intent(this, com.opsc6311.poe.ui.views.DashboardView::class.java))
                    finish()
                    true
                }
                R.id.nav_graph -> {
                    android.util.Log.d("AccountDetailsView", "Navigating to Graph")
                    startActivity(Intent(this, com.opsc6311.poe.ui.views.GraphView::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    android.util.Log.d("AccountDetailsView", "Navigating to Profile")
                    startActivity(Intent(this, com.opsc6311.poe.ui.views.ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> {
                    android.util.Log.d("AccountDetailsView", "Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }
        // Don't automatically select dashboard - let the user choose
        // binds.bottomNavigation.selectedItemId = R.id.nav_dashboard
        android.util.Log.d("AccountDetailsView", "Bottom navigation setup completed")
    }


    // --- ViewModel


    private fun observeViewModel() {
        model.account.observe(this) { account ->
            try {
                account?.let {
                    android.util.Log.d("AccountDetailsView", "Account loaded in UI: ${it.name} with ID: ${it.id}")
                    binds.accountNameTextView.text = it.name
                    binds.accountTypeTextView.text = it.type

                    model.calculatedBalance.observe(this) { balance ->
                        binds.accountBalanceAmount.text = CurrencyFormatter.format(balance)
                    }

                    val icon = when (it.type.lowercase(Locale.getDefault())) {
                        "credit" -> R.drawable.ic_credit_card
                        "savings" -> R.drawable.ic_savings
                        else -> R.drawable.ic_account_balance
                    }
                    binds.accountIcon.setImageResource(icon)
                    supportActionBar?.title = it.name
                } ?: run {
                    android.util.Log.e("AccountDetailsView", "Account is null in observer")
                    Toast.makeText(this, "Failed to load account details", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                android.util.Log.e("AccountDetailsView", "Exception in account observer", e)
                Toast.makeText(this, "Error displaying account: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        model.transactions.observe(this) { txs ->
            try {
                android.util.Log.d("AccountDetailsView", "Transactions loaded: ${txs.size}")
                binds.transactionsSummaryTextView.text = "${txs.size} Transactions"
                updateLineChart(txs)
            } catch (e: Exception) {
                android.util.Log.e("AccountDetailsView", "Exception in transactions observer", e)
            }
        }

        model.isLoading.observe(this) { /* show loader */ }
        model.error.observe(this) {
            it?.let { msg ->
                android.util.Log.e("AccountDetailsView", "Error from ViewModel: $msg")
                Snackbar.make(
                    binds.root, msg, Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }


    // --- Internals


    private fun setupLineChart() {
        binds.accountLineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                textColor = Color.BLACK
                textSize = 10f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
                textColor = Color.BLACK
                textSize = 10f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
            extraBottomOffset = 10f
            setExtraOffsets(10f, 10f, 10f, 10f)
        }
    }


    private fun updateLineChart(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            binds.accountLineChart.clear()
            return
        }

        val sorted = transactions.sortedBy { it.date }
        var balance = 0.0
        val entries = sorted.map { tx ->
            balance += when (tx.type) {
                TransactionType.INCOME -> tx.amount
                TransactionType.EXPENSE -> -tx.amount
                else -> 0.0
            }
            Entry(tx.date.toDate().time.toFloat(), balance.toFloat())
        }

        val ds = LineDataSet(entries, "Balance").apply {
            setDrawValues(false)
            setDrawFilled(true)
            fillAlpha = 85
            mode = LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = 2f
            val col = resources.getColor(R.color.primary, null)
            color = col
            setCircleColor(col)
            fillColor = col
            valueTextColor = Color.BLACK
        }

        binds.accountLineChart.data = LineData(ds)
        val labels = sorted.map { fmt ->
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(fmt.date.toDate())
        }
        binds.accountLineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            labelRotationAngle = -45f
            granularity = TimeUnit.DAYS.toMillis(1).toFloat()
        }
        binds.accountLineChart.invalidate()
    }


    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Delete Account")
            setMessage("Are you sure you want to delete this account? This action cannot be undone.")
            setPositiveButton("Delete") { _, _ ->
                val accId = model.account.value?.id // <-- Here maybe?
                if (accId != null) model.deleteAccount(accId)
                
                onSupportNavigateUp()
                Toast.makeText( // Go back to previous screen
                    this@AccountDetailsView, "Deletion successful", Toast.LENGTH_SHORT
                ).show()
            }
            setNegativeButton("Cancel", null)
        }.show()
    }


    // --- Event Handlers


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            // If there's nothing to go back to, launch Dashboard
            startActivity(Intent(this, com.opsc6311.poe.ui.views.DashboardView::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binds.viewTransactionsButton.id -> {
                val intent = Intent(this, TransactionsActivity::class.java).apply {
                    putExtra("account_id", model.account.value?.id)
                }
                startActivity(intent)
            }

            binds.deleteAccountButton.id -> {
                showDeleteConfirmationDialog()
            }
        }
    }


    private fun setupClickListeners() {
        binds.viewTransactionsButton.setOnClickListener(this)
        binds.deleteAccountButton.setOnClickListener(this)
    }


    // --- UI Configuration


    private fun setupToolbar() {
        setSupportActionBar(binds.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Account"
    }


    private fun setupTimePeriodChips() {
        binds.timePeriodChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip1Week -> model.filterTransactionsByTimePeriod("1 week")
                R.id.chip1Month -> model.filterTransactionsByTimePeriod("1 month")
                R.id.chip3Months -> model.filterTransactionsByTimePeriod("3 months")
            }
        }
    }


    // --- UI


    private fun setupBindings() {
        binds = ActivityAccountDetailsBinding.inflate(layoutInflater)
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
        setupToolbar()
        setupLineChart()
        setupTimePeriodChips()
    }

    private fun setupStatusBar() {
        window.statusBarColor = getColor(R.color.primary)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and 
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }

}
