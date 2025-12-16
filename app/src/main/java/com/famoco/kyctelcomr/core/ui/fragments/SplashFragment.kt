package com.famoco.kyctelcomr.core.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.famoco.famocodialog.DialogType
import com.famoco.famocodialog.FamocoDialog
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.databinding.FragmentSplashBinding
import com.famoco.kyctelcomr.core.ui.viewmodels.SplashViewModel
import com.famoco.kyctelcomr.core.utils.famocoCheck.FamocoCheck
import com.famoco.kyctelcomr.core.utils.famocoCheck.FamocoListener
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SplashFragment : Fragment(), FamocoListener {

    companion object {
        private val TAG = SplashFragment::class.java.simpleName
    }

     private var _binding: FragmentSplashBinding? = null

    private val binding get() = _binding!!

    private val splashViewModel: SplashViewModel by viewModels()

    private lateinit var detachSnackbar: Snackbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // detachSnackbar = Snackbar.make(binding.root, getString(R.string.external_devices_deconnection), Snackbar.LENGTH_INDEFINITE)

//        splashViewModel.initStateLiveData.observe(viewLifecycleOwner) {
//            // Do something only when both values are set to true
//            if (it) {
//                Log.i(TAG, "peripherals ready => go to home fragment")
//                findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToHomeFragment())
//            }
//        }
        findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToHomeFragment())

        splashViewModel.loadingTxt.observe(viewLifecycleOwner) {
            binding.loadingTxt.text = it
        }

//        splashViewModel.connectionErrorLiveData.observe(viewLifecycleOwner) {
//            if (it.isEmpty()) {
//                return@observe
//            }
//            Log.w(TAG, "connection error: $it")
//            val dialog = FamocoDialog(requireContext())
//            dialog.setDialogType(DialogType.ERROR)
//                .setTitle(getString(R.string.error_dialog_title))
//                .setContent(String.format(getString(R.string.error_dialog_init_content),
//                    it))
//                .setOnNegativeButtonClicked(getString(R.string.dialog_ok_btn)) {
//                    dialog.dismiss()
//                    findNavController().popBackStack()
//                }
//                .showPositiveButton(false)
//                .show()
//        }
        FamocoCheck(requireContext(), this).verify()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
//        if(BuildConfig.FLAVOR == "mauritel") {
//            binding.loadingTxt.setTextColor(resources.getColor(R.color.white, null))
//        }
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFamocoDevice() {
        Log.i(TAG, "Current device is a Famoco device (or has Famoco Layer)")
        //Init module only if this app is running on Famoco devices (or devices that have FamocoLayer)
        splashViewModel.smartCardReaderPlugged.observe(this) {
            if (it == true) {
                detachSnackbar.dismiss()
                splashViewModel.initModules()
            }
            if (it == false) {
              //  detachSnackbar.show()
            }
        }
    }

    override fun onNotFamocoDevice() {
        Log.w(TAG, "Current device is a non-Famoco device (or has Famoco Layer)")
        val dialog = FamocoDialog(requireContext())
        dialog.setDialogType(DialogType.ERROR)
            .setTitle(getString(R.string.app_running_on_non_famoco_devices_dialog_title))
            .setContent(getString(R.string.app_running_on_non_famoco_devices_dialog_content))
            .setOnNegativeButtonClicked(getString(R.string.dialog_ok_btn)) {
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .showPositiveButton(false)
            .show()
    }
}
