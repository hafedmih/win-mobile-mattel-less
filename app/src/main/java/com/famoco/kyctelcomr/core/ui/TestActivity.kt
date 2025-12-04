package com.famoco.kyctelcomr.core.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.famoco.kyctelcomr.R
import com.famoco.kyctelcomr.R.id.imageView
import com.famoco.kyctelcomr.face.FaceViewModel
import com.famoco.kyctelcomr.face.services.facenet.FaceNetService
import com.famoco.kyctelcomr.face.services.mtcnn.MTCNN
import java.io.ByteArrayOutputStream

class TestActivity : AppCompatActivity() {

    private lateinit var viewModel: FaceViewModel
    private lateinit var imageView: ImageView
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var capturedImage: Bitmap
    private lateinit var button: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // enableEdgeToEdge()
        setContentView(R.layout.activity_test)

         button = findViewById(R.id.btnValider)
        imageView = findViewById(R.id.imageView2) // Assuming there's an ImageView with this id in your layout

        val faceNetService = FaceNetService(context = application)
        val mtcnn = MTCNN(application)
        mtcnn.load()
        faceNetService.load()

        viewModel = FaceViewModel(faceNetService, mtcnn)

        // Open the camera when the button is clicked
        button.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    // Capture an image using the camera
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle the captured image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data?.extras
            capturedImage = extras?.get("data") as Bitmap

            // Display the captured image in ImageView
            imageView.setImageBitmap(capturedImage)

            // Convert the image to a byte array and pass it to the ViewModel
            val image1Bytes = bitmapToByteArray(capturedImage)

            // Assuming there's a second image in drawable resources
            val image2Bitmap: Bitmap = (resources.getDrawable(R.drawable.image2, null) as BitmapDrawable).bitmap

            // Pass the images to the ViewModel
            //viewModel.match2(image1Bytes, image2Bitmap)
            viewModel.match2(image1Bytes, image2Bitmap) { score ->
                // Update the UI with the score
              //  scoreTextView.text = "Similarity Score: $score"
                if (score >=0.8){
                    Toast.makeText(this, "Auth true", Toast.LENGTH_SHORT).show()
                   button.text="Auth true "+score

                }   else {
                    Toast.makeText(this, "Auth false", Toast.LENGTH_SHORT).show()
                    button.text="Auth false "
                }
            }
        }
    }

    // Convert Bitmap to ByteArray
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
