package com.example.medicheckreminder.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.FragmentRegisterBinding
import com.example.medicheckreminder.util.PasswordValidator
import com.google.android.material.color.MaterialColors

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        setupFooter()
        setupPasswordStrength()

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                // Perform registration logic
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
        }
    }

    private fun setupPasswordStrength() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s?.toString() ?: ""
                updateStrengthUI(password)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateStrengthUI(password: String) {
        val strength = PasswordValidator.calculateStrength(password)
        if (strength == null) {
            binding.progressStrength.visibility = View.GONE
            binding.textStrengthLabel.visibility = View.GONE
            return
        }

        binding.progressStrength.visibility = View.VISIBLE
        binding.textStrengthLabel.visibility = View.VISIBLE

        val (color, labelRes, progress) = when (strength) {
            PasswordValidator.Strength.WEAK -> Triple(
                MaterialColors.getColor(binding.textStrengthLabel, com.google.android.material.R.attr.colorError),
                R.string.password_strength_weak,
                33
            )
            PasswordValidator.Strength.MEDIUM -> Triple(
                MaterialColors.getColor(binding.textStrengthLabel, com.google.android.material.R.attr.colorPrimary),
                R.string.password_strength_medium,
                66
            )
            PasswordValidator.Strength.STRONG -> Triple(
                MaterialColors.getColor(binding.textStrengthLabel, com.google.android.material.R.attr.colorSecondary),
                R.string.password_strength_strong,
                100
            )
        }

        binding.progressStrength.setIndicatorColor(color)
        binding.progressStrength.progress = progress
        binding.textStrengthLabel.text = getString(labelRes)
        binding.textStrengthLabel.setTextColor(color)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val email = binding.etEmail.text.toString()
        if (!PasswordValidator.validateEmail(email)) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        val password = binding.etPassword.text.toString()
        if (!PasswordValidator.isComplexEnough(password)) {
            binding.tilPassword.error = getString(R.string.error_weak_password)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        val confirmPassword = binding.etConfirmPassword.text.toString()
        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun setupFooter() {
        val loginAction = getString(R.string.login_action)
        val fullText = getString(R.string.login_footer, loginAction)
        
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(loginAction)
        val end = start + loginAction.length

        if (start != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    findNavController().popBackStack()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.isFakeBoldText = true
                    ds.color = MaterialColors.getColor(
                        binding.textLoginFooter,
                        com.google.android.material.R.attr.colorPrimary
                    )
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.textLoginFooter.text = spannable
        binding.textLoginFooter.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
