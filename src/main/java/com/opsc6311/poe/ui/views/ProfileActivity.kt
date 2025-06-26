package com.opsc6311.poe.ui.views

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.opsc6311.poe.R
import com.opsc6311.poe.databinding.ActivityProfileBinding
import com.opsc6311.poe.ui.viewmodels.AuthViewModel
import com.opsc6311.poe.data.repository.CategoryRepository
import com.opsc6311.poe.data.repository.TransactionRepository
import com.opsc6311.poe.data.models.Transaction
import com.opsc6311.poe.data.models.Category
import com.opsc6311.poe.data.models.TransactionType
import com.opsc6311.poe.data.models.User
import android.util.Base64
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binds: ActivityProfileBinding
    private lateinit var model: AuthViewModel
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    // --- Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBindings()
        setupLayoutUi()
        setupClickListeners()

        model = ViewModelProvider(this)[AuthViewModel::class.java]
        transactionRepository = TransactionRepository()
        categoryRepository = CategoryRepository()

        setupBottomNavigation()
        setupChartData()
        observeViewModel()

        model.loadUserProfile() // Load user info from Firestore
    }

    override fun onResume() {
        super.onResume() // Ensure profile is selected when returning to this activity
        binds.bottomNavigation.selectedItemId = R.id.nav_profile
    }

    // --- ViewModel

    private fun observeViewModel() {
        model.currentUser.observe(this) { user ->
            user?.let {
                binds.nameText.text = it.getFullName()
                binds.emailText.text = it.email

                // Load profile picture from Firestore
                loadProfilePicture()
            }
        }

        model.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfilePicture() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val profilePictureBase64 = document.getString("profilePicture")
                    if (!profilePictureBase64.isNullOrEmpty()) {
                        try {
                            val imageBytes = Base64.decode(profilePictureBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            binds.profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // If loading fails, set default image
                            binds.profileImage.setImageResource(R.drawable.ic_profile)
                        }
                    } else {
                        // Set default image if no profile picture
                        binds.profileImage.setImageResource(R.drawable.ic_profile)
                    }
                } else {
                    // Set default image if user document doesn't exist
                    binds.profileImage.setImageResource(R.drawable.ic_profile)
                }
            }
            .addOnFailureListener {
                // Set default image on failure
                binds.profileImage.setImageResource(R.drawable.ic_profile)
            }
    }

    // --- Internals

    private fun setupBottomNavigation() {
        binds.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, com.opsc6311.poe.ui.views.DashboardView::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, com.opsc6311.poe.ui.views.CategoryManagementActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_achievements -> {
                    startActivity(Intent(this, com.opsc6311.poe.ui.views.AchievementsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
        binds.bottomNavigation.selectedItemId = R.id.nav_profile
        binds.bottomNavigation.setBackgroundColor(getColor(R.color.surface))
    }

    // --- Event Handlers

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This will permanently delete your account and all associated data. Continue?")
            .setPositiveButton("Delete") { _, _ -> deleteAccountAndData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Can move into user repo, or viewmodel
    private fun deleteAccountAndData() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (user == null || uid == null) {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(uid)

        // Step 1: Delete nested transactions under each account
        userDocRef.collection("accounts").get()
            .addOnSuccessListener { accounts ->
                val deletionTasks = mutableListOf<com.google.android.gms.tasks.Task<Void>>()

                for (account in accounts) {
                    val accountId = account.id
                    val txRef = userDocRef.collection("accounts")
                        .document(accountId)
                        .collection("transactions")

                    // Delete each transaction in this account
                    txRef.get().addOnSuccessListener { transactions ->
                        for (transaction in transactions) {
                            deletionTasks.add(txRef.document(transaction.id).delete())
                        }
                    }

                    // Delete the account itself
                    deletionTasks.add(userDocRef.collection("accounts").document(accountId).delete())
                }

                // Step 2: Delete savings goals
                userDocRef.collection("savingsGoals").get()
                    .addOnSuccessListener { goals ->
                        for (goal in goals) {
                            deletionTasks.add(userDocRef.collection("savingsGoals").document(goal.id).delete())
                        }

                        // Step 3: Delete budgets
                        userDocRef.collection("budgets").get()
                            .addOnSuccessListener { budgets ->
                                for (budget in budgets) {
                                    deletionTasks.add(userDocRef.collection("budgets").document(budget.id).delete())
                                }

                                // Step 4: Delete user document
                                deletionTasks.add(userDocRef.delete())

                                // Wait for all deletes, then delete auth account
                                com.google.android.gms.tasks.Tasks.whenAllComplete(deletionTasks)
                                    .addOnSuccessListener {
                                        user.delete().addOnSuccessListener {
                                            Toast.makeText(this, "Account fully deleted.", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, com.opsc6311.poe.ui.views.SignInActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            })
                                            finish()
                                        }.addOnFailureListener {
                                            Toast.makeText(this, "Failed to delete auth account.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete account data.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.manageGoalsButton -> {
                startActivity(Intent(this, com.opsc6311.poe.ui.views.ManageGoalsActivity::class.java))
            }
            R.id.deleteAccountButton -> {
                showDeleteAccountConfirmation()
            }
            R.id.logoutButton -> {
                model.signOut()
                startActivity(Intent(this, com.opsc6311.poe.ui.views.SignInActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            R.id.editProfileImageButton -> {
                openImagePicker()
            }
        }
    }

    private fun setupClickListeners() {
        binds.manageGoalsButton.setOnClickListener(this)
        binds.deleteAccountButton.setOnClickListener(this)
        binds.logoutButton.setOnClickListener(this)
        binds.editProfileImageButton.setOnClickListener(this)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                uploadProfilePicture(imageUri)
            }
        }
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        lifecycleScope.launch {
            try {
                // Show loading state
                binds.editProfileImageButton.isEnabled = false
                
                // Convert image to base64
                val inputStream = contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                    
                    // Get current user ID from Firebase Auth
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        // Update user profile in Firestore
                        val db = FirebaseFirestore.getInstance()
                        val userRef = db.collection("users").document(currentUser.uid)
                        
                        val updates = hashMapOf<String, Any>(
                            "profilePicture" to base64Image
                        )
                        
                        userRef.set(updates, SetOptions.merge())
                            .addOnSuccessListener {
                                // Update local UI
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                binds.profileImage.setImageBitmap(bitmap)
                                
                                // Update dashboard profile picture
                                updateDashboardProfilePicture(base64Image)
                                
                                Toast.makeText(this@ProfileActivity, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@ProfileActivity, "Failed to update profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this@ProfileActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binds.editProfileImageButton.isEnabled = true
            }
        }
    }

    private fun updateDashboardProfilePicture(base64Image: String) {
        // Send broadcast to update dashboard
        val intent = Intent("PROFILE_PICTURE_UPDATED")
        intent.putExtra("profile_picture", base64Image)
        sendBroadcast(intent)
    }

    private fun setupChartData() {
        // Load transaction data for charts
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Load income/expense data for line chart
        loadIncomeExpenseData(userId)
        
        // Load category budget data for bar chart
        loadCategoryBudgetData(userId)
    }

    private fun loadIncomeExpenseData(userId: String) {
        val db = FirebaseFirestore.getInstance()
        
        // Get all accounts and their transactions
        db.collection("users").document(userId).collection("accounts").get()
            .addOnSuccessListener { accountDocs ->
                val allTransactions = mutableListOf<com.opsc6311.poe.data.models.Transaction>()
                
                accountDocs.forEach { accountDoc ->
                    db.collection("users").document(userId)
                        .collection("accounts").document(accountDoc.id)
                        .collection("transactions").get()
                        .addOnSuccessListener { transactionDocs ->
                            transactionDocs.forEach { transactionDoc ->
                                val transaction = transactionDoc.toObject(com.opsc6311.poe.data.models.Transaction::class.java)
                                transaction?.let { allTransactions.add(it) }
                            }
                            
                            // Update chart when all transactions are loaded
                            if (allTransactions.size == transactionDocs.size()) {
                                updateIncomeExpenseChart(allTransactions)
                            }
                        }
                }
            }
    }

    private fun loadCategoryBudgetData(userId: String) {
        val db = FirebaseFirestore.getInstance()
        
        // Get categories and their budget data
        db.collection("users").document(userId).collection("categories").get()
            .addOnSuccessListener { categoryDocs ->
                val categories = categoryDocs.mapNotNull { it.toObject(com.opsc6311.poe.data.models.Category::class.java) }
                updateCategoryBudgetChart(categories)
            }
    }

    private fun updateIncomeExpenseChart(transactions: List<com.opsc6311.poe.data.models.Transaction>) {
        // Group transactions by month and type
        val monthlyData = transactions.groupBy { transaction ->
            val date = transaction.date.toDate()
            "${date.year + 1900}-${date.month + 1}"
        }
        
        val entries = mutableListOf<com.github.mikephil.charting.data.Entry>()
        val labels = mutableListOf<String>()
        
        monthlyData.entries.sortedBy { it.key }.forEachIndexed { index, (month, monthTransactions) ->
            val income = monthTransactions.filter { it.type == com.opsc6311.poe.data.models.TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.type == com.opsc6311.poe.data.models.TransactionType.EXPENSE }.sumOf { it.amount }
            
            entries.add(com.github.mikephil.charting.data.Entry(index.toFloat(), income.toFloat()))
            entries.add(com.github.mikephil.charting.data.Entry(index.toFloat(), expense.toFloat()))
            labels.add(month)
        }
        
        // Set up line chart
        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Income/Expense")
        dataSet.color = getColor(R.color.primary)
        dataSet.setCircleColor(getColor(R.color.primary))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 6f
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = getColor(R.color.text_primary)
        dataSet.valueTextSize = 12f
        
        val lineData = com.github.mikephil.charting.data.LineData(dataSet)
        binds.profileIncomeExpenseLineChart.data = lineData
        
        // Configure chart appearance
        binds.profileIncomeExpenseLineChart.apply {
            description.isEnabled = false
            legend.textColor = getColor(R.color.text_primary)
            legend.textSize = 14f
            xAxis.textColor = getColor(R.color.text_primary)
            xAxis.textSize = 12f
            axisLeft.textColor = getColor(R.color.text_primary)
            axisLeft.textSize = 12f
            axisRight.isEnabled = false
            setBackgroundColor(getColor(R.color.surface))
            setGridBackgroundColor(getColor(R.color.surface))
        }
        
        android.util.Log.d("ProfileActivity", "Income/Expense chart data entries: $entries")
        binds.profileIncomeExpenseLineChart.invalidate()
    }

    private fun updateCategoryBudgetChart(categories: List<com.opsc6311.poe.data.models.Category>) {
        val entries = mutableListOf<com.github.mikephil.charting.data.BarEntry>()
        
        categories.forEachIndexed { index, category ->
            entries.add(com.github.mikephil.charting.data.BarEntry(index.toFloat(), category.maxBudget.toFloat()))
        }
        
        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Category Budgets")
        dataSet.color = getColor(R.color.primary)
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = getColor(R.color.text_primary)
        dataSet.valueTextSize = 12f
        
        val barData = com.github.mikephil.charting.data.BarData(dataSet)
        binds.profileCategoryBudgetBarChart.data = barData
        
        // Configure chart appearance
        binds.profileCategoryBudgetBarChart.apply {
            description.isEnabled = false
            legend.textColor = getColor(R.color.text_primary)
            legend.textSize = 14f
            xAxis.textColor = getColor(R.color.text_primary)
            xAxis.textSize = 12f
            axisLeft.textColor = getColor(R.color.text_primary)
            axisLeft.textSize = 12f
            axisRight.isEnabled = false
            setBackgroundColor(getColor(R.color.surface))
            setGridBackgroundColor(getColor(R.color.surface))
        }
        
        android.util.Log.d("ProfileActivity", "Category budget chart data entries: $entries")
        binds.profileCategoryBudgetBarChart.invalidate()
    }

    // --- UI Configuration

    private fun setupToolbar() {
        setSupportActionBar(binds.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
    }

    // --- UI

    private fun setupBindings() {
        binds = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binds.root)
    }

    private fun setupLayoutUi() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binds.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        // Set status bar color
        window.statusBarColor = getColor(R.color.surface)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
}
