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

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: FinanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Backup/Restore
        binding.btnBackup.setOnClickListener {
            viewModel.backupData(requireContext())
        }

        binding.btnRestore.setOnClickListener {
            viewModel.restoreData(requireContext())
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            // Navigate back to LoginActivity
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }
    }
}