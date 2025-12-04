package com.famoco.kyctelcomr.chinguitel.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.chinguitel.model.Customer
import com.famoco.kyctelcomr.chinguitel.ui.viewmodels.SummaryFragmentViewModel
import com.famoco.kyctelcomr.databinding.FragmentSummaryBinding
import com.famoco.kyctelcomr.core.utils.toDate
import com.famoco.kyctelcomr.core.utils.toDateString
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null

    private val viewModel: SummaryFragmentViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.nextBtn.setOnClickListener {
            findNavController().navigate(SummaryFragmentDirections.actionSummaryFragmentToSendDataFragment())
        }

        viewModel.customer.observe(viewLifecycleOwner) {
            if (it == null) {
                return@observe
            }
            populateView(it)
        }
        viewModel.identity.observe(viewLifecycleOwner) {
            if (it != null) {
                populateCardView(it)
            }
        }
    }

    private fun populateView(customer: Customer) {
        binding.phoneNumberInput.text = String.format(getString(R.string.phone_number_format), customer.phoneNumber)
    }

    private fun populateCardView(identity: com.famoco.kyctelcomrtlib.smartcard.Identity) {
        if(null != identity.photo) {
            binding.identityPhoto.setImageBitmap(identity.photo)
        } else {
            binding.identityPhoto.setImageResource(R.drawable.ic_person_placeholder)
        }

        binding.cardNumberInput.text = viewModel.cardNumber.value
        binding.personalNumberInput.text = identity.personalNumber
        binding.firstNameInput.text = identity.firstName
        binding.fatherGivenNameInput.text = identity.fatherFirstName
        binding.lastNameInput.text = identity.lastName
        binding.sexInput.text = identity.sex
        binding.birthdateInput.text = identity.dateOfBirth.toDate()?.toDateString() ?: ""
        binding.birthplaceInput.text = identity.placeOfBirth
        binding.expiryDateInput.text = identity.expiryDate.toDate()?.toDateString() ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
