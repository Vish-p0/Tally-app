package com.example.tally.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.tally.R
import com.example.tally.adapters.CategoryAdapter
import com.example.tally.databinding.FragmentCategoriesBinding
import com.example.tally.models.Category
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class CategoriesFragment : Fragment() {

    private lateinit var binding: FragmentCategoriesBinding
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        adapter = CategoryAdapter { category ->
            // Handle category edit/delete
            showCategoryOptions(category)
        }
        binding.rvCategories.adapter = adapter

        // Observe categories
        viewModel.categories.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        // Add Category Button
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog(category: Category? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val typeInput = dialogView.findViewById<TextInputEditText>(R.id.typeInput)
        
        // Pre-fill fields if editing
        if (category != null) {
            nameInput.setText(category.name)
            typeInput.setText(category.type)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (category == null) "Add Category" else "Edit Category")
            .setView(dialogView)
            .setPositiveButton(if (category == null) "Add" else "Save") { _, _ ->
                val name = nameInput.text.toString()
                val type = typeInput.text.toString()
                
                if (name.isNotEmpty() && type.isNotEmpty()) {
                    if (category == null) {
                        // Add new category
                        val newCategory = Category(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            type = type
                        )
                        viewModel.addCategory(newCategory)
                    } else {
                        // Update existing category
                        val updatedCategory = category.copy(
                            name = name,
                            type = type
                        )
                        viewModel.updateCategory(updatedCategory)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCategoryOptions(category: Category) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Category Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddCategoryDialog(category)
                    1 -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Category")
                            .setMessage("Are you sure you want to delete this category?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteCategory(category)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }
}