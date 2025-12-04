package com.famoco.kyctelcomr.core.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.databinding.FragmentHomeBinding
import com.famoco.kyctelcomr.core.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val mainViewModel: MainViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        mainViewModel.smartCardReaderPlugged.observe(viewLifecycleOwner) {
          //  binding.enrollCardView.isEnabled = it == true
          //  binding.enrollCardView.isClickable = it == true
//            if (it == true) {
//                binding.enrollIcon.setImageResource(R.drawable.ic_person)
//            } else {
//                binding.enrollIcon.setImageResource(R.drawable.ic_person_placeholder)
//            }
        }

        binding.enrollCardView.setOnClickListener {
            mainViewModel.reset()
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToBiometryFragment())
            //findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToMenuFragment())

        }

        binding.logsCardView.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToFaceFragment2())

            // todo nagivate to list record db
         //   Snackbar.make(binding.root, getString(R.string.feature_not_yet_implemented), Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}