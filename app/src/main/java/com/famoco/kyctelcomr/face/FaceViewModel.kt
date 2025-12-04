package com.famoco.kyctelcomr.face


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famoco.kyctelcomr.face.services.facenet.FaceNetService
import com.famoco.kyctelcomr.face.services.facenet.MyUtil
import com.famoco.kyctelcomr.face.services.mtcnn.MTCNN
import com.ws.idcheck.services.ImageTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class FaceViewState {
    LOADING,
    CAPTURE,
    CAPTURED,
    MATCHING,
    MATCHING_RESULT
}

class FaceViewModel (
    private val faceNetService: FaceNetService,
    private val mtcnn: MTCNN,
  //  private val cardReaderService: CardReaderService
): ViewModel() {

    val viewState = MutableLiveData(FaceViewState.LOADING)

    val isFlashOn = MutableLiveData(false)
    val cardError = MutableLiveData<String>(null)
    val capturedImage = MutableLiveData<Bitmap>(null)
   // val matchingResult = MutableLiveData<AuthResult?>(null)

    init {
//        viewModelScope.launch(Dispatchers.IO) {
//            cardReaderService.cardEvents.distinctUntilChanged().collect {
//                when (it) {
//                    SmartCardReaderStatus.CARD_DETECTED -> {
//                        viewState.postValue(FaceViewState.CAPTURE)
//                    }
//                    else -> {
//                        viewState.postValue(FaceViewState.CAPTURE)
//                    }
//                }
//            }
//        }
    }

    fun imageCaptured(bitmap: Bitmap) {
        capturedImage.postValue(bitmap)
        viewState.postValue(FaceViewState.CAPTURED)
        isFlashOn.postValue(false)
    }

    fun match (cardImage: ByteArray) {
        viewState.postValue(FaceViewState.MATCHING)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val profileImage = ImageTools.jp200ToBitmap(cardImage!!)
                val faces1 = mtcnn.detectFaces(capturedImage.value!!, 30)
                val faces2 = mtcnn.detectFaces(profileImage, 30)

                if (faces1.isEmpty() || faces2.isEmpty() || faces1.size > 1 || faces2.size > 1) {
                  //  matchingResult.postValue(AuthResult.FAILURE)
                    viewState.postValue(FaceViewState.MATCHING_RESULT)
                    return@withContext
                }

                val faceCrop1 = MyUtil.crop(capturedImage.value!!, faces1[0].transform2Rect())
                val faceCrop2 = MyUtil.crop(profileImage, faces2[0].transform2Rect())

                val score = faceNetService.matchFaces(faceCrop1, faceCrop2)
                Log.i("score",score.toString())
              //  matchingResult.postValue(if (score > 0.8) AuthResult.SUCCESS else AuthResult.FAILURE)
                viewState.postValue(FaceViewState.MATCHING_RESULT)
            }
        }
    }

    fun match2(cardImage1: ByteArray, cardImage2: Bitmap, onScoreComputed: (Float) -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val profileImage = ImageTools.jp200ToBitmap(cardImage1)
                val faces1 = mtcnn.detectFaces(cardImage2, 30)
                val faces2 = mtcnn.detectFaces(profileImage, 30)

                if (faces1.isEmpty() || faces2.isEmpty() || faces1.size > 1 || faces2.size > 1) {
                    // Handle failure cases, e.g., multiple faces or no face detected
                    Log.i("Face Matching", "Face detection failed or multiple faces detected")
                    return@withContext
                }

                // Crop the detected faces
                val faceCrop1 = MyUtil.crop(cardImage2, faces1[0].transform2Rect())
                val faceCrop2 = MyUtil.crop(profileImage, faces2[0].transform2Rect())

                // Compute the similarity score
                val score = faceNetService.matchFaces(faceCrop1, faceCrop2)

                // Log the score
                Log.i("Face Matching Score", score.toString())

                // Pass the score to the callback function
                withContext(Dispatchers.Main) {
                    onScoreComputed(score)
                }
            }
        }
    }

    fun retry() {
        viewState.postValue(FaceViewState.CAPTURE)
      //  matchingResult.postValue(null)
        isFlashOn.postValue(false)
    }


}