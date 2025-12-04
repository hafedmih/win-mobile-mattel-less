package com.famoco.kyctelcomr.mattel.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.chinguitel.ui.viewmodels.SendDataViewModel
import com.famoco.kyctelcomr.core.ui.MainActivity
import com.famoco.kyctelcomr.databinding.FragmentPhoneBinding
import com.famoco.kyctelcomr.mattel.ui.viewmodels.MattelSummaryFragmentViewModel
import com.famoco.kyctelcomr.mattel.ui.viewmodels.PhoneViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneFragment : Fragment() {

    private var _binding: FragmentPhoneBinding? = null
    private val viewModel: PhoneViewModel by activityViewModels()
    private val viewModel2: MattelSummaryFragmentViewModel by activityViewModels()
    private val sendDataViewModel: SendDataViewModel by activityViewModels()

    private var choixSelected = "0"
    private val binding get() = _binding!!

    val otpPin = (Math.random() * 9000).toInt() + 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhoneBinding.inflate(inflater, container, false)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Disable default back
        }
        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        viewModel2.customer.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            choixSelected = it.option

            when (choixSelected) {
                "1" -> {
                    binding.imsiEditText.text?.clear()
                    binding.imsiDescriptionText.visibility = View.VISIBLE
                    binding.imsiTextInputLayout.visibility = View.VISIBLE
                    binding.otpDescriptionText.visibility = View.GONE
                    binding.OtpTextInputLayout.visibility = View.GONE
                    (activity as MainActivity).supportActionBar?.title = "Creation"
                }
                "0" -> {
                    binding.imsiDescriptionText.visibility = View.GONE
                    binding.imsiTextInputLayout.visibility = View.GONE
                    binding.otpDescriptionText.visibility = View.GONE
                    binding.OtpTextInputLayout.visibility = View.GONE
                    (activity as MainActivity).supportActionBar?.title = "IMSI pres-cree"
                }
                "2" -> {
                    binding.otpDescriptionText.visibility = View.VISIBLE
                    binding.OtpTextInputLayout.visibility = View.VISIBLE
                    binding.imsiDescriptionText.visibility = View.GONE
                    binding.imsiTextInputLayout.visibility = View.GONE
                    (activity as MainActivity).supportActionBar?.title = "Identification"
                }
                "3" -> {
                    binding.imsiEditText.text?.clear()
                    binding.imsiDescriptionText.visibility = View.VISIBLE
                    binding.imsiTextInputLayout.visibility = View.VISIBLE
                    binding.otpDescriptionText.visibility = View.GONE
                    binding.OtpTextInputLayout.visibility = View.GONE
                    (activity as MainActivity).supportActionBar?.title = "Echange sim"
                }
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(activity!!, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.nextBtn.setOnClickListener {
            val msisdn = "3" + binding.msisdnEditText.text.toString().replace(" ", "")
            val imsi = "609010" + binding.imsiEditText.text.toString().replace(" ", "")
            val otp = binding.otpEditText.text.toString()

            if (choixSelected == "2" && otp != otpPin.toString()) {
                Snackbar.make(binding.root, "otp incorrect", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (msisdn.isNotEmpty() && imsi.isNotEmpty()) {
                if (msisdn.length != 8) {
                    binding.msisdnTextInputLayout.error = getString(R.string.msisdn_error)
                }
                //else if (imsi.length != 15 && choixSelected != "0") {
                 //   binding.imsiTextInputLayout.error = getString(R.string.imsi_number_error)
                //}
                else {
                    viewModel.setCustomerMSISDN(msisdn)
                    viewModel.setCustomerIMSI(imsi)
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
            private val groupSize = 2
            private val maxDigits = 8   // digits after the prefix

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.msisdnTextInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val fullText = s.toString()

                // Find first digit group start (after prefix)
                // Example: "+2223" → prefix ends at index 5
                val prefixEnd = fullText.indexOfFirst { it.isDigit() }.let { firstDigit ->
                    // Find where prefix ends → where user starts typing the MSISDN
                    if (firstDigit == -1) {
                        isFormatting = false
                        return
                    }
                    // Move to end of contiguous digit prefix
                    var i = firstDigit
                    while (i < fullText.length && fullText[i].isDigit()) i++
                    i
                }

                val prefix = fullText.substring(0, prefixEnd)

                // The digits AFTER the prefix (to be formatted)
                var digits = fullText.substring(prefixEnd).replace(" ", "")

                // Limit to max 8 digits
                if (digits.length > maxDigits)
                    digits = digits.substring(0, maxDigits)

                // Format digits as XX XX XX XX
                val formattedDigits = StringBuilder()
                for (i in digits.indices) {
                    formattedDigits.append(digits[i])
                    if ((i + 1) % groupSize == 0 && (i + 1) < digits.length)
                        formattedDigits.append(" ")
                }

                val finalText = prefix + formattedDigits.toString()

                if (finalText != fullText) {
                    s.replace(0, s.length, finalText)
                }

                isFormatting = false
            }
        })

        // IMSI: digits only, clear error on typing
        binding.imsiEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.imsiTextInputLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.otpDescriptionText.setOnClickListener {
            val phoneNumber = binding.msisdnEditText.text.toString()
                .replace(" ", "")
                .replace("+222", "")

            sendDataViewModel.sendOTP("3$phoneNumber", otpPin.toString())
            Snackbar.make(binding.root, "SMS OTP sent 3$phoneNumber", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onBackPressed() {
        val fragmentManager: FragmentManager? = fragmentManager
        fragmentManager?.popBackStack(
            fragmentManager.getBackStackEntryAt(fragmentManager.backStackEntryCount - 2).id,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }
}
