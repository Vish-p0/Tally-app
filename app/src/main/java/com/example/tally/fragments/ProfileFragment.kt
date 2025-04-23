package com.example.tally.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.tally.R
import com.example.tally.activities.LoginActivity
import com.example.tally.databinding.FragmentProfileBinding
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Backup/Restore
        binding.btnBackup.setOnClickListener {
            showBackupConfirmationDialog()
        }

        binding.btnRestore.setOnClickListener {
            showRestoreConfirmationDialog()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showBackupConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Backup Data")
            .setMessage("Do you want to create a backup of your data?")
            .setPositiveButton("Backup") { _, _ ->
                viewModel.backupData(requireContext())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Data")
            .setMessage("This will replace all your current data with the most recent backup. Are you sure?")
            .setPositiveButton("Restore") { _, _ ->
                viewModel.restoreData(requireContext())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Navigate back to LoginActivity
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                activity?.finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}