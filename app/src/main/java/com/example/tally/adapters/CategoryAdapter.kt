package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.databinding.ItemCategoryBinding
import com.example.tally.models.Category

class CategoryAdapter(
    private var categories: List<Category>,
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnLongClickListener {
                onDeleteClick(categories[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        with(holder.binding) {
            tvCategoryName.text = "${category.emoji} ${category.name}"
            tvCategoryType.text = category.type
            btnEdit.setOnClickListener { onEditClick(category) }
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}