package com.example.medicheckreminder.ui.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.example.medicheckreminder.data.repository.AccountResult
import com.example.medicheckreminder.databinding.FragmentLoginBinding
import com.example.medicheckreminder.util.PasswordValidator
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupFooter()
        binding.layoutDivider.isVisible = false
        binding.btnGoogle.isVisible = false

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            if (!PasswordValidator.validateEmail(email.trim())) {
                binding.tilEmail.error = getString(R.string.error_invalid_email)
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.btnLogin.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = (requireContext().applicationContext as MediCheckApp)
                    .container.accountStore
                    .login(email, password)
                if (!isAdded) return@launch
                binding.btnLogin.isEnabled = true
                when (result) {
                    AccountResult.Success ->
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    is AccountResult.Failure -> {
                        if (result.messageRes == R.string.error_invalid_email) {
                            binding.tilEmail.error = getString(result.messageRes)
                        } else {
                            binding.tilPassword.error = getString(result.messageRes)
                        }
                    }
                }
            }
        }

        binding.btnForgotPassword.setOnClickListener {
             findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    private fun setupFooter() {
        val registerAction = getString(R.string.register_action)
        val fullText = getString(R.string.register_footer, registerAction)
        
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(registerAction)
        val end = start + registerAction.length

        if (start != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.isFakeBoldText = true
                    ds.color = MaterialColors.getColor(
                        binding.textRegisterFooter,
                        com.google.android.material.R.attr.colorPrimary
                    )
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.textRegisterFooter.text = spannable
        binding.textRegisterFooter.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
