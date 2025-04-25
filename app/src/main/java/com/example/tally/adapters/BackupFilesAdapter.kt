package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying a list of backup files with radio button selection.
 */
class BackupFilesAdapter(
    private val backupFiles: List<Pair<File, Date>>,
    private val onBackupSelected: (File) -> Unit
) : RecyclerView.Adapter<BackupFilesAdapter.BackupViewHolder>() {

    private var selectedPosition = -1

    inner class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton = itemView.findViewById(R.id.radioButton)
        val tvBackupName: TextView = itemView.findViewById(R.id.tvBackupName)
        val tvBackupDate: TextView = itemView.findViewById(R.id.tvBackupDate)
        val tvBackupSize: TextView = itemView.findViewById(R.id.tvBackupSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_backup_file, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val (file, date) = backupFiles[position]
        
        // Format the date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy (HH:mm)", Locale.getDefault())
        val formattedDate = dateFormat.format(date)
        
        // Format the file size
        val fileSizeKB = file.length() / 1024
        val fileSizeMB = fileSizeKB / 1024.0
        val fileSizeFormatted = if (fileSizeMB < 1.0) {
            "$fileSizeKB KB"
        } else {
            String.format("%.2f MB", fileSizeMB)
        }
        
        // Set the view data
        holder.tvBackupName.text = file.name
        holder.tvBackupDate.text = formattedDate
        holder.tvBackupSize.text = fileSizeFormatted
        
        // Set radio button state
        holder.radioButton.isChecked = position == selectedPosition
        
        // Set click listeners
        val itemClickListener = View.OnClickListener {
            if (selectedPosition != holder.adapterPosition) {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                
                // Update radio buttons
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                
                // Notify selected file
                onBackupSelected(backupFiles[selectedPosition].first)
            }
        }
        
        // Apply click listener to the whole item and the radio button
        holder.itemView.setOnClickListener(itemClickListener)
        holder.radioButton.setOnClickListener(itemClickListener)
    }

    override fun getItemCount(): Int = backupFiles.size
    
    /**
     * Returns the currently selected backup file or null if nothing is selected.
     */
    fun getSelectedBackupFile(): File? {
        return if (selectedPosition >= 0 && selectedPosition < backupFiles.size) {
            backupFiles[selectedPosition].first
        } else {
            null
        }
    }
    
    /**
     * Sets the selected position to the most recent backup file.
     */
    fun selectMostRecentBackup() {
        if (backupFiles.isNotEmpty()) {
            selectedPosition = 0  // The list is already sorted by date, newest first
            notifyItemChanged(selectedPosition)
            onBackupSelected(backupFiles[selectedPosition].first)
        }
    }
} 