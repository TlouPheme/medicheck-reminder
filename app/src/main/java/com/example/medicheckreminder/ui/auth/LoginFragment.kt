package com.example.medicheckreminder.ui.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupFooter()
        setupGoogleSignIn()

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

    /**
     * Google sign-in is shown only when Firebase is configured (google-services.json present),
     * so a build without it still offers email login instead of a button that cannot work.
     */
    private fun setupGoogleSignIn() {
        val helper = GoogleSignInHelper(requireContext())
        val accountStore = (requireContext().applicationContext as MediCheckApp).container.accountStore
        val available = accountStore.isGoogleSignInAvailable && helper.webClientId != null
        binding.layoutDivider.isVisible = available
        binding.btnGoogle.isVisible = available
        if (!available) {
            Log.d(TAG, "Google sign-in hidden: Firebase is not configured")
            return
        }

        binding.btnGoogle.setOnClickListener {
            if (!helper.isOnline()) {
                showMessage(R.string.error_no_network)
                return@setOnClickListener
            }
            setBusy(true)
            viewLifecycleOwner.lifecycleScope.launch {
                val message: Int? = try {
                    val idToken = helper.requestIdToken(requireActivity())
                    when (val result = accountStore.signInWithGoogle(idToken)) {
                        AccountResult.Success -> null
                        is AccountResult.Failure -> result.messageRes
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Google sign-in timed out", e)
                    R.string.error_no_network
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Google sign-in failed", e)
                    GoogleSignInErrors.messageFor(e) ?: CANCELLED
                }
                if (!isAdded) return@launch
                setBusy(false)
                when (message) {
                    null -> findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    CANCELLED -> Log.d(TAG, "Google sign-in cancelled by the user")
                    else -> showMessage(message)
                }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.btnGoogle.isEnabled = !busy
        binding.btnLogin.isEnabled = !busy
    }

    private fun showMessage(messageRes: Int) {
        Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
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

    private companion object {
        const val TAG = "LoginFragment"
        const val CANCELLED = 0
    }
}
