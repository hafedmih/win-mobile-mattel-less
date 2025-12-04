package com.famoco.kyctelcomr.mattel.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.databinding.ObservableBoolean
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.DBHelper
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.chinguitel.ui.viewmodels.SendDataViewModel
import com.famoco.kyctelcomr.core.ui.fragments.HomeFragmentDirections
import com.famoco.kyctelcomr.databinding.FragmentMenuListBinding

import com.famoco.kyctelcomr.mattel.ui.viewmodels.PhoneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuFragment : Fragment() {

    private var _binding: FragmentMenuListBinding? = null

    private val viewModel: PhoneViewModel by activityViewModels()
    private val sendDataViewModel: SendDataViewModel by activityViewModels()
    val checkboxEnabled = ObservableBoolean(true)


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMenuListBinding.inflate(inflater, container, false)

        binding.optionPresCree.setOnClickListener {
            viewModel.setCustomerOption("0")

            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToPhoneFragment())
            //findNavController().popBackStack(R.id.homeFragment, false)
        }
        binding.optionIdentification.setOnClickListener {
            viewModel.setCustomerOption("2")
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToPhoneFragment())

        }
        binding.optionEchangeSim.setOnClickListener {
            viewModel.setCustomerOption("3")
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToPhoneFragment())

        }
        binding.optionCreation.setOnClickListener {
            viewModel.setCustomerOption("1")

            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToPhoneFragment())

        }
        if(DBHelper.CURRENT_PROFIL == DBHelper.PROFIL_NORMAL){

            binding.optionPresCree.visibility=View.VISIBLE
            binding.optionEchangeSim.visibility=View.VISIBLE
            binding.optionCreation.visibility=View.VISIBLE
            binding.optionIdentification.visibility=View.VISIBLE

        }else if(DBHelper.CURRENT_PROFIL == DBHelper.PROFIL_SANS_ECHANGE_SIM)
        {
            binding.optionPresCree.visibility=View.VISIBLE
            binding.optionEchangeSim.visibility=View.GONE
            binding.optionCreation.visibility=View.VISIBLE
            binding.optionIdentification.visibility=View.VISIBLE

        }else if(DBHelper.CURRENT_PROFIL == DBHelper.PROFIL_IDENTIFICATION)

         {
             binding.optionPresCree.visibility=View.GONE
             binding.optionEchangeSim.visibility=View.GONE
             binding.optionCreation.visibility=View.GONE
             binding.optionIdentification.visibility=View.VISIBLE

        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}