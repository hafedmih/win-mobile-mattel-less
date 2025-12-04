package com.famoco.kyctelcomr.core.ui.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.famocodialog.DialogType
import com.famoco.famocodialog.FamocoDialog
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.databinding.FragmentBiometryBinding
import com.famoco.kyctelcomr.core.ui.viewmodels.MainViewModel
import com.famoco.kyctelcomr.core.utils.toInternalErrorMessage
import com.famoco.kyctelcomr.core.utils.toSensorMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BiometryFragment : Fragment() {

    companion object {
        private val TAG = BiometryFragment::class.java.simpleName
    }

    private var _binding: FragmentBiometryBinding? = null

    private val mainViewModel: MainViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentBiometryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.nextBtn.setOnClickListener {
            if (mainViewModel.onCaptureCompleted.value == true) {
                findNavController().navigate(BiometryFragmentDirections.actionBiometryFragmentToSmartCardFragment())
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.biometry_fragment_capture_fingerprint_before_next),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        mainViewModel.sensorImage.observe(viewLifecycleOwner) {
            binding.sensorImage.setImageBitmap(it)
        }

        mainViewModel.sensorQuality.observe(viewLifecycleOwner) {
            updateProgress(it)
        }

        mainViewModel.sensorMessage.observe(viewLifecycleOwner) {
            //Fingerprint captured case
            if (it == 8) {
                binding.sensorMessage.setTextColor(resources.getColor(R.color.success, null))
                binding.sensorMessage.setTypeface(null, Typeface.BOLD)
            } else {
                binding.sensorMessage.setTextColor(resources.getColor(R.color.black, null))
                binding.sensorMessage.setTypeface(null, Typeface.NORMAL)
            }
            binding.sensorMessage.text = it.toSensorMessage(requireContext())
        }

        mainViewModel.morphoInternalError.observe(viewLifecycleOwner) {
            if (it.first != 0) {
                Log.w(TAG, "morpho internal error received: ${it.first.toInternalErrorMessage()}")
                val dialog = FamocoDialog(context)
                dialog.setDialogType(DialogType.ERROR)
                    .setTitle(getString(R.string.error_dialog_title))
                    .setContent(String.format(getString(R.string.error_dialog_morpho_content), it.first, it.first.toInternalErrorMessage()))
                    .setOnNegativeButtonClicked(getString(R.string.dialog_retry_button)) {
                        dialog.dismiss()
                    }
                    .showPositiveButton(false)
                    .show()
            }
        }
        mainViewModel.capturedTemplateList.observe(viewLifecycleOwner) {
            if (it != null) {
                Log.i(TAG, "template received")
                mainViewModel.setCustomerTemplates(it)
            }
        }

        mainViewModel.captureFingerprint()
    }

    private fun updateProgress(progress: Int) {
        if (progress <= 25) {
            binding.sensorProgress.progressDrawable.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    ContextCompat.getColor(requireContext(), R.color.colorError_bis),
                    BlendModeCompat.SRC_IN
                )
        }
        if (progress in 26..50) {
            binding.sensorProgress.progressDrawable.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    ContextCompat.getColor(requireContext(), R.color.colorError),
                    BlendModeCompat.SRC_IN
                )
        }
        if (progress > 50) {
            binding.sensorProgress.progressDrawable.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    ContextCompat.getColor(requireContext(), R.color.success),
                    BlendModeCompat.SRC_IN
                )
        }
        binding.sensorProgress.progress = progress
    }

    override fun onPause() {
        super.onPause()
        mainViewModel.stopCapture()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}