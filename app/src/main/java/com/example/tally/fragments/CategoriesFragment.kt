package com.example.tally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.databinding.FragmentCategoriesBinding
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryTileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        updateFinancialData()
        observeCategories()
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryTileAdapter(emptyList()) { category ->
            showCategoryOptions(category)
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFinancialData() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateTotalBalance(transactions)
            updateExpenseInfo(transactions)
        }
    }

    private fun updateTotalBalance(transactions: List<Transaction>) {
        val totalBalance = transactions.sumOf { 
            if (it.type == "Income") it.amount else -it.amount 
        }
        binding.tvTotalBalance.text = String.format("$%,.2f", totalBalance)
    }

    private fun updateExpenseInfo(transactions: List<Transaction>) {
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        binding.tvTotalExpense.text = String.format("-$%,.2f", totalExpense)
    }

    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.updateCategories(categories)
        }
    }

    private fun showCategoryOptions(category: Category) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Category Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditCategoryDialog(category)
                    1 -> checkAndDeleteCategory(category)
                }
            }
            .show()
    }

    private fun checkAndDeleteCategory(category: Category) {
        val transactions = viewModel.transactions.value ?: emptyList()
        val hasTransactions = transactions.any { it.categoryId == category.id }
        if (hasTransactions) {
            Toast.makeText(requireContext(), "Cannot delete category with existing transactions", Toast.LENGTH_SHORT).show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete this category?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteCategory(category.id)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val typeInput = dialogView.findViewById<EditText>(R.id.typeInput)
        val emojiInput = dialogView.findViewById<EditText>(R.id.emojiInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                val type = typeInput.text.toString()
                val emoji = emojiInput.text.toString()
                if (name.isNotBlank() && type in listOf("Income", "Expense") && emoji.isNotBlank()) {
                    val newCategory = Category(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        emoji = emoji
                    )
                    viewModel.addCategory(newCategory)
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val typeInput = dialogView.findViewById<EditText>(R.id.typeInput)
        val emojiInput = dialogView.findViewById<EditText>(R.id.emojiInput)

        nameInput.setText(category.name)
        typeInput.setText(category.type)
        emojiInput.setText(category.emoji)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString()
                val type = typeInput.text.toString()
                val emoji = emojiInput.text.toString()
                if (name.isNotBlank() && type in listOf("Income", "Expense") && emoji.isNotBlank()) {
                    val updatedCategory = category.copy(name = name, type = type, emoji = emoji)
                    viewModel.updateCategory(updatedCategory)
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Inner class for the Category Tile Adapter
    inner class CategoryTileAdapter(
        private var categories: List<Category>,
        private val onItemClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryTileAdapter.CategoryViewHolder>() {
        
        // Category background color for all categories (light_green)
        private val categoryColor = "#DFF7E2" // light_green color
        
        fun updateCategories(newCategories: List<Category>) {
            categories = newCategories
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_tile, parent, false)
            return CategoryViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            if (position < categories.size) {
                // Regular category item
                val category = categories[position]
                holder.bind(category)
            } else {
                // "More" item at the end
                holder.bindMoreItem()
            }
        }
        
        override fun getItemCount(): Int = categories.size + 1 // +1 for the "More" item
        
        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val emojiTextView: TextView = itemView.findViewById(R.id.tvEmoji)
            private val nameTextView: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val cardView: CardView = itemView as CardView
            
            fun bind(category: Category) {
                emojiTextView.text = category.emoji
                nameTextView.text = category.name
                
                // Set all category backgrounds to light_green
                try {
                    cardView.setCardBackgroundColor(android.graphics.Color.parseColor(categoryColor))
                } catch (e: Exception) {
                    // Fallback if color parsing fails
                    cardView.setCardBackgroundColor(resources.getColor(R.color.light_green, null))
                }
                
                itemView.setOnClickListener { onItemClick(category) }
            }
            
            fun bindMoreItem() {
                emojiTextView.text = "+"
                nameTextView.text = "More"
                
                // Set a different color for the "More" item
                try {
                    cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                } catch (e: Exception) {
                    // Fallback if color parsing fails
                }
                
                itemView.setOnClickListener { showAddCategoryDialog() }
            }
        }
    }
}