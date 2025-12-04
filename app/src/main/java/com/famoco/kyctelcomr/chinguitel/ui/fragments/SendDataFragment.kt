package com.famoco.kyctelcomr.chinguitel.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.chinguitel.ui.viewmodels.SendDataViewModel
import com.famoco.kyctelcomr.databinding.FragmentSendDataBinding
import com.famoco.kyctelcomr.core.ui.viewmodels.MainViewModel
import com.famoco.kyctelcomr.core.utils.SendDataEnum
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SendDataFragment : Fragment() {

    private var _binding: FragmentSendDataBinding? = null

    private val viewModel: SendDataViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSendDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toggleButton.addOnButtonCheckedListener { _, checkedId, _ ->
            when (checkedId) {
                R.id.sms_btn -> {
                    viewModel.updateSendDataWay(SendDataEnum.SMS)
                }
                R.id.ussd_btn -> {
                    viewModel.updateSendDataWay(SendDataEnum.USSD)
                }
                R.id.web_btn -> {
                    viewModel.updateSendDataWay(SendDataEnum.WEB)
                }
            }
        }

        viewModel.sendData.observe(viewLifecycleOwner) {
            if (it == SendDataEnum.WEB) {
                Snackbar.make(binding.root, getString(R.string.feature_not_yet_implemented), Snackbar.LENGTH_LONG).show()
            }
        }

        binding.sendBtn.setOnClickListener {
            viewModel.sendData()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.smsState.observe(viewLifecycleOwner) {
            if (null != it) {
                binding.sendStateTxt.text = it.second
                if ((!it.first) || (it.first && it.second != "SMS_SENT")) {
                    viewModel.notifyMessageSendResult(true)
                }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
