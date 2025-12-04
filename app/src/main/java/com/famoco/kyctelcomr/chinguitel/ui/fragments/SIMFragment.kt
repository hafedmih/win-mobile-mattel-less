package com.famoco.kyctelcomr.chinguitel.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.chinguitel.ui.viewmodels.SIMViewModel
import com.famoco.kyctelcomr.databinding.FragmentSimBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SIMFragment : Fragment() {

    private var _binding: FragmentSimBinding? = null
    private val viewModel: SIMViewModel by activityViewModels()

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.precreatedImsiSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.imsiEditText.text?.clear()
            binding.imsiDescriptionText.visibility = if (!isChecked) View.VISIBLE else View.GONE
            binding.imsiTextInputLayout.visibility = if (!isChecked) View.VISIBLE else View.GONE
        }

        binding.nextBtn.setOnClickListener {
            val phoneNumber = binding.editText.text.toString().replace(" ", "")
            val imsi = binding.imsiEditText.text.toString()

            if (phoneNumber.isNotEmpty()) {
                if (phoneNumber.length != 11) {
                    binding.textInputLayout.error = getString(R.string.phone_number_error)
                } else {
                    viewModel.setCustomerPhoneNumber(phoneNumber)
                    if (!binding.precreatedImsiSwitch.isChecked) {
                        viewModel.setCustomerIMSI(imsi)
                    }
                    findNavController().navigate(
                        SIMFragmentDirections.actionSimFragmentToSummaryFragment()
                    )
                }
            } else {
                binding.textInputLayout.error = getString(R.string.sim_fragment_edit_text_error)
                Snackbar.make(
                    binding.root,
                    getString(R.string.sim_fragment_snackbar_missing_phone_number),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        // Custom TextWatcher for phone number formatting
        binding.editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private val spaceInterval = 2

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Clear error on typing
                binding.textInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                s?.let {
                    val digits = it.toString().replace(" ", "") // Remove spaces
                    val formatted = StringBuilder()

                    for (i in digits.indices) {
                        formatted.append(digits[i])
                        // Add space after every 2 digits except at the end
                        if ((i + 1) % spaceInterval == 0 && i + 1 < digits.length) {
                            formatted.append(" ")
                        }
                    }

                    val newText = formatted.toString()
                    if (newText != it.toString()) {
                        it.replace(0, it.length, newText)
                    }
                }

                isFormatting = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
