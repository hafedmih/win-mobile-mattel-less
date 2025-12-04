package com.famoco.kyctelcomr.mattel.ui.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.core.utils.SMSListener
import com.famoco.kyctelcomr.core.utils.SMSReceiver
import com.famoco.kyctelcomr.core.utils.toDate
import com.famoco.kyctelcomr.core.utils.toDateString
import com.famoco.kyctelcomr.databinding.FragmentMattelSummaryBinding
import com.famoco.kyctelcomr.mattel.model.Customer
import com.famoco.kyctelcomr.mattel.ui.viewmodels.MattelSummaryFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MattelSummaryFragment : Fragment() {

    private var _binding: FragmentMattelSummaryBinding? = null

    private val viewModel: MattelSummaryFragmentViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        _binding = FragmentMattelSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.nextBtn.setOnClickListener {
            binding.pBar.setIndeterminate(true);
            binding.pBar.visibility=View.VISIBLE
            binding.nextBtn.visibility=View.INVISIBLE

                //.visibility(View.VISIBLE);

            viewModel.sendData()
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
        viewModel.messageSendResult.observe(viewLifecycleOwner) {
            if (null != it) {
                if (it) {
                    viewModel.notifyMessageSendResult(false)
                    findNavController().popBackStack(R.id.homeFragment, false)
                }
            }
        }
        viewModel.smsState.observe(viewLifecycleOwner) {
            if (null != it) {
                //binding.sendStateTxt.text = it.second
                if ((!it.first) || (it.first && it.second != "SMS_SENT")) {
                    viewModel.notifyMessageSendResult(true)
                }
            }
        }


    }

    private fun populateView(customer: Customer) {
        binding.msisdnInput.text = String.format(getString(R.string.phone_number_format), customer.msisdn)
       if(customer.option.isNotEmpty()){
           binding.cardNumberInput.text=customer.option
       }else {
           Toast.makeText(activity,"option empty",Toast.LENGTH_LONG).show()
       }
        if (customer.imsi.isNotEmpty()) {
            binding.imsiInput.text = customer.imsi
        } else {
            binding.imsi.visibility = View.GONE
        }
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
        binding.birthdateInput.text = identity.dateOfBirth.toDate().toString() ?: ""
        binding.birthplaceInput.text = identity.placeOfBirth
        binding.expiryDateInput.text = identity.expiryDate.toDate()?.toDateString() ?: ""
        //binding.gpsInput.text =  "15,18"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
