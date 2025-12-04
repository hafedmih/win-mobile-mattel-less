package com.famoco.kyctelcomr.core.ui.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.famoco.famocodialog.DialogType
import com.famoco.famocodialog.FamocoDialog
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.databinding.FragmentFaceBinding
import com.famoco.kyctelcomr.core.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class FaceFragment : Fragment() {

    private lateinit var capturedImage: Bitmap
    private lateinit var imageCapture: ImageCapture

    companion object {
        private val TAG = FaceFragment::class.java.simpleName
    }

    private var _binding: FragmentFaceBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()

    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFaceBinding.inflate(inflater, container, false)


        startCamera()

        binding.captureButton.setOnClickListener {
            captureImage()
        }

        return binding.root
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview Use Case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // ImageCapture Use Case
            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
          //  val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Must unbind the use-cases before rebinding them
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("FaceFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    private fun captureImage() {
        // Create a file to save the image
        val photoFile = File(requireContext().filesDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Handle the saved image
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    // Display the image, update ViewModel, etc.
                    binding.capturedImageView.setImageURI(savedUri)
                    binding.capturedImageView.visibility = View.VISIBLE // Show the ImageView
                    binding.previewView.visibility = View.GONE // Hide the PreviewView
                    val capturedBitmap = BitmapFactory.decodeFile(savedUri.path)

                    mainViewModel.updateCapturedImage(capturedBitmap)

                    //   Toast.makeText(requireContext(), "Image saved: $savedUri", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("FaceFragment", "Image capture failed", exception)
                    Toast.makeText(requireContext(), "Image capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    override fun onResume() {
        super.onResume()


        binding.nextBtn.setOnClickListener {
            if (mainViewModel.capturedImage.value != null) {
//                findNavController().navigate(
//
//                    FaceFragmentDirections.actionFaceFragment2ToSmartCardFragment()
//                )
                findNavController().navigate(
                    FaceFragmentDirections.actionFaceFragment2ToSmartCardFragment()
                )
            } else {
                Snackbar.make(
                    binding.root,
                    "Please take picture before going on the next stage.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        mainViewModel.capturedImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                //binding.faceImage.setImageBitmap(bitmap)
            }
        }

       // dispatchTakePictureIntent()
    }

//    private fun dispatchTakePictureIntent() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
//            takePictureLauncher.launch(takePictureIntent)  // Use the new API here
//        } else {
//            Toast.makeText(requireContext(), "Unable to open camera", Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
