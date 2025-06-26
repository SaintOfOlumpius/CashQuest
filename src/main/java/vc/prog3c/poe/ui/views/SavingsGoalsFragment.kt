package vc.prog3c.poe.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.opsc6311.poe.databinding.FragmentSavingsGoalsBinding
import vc.prog3c.poe.ui.adapters.SavingsGoalAdapter
import vc.prog3c.poe.ui.viewmodels.DashboardViewModel

class SavingsGoalsFragment : Fragment() {
    private var _binding: FragmentSavingsGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var savingsGoalAdapter: SavingsGoalAdapter
    private lateinit var model: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavingsGoalsBinding.inflate(inflater, container, false)
        model = (activity as DashboardView).model // Get the ViewModel from the activity

        setupRecyclerView()
        observeSavingsGoals()

        return binding.root
    }

    private fun setupRecyclerView() {
        savingsGoalAdapter = SavingsGoalAdapter()
        binding.savingsGoalsRecyclerView.apply {
            adapter = savingsGoalAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun observeSavingsGoals() {
        model.savingsGoals.observe(viewLifecycleOwner) { goals ->
            savingsGoalAdapter.submitList(goals)
            binding.noSavingsGoalsText.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
