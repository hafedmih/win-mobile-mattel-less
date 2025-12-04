package com.famoco.kyctelcomr.core.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Visibility
import com.famoco.kyctelcomr.BuildConfig
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.databinding.FragmentSmartCardBinding
import com.famoco.kyctelcomr.core.ui.viewmodels.MainViewModel
import com.famoco.kyctelcomr.face.FaceViewModel
import com.famoco.kyctelcomr.face.services.facenet.FaceNetService
import com.famoco.kyctelcomr.face.services.mtcnn.MTCNN
import com.famoco.kyctelcomrtlib.smartcard.CardState
import com.famoco.kyctelcomrtlib.smartcard.FingerEnum
import com.famoco.kyctelcomrtlib.smartcard.Operation
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class SmartCardFragment : Fragment() {

    companion object {
        private val TAG = SmartCardFragment::class.java.simpleName
    }

    private var _binding: FragmentSmartCardBinding? = null
    private var isMatchingInProgress = false

// Before calling match2


    private val mainViewModel: MainViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSmartCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.left_finger_btn -> { mainViewModel.updateChosenFinger(FingerEnum.LEFT) }
                    R.id.right_finger_btn -> { mainViewModel.updateChosenFinger(FingerEnum.RIGHT) }
                }


            }
        }
        if(mainViewModel.capturedImage.value !=null) {
            binding.leftFingerBtn.visibility = View.INVISIBLE
            binding.rightFingerBtn.visibility = View.INVISIBLE
            binding.chooseFingerTxt.visibility = View.INVISIBLE
        }

        mainViewModel.detectedIsoDep.observe(viewLifecycleOwner) { isoDep ->
            if (isoDep != null) {
                Log.i(TAG, "Card detected via NFC, starting flow...")
                // Step 1: Get Card Number using the IsoDep object
                mainViewModel.getCardNumber(isoDep)
            }
        }

        mainViewModel.attemptLeft.observe(viewLifecycleOwner) {
            if (null != it) {
                Log.i(TAG, "attempt left received")
                binding.attemptLeft.text = resources.getQuantityString(R.plurals.attempt_left_txt, it, it)
            }
        }

        mainViewModel.cardNumber.observe(viewLifecycleOwner) { cardNumber ->
            if (!cardNumber.isNullOrEmpty()) {
                Log.i(TAG, "Card number received: $cardNumber")

                // Retrieve the current IsoDep object to continue the session
                val currentIsoDep = mainViewModel.detectedIsoDep.value

                if (currentIsoDep != null && currentIsoDep.isConnected) {
                    // Step 2: Get Identity
                    Log.i(TAG, "Requesting Identity...")
                    mainViewModel.getIdentity(currentIsoDep)

                    // Optional: Also trigger matchOnCard here if your flow requires it immediately,
                    // but usually you wait for identity to return to show UI first.
                } else {
                    Log.e(TAG, "Cannot get Identity: IsoDep is null or disconnected")
                }
            }
        }

        mainViewModel.identity.observe(viewLifecycleOwner) { identity ->
            if ((null != identity) && (identity.personalNumber.isNotEmpty())) {
                Log.i(TAG, "Identity received")

                // Determine if we do Face Match or Fingerprint Match on Card
                if (mainViewModel.capturedImage.value != null) {
                    // --- FACE MATCH FLOW ---
                    handleFaceMatch()
                } else {
                    // --- FINGERPRINT MATCH ON CARD FLOW ---
                    val currentIsoDep = mainViewModel.detectedIsoDep.value
                    if (currentIsoDep != null && currentIsoDep.isConnected) {
                        Log.i(TAG, "Starting Match on Card...")
                        // Step 3: Match On Card
                        mainViewModel.matchOnCard(currentIsoDep)
                    }
                }
            }
        }
        mainViewModel.apduErrorMessage.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        mainViewModel.matchLiveData.observe(viewLifecycleOwner) {
            if (it == true) {
                Log.i(TAG, "match succeed")
                hideLoader()
                binding.resultImageView.visibility = View.VISIBLE
                binding.descriptionText.text =
                    getString(R.string.smartcard_fragment_match_on_card_ok)
                binding.resultImageView.setImageResource(R.drawable.ic_check)
                mainViewModel.notifyMatchResult(true)
            }

            if (it == false) {
                Log.i(TAG, "match failed")
              hideLoader()
                binding.resultImageView.visibility = View.VISIBLE
                binding.descriptionText.text =
                    getString(R.string.smartcard_fragment_match_on_card_ko)
                binding.resultImageView.setImageResource(R.drawable.ic_block)
                mainViewModel.notifyMatchResult(false)
            }
        }

        mainViewModel.applyMatchingResult.observe(viewLifecycleOwner) {
            if (it == true) {
                if (findNavController().currentDestination?.id == R.id.smartCardFragment) {
                    findNavController().navigate(SmartCardFragmentDirections.actionSmartCardFragmentToMenuFragment())
                }
            }

            if (it == false) {
                findNavController().popBackStack()
            }
        }

        mainViewModel.smartCardReaderScanState.observe(viewLifecycleOwner) {
            when (it) {
                Operation.IDLE -> {
                    if (mainViewModel.matchLiveData.value == null) {
                        initAnimation()
                        binding.descriptionText.text = getString(R.string.smartcard_fragment_idle_state)
                        binding.arrows.motionLayout.visibility = View.VISIBLE
                        hideLoader()
                        binding.resultImageView.visibility = View.GONE
                        binding.leftFingerBtn.isEnabled = true
                        binding.rightFingerBtn.isEnabled = true
                    }
                }
                Operation.ASK_CARD_NUMBER -> {
                    binding.descriptionText.text = getString(R.string.smartcard_fragment_do_not_remove_card)
                    binding.arrows.motionLayout.visibility = View.GONE
                   showLoader()
                    binding.resultImageView.visibility = View.GONE
                    binding.leftFingerBtn.isEnabled = false
                    binding.rightFingerBtn.isEnabled = false
                }
                Operation.ASK_IDENTITY -> {
                    binding.arrows.motionLayout.visibility = View.GONE
                    showLoader()
                    binding.resultImageView.visibility = View.GONE
                    binding.leftFingerBtn.isEnabled = false
                    binding.rightFingerBtn.isEnabled = false
                }
                Operation.ASK_MATCH_ON_CARD -> {
                    binding.descriptionText.text = getString(R.string.smartcard_fragment_match_on_card_pending)
                    binding.arrows.motionLayout.visibility = View.GONE
                    hideLoader()
                    binding.resultImageView.visibility = View.GONE
                    binding.leftFingerBtn.isEnabled = false
                    binding.rightFingerBtn.isEnabled = false
                }
            }
        }
    }

    private fun initAnimation() {
        binding.arrows.motionLayout.visibility = View.VISIBLE
       hideLoader()
        binding.resultImageView.visibility = View.GONE
        binding.arrows.motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
                //Nothing to do
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                //Nothing to do
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (motionLayout?.currentState == R.id.start) {
                    motionLayout.transitionToEnd()
                } else {
                    motionLayout?.transitionToStart()
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
                //Nothing to do
            }

        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    private fun showLoader() {
        binding.loader.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        if (!isMatchingInProgress) { // Check if matching is in progress
            binding.loader.visibility = View.GONE
        }
    }
    private fun handleFaceMatch() {
        binding.leftFingerBtn.visibility = View.INVISIBLE
        binding.rightFingerBtn.visibility = View.INVISIBLE

        val faceNetService = FaceNetService(context = requireContext())
        val mtcnn = MTCNN(requireContext())
        mtcnn.load()
        faceNetService.load()
        showLoader()
        isMatchingInProgress = true

        val viewModel = FaceViewModel(faceNetService, mtcnn)
        val identityPhoto = mainViewModel.identity.value!!.photo!!
        val capturedPhoto = mainViewModel.capturedImage.value!!

        viewModel.match2(bitmapToByteArray(identityPhoto), capturedPhoto) { score ->
            if (score >= 0.8) {
                binding.resultImageView.visibility = View.VISIBLE
                binding.descriptionText.text = getString(R.string.smartcard_fragment_match_on_card_ok)
                binding.resultImageView.setImageResource(R.drawable.ic_check)
                mainViewModel.notifyMatchResult(true)
            } else {
                binding.resultImageView.visibility = View.VISIBLE
                binding.descriptionText.text = getString(R.string.smartcard_fragment_match_on_card_ko)
                binding.resultImageView.setImageResource(R.drawable.ic_block)
                mainViewModel.notifyMatchResult(false)
            }
            isMatchingInProgress = false
            hideLoader()
        }
    }
}