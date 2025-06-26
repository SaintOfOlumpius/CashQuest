package vc.prog3c.poe.ui.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import com.opsc6311.poe.R

class DashboardActivity : AppCompatActivity() {

    private lateinit var budgetProgressBar: LinearProgressIndicator
    private lateinit var budgetProgressText: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var accountsRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var addAccountButton: FloatingActionButton
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        budgetProgressText = findViewById(R.id.budgetProgressText)
        viewPager = findViewById(R.id.viewPager)
        accountsRecyclerView = findViewById(R.id.dashboardAccountsRecyclerView)
        fab = findViewById(R.id.fab)
        addAccountButton = findViewById(R.id.addAccountButton)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Example logic
        budgetProgressBar.progress = 50
        budgetProgressText.text = "50%"
    }
}
