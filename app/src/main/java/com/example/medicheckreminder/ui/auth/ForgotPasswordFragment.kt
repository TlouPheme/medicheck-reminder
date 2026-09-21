package com.example.medicheckreminder.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.example.medicheckreminder.data.repository.AccountResult
import com.example.medicheckreminder.databinding.FragmentForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnReset.setOnClickListener { resetPassword() }
    }

    private fun resetPassword() {
        val email = binding.etEmail.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        if (password != confirm) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            return
        }
        binding.btnReset.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = (requireContext().applicationContext as MediCheckApp)
                .container.accountStore
                .resetPassword(email, password)
            if (!isAdded) return@launch
            binding.btnReset.isEnabled = true
            when (result) {
                AccountResult.Success -> {
                    Snackbar.make(binding.root, R.string.forgot_password_saved, Snackbar.LENGTH_LONG)
                        .show()
                    findNavController().navigateUp()
                }
                is AccountResult.Failure -> {
                    val message = getString(result.messageRes)
                    if (result.messageRes == R.string.error_weak_password) {
                        binding.tilPassword.error = message
                    } else {
                        binding.tilEmail.error = message
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
