package com.famoco.kyctelcomr.mattel2.ui.fragments

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
import com.famoco.kyctelcomr.databinding.FragmentPhoneBinding
import com.famoco.kyctelcomr.mattel.ui.fragments.PhoneFragmentDirections
import com.famoco.kyctelcomr.mattel.ui.viewmodels.PhoneViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneFragment : Fragment() {

    private var _binding: FragmentPhoneBinding? = null
    private val viewModel: PhoneViewModel by activityViewModels()

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhoneBinding.inflate(inflater, container, false)
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
            val msisdn = "3" + binding.msisdnEditText.text.toString().replace(" ", "")
            val imsi = "609010" + binding.imsiEditText.text.toString().replace(" ", "")

            if (msisdn.isNotEmpty() && imsi.isNotEmpty()) {
                if (msisdn.length != 11) {
                    binding.msisdnTextInputLayout.error = getString(R.string.msisdn_error)
                } else if (imsi.length != 15 && !binding.precreatedImsiSwitch.isChecked) {
                    binding.imsiTextInputLayout.error = getString(R.string.imsi_number_error)
                } else {
                    viewModel.setCustomerMSISDN(msisdn)
                    if (!binding.precreatedImsiSwitch.isChecked) {
                        viewModel.setCustomerIMSI(imsi)
                    }
                    findNavController().navigate(
                        PhoneFragmentDirections.actionPhoneFragmentToMattelSummaryFragment()
                    )
                }
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.phone_fragment_snackbar_missing_info),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        // Custom formatter for MSISDN (## ## ## ##)
        binding.msisdnEditText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private val spaceInterval = 2

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.msisdnTextInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                s?.let {
                    val digits = it.toString().replace(" ", "")
                    val formatted = StringBuilder()
                    for (i in digits.indices) {
                        formatted.append(digits[i])
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

        // IMSI: digits only (no pattern, just clear error on typing)
        binding.imsiEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.imsiTextInputLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
