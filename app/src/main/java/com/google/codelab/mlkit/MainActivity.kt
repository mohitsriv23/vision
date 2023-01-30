// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.codelab.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.google.codelab.mlkit.GraphicOverlay.Graphic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import java.io.IOException
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity(), OnItemSelectedListener {
    private var mImageView: ImageView? = null
    private var mTextButton: Button? = null
    private var mFaceButton: Button? = null
    private var mSelectedImage: Bitmap? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null
    private val sortedLabels = PriorityQueue<Map.Entry<String, Float>>(
        RESULTS_TO_SHOW
    ) { (_, value), (_, value1) -> value.compareTo(value1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mImageView = findViewById(R.id.image_view)
        mTextButton = findViewById(R.id.button_text)
        mFaceButton = findViewById(R.id.button_face)
        mGraphicOverlay = findViewById(R.id.graphic_overlay)
        mTextButton.setOnClickListener(View.OnClickListener { runTextRecognition() })
        mFaceButton.setOnClickListener(View.OnClickListener { runFaceContourDetection() })
        val dropdown = findViewById<Spinner>(R.id.spinner)
        val items = arrayOf("Test Image 1 (Text)", "Test Image 2 (Face)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter
        dropdown.onItemSelectedListener = this
    }

    private fun runTextRecognition() {
        val image = InputImage.fromBitmap(mSelectedImage!!, 0)
        val recognizer = TextRecognition.getClient()
        mTextButton!!.isEnabled = false
        recognizer.process(image)
            .addOnSuccessListener { texts ->
                mTextButton!!.isEnabled = true
                processTextRecognitionResult(texts)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                mTextButton!!.isEnabled = true
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
            showToast("No text found")
            return
        }
        mGraphicOverlay!!.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic: Graphic = TextGraphic(mGraphicOverlay, elements[k])
                    mGraphicOverlay!!.add(textGraphic)
                }
            }
        }
    }

    private fun runFaceContourDetection() {
        val image = InputImage.fromBitmap(mSelectedImage!!, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        mFaceButton!!.isEnabled = false
        val detector = FaceDetection.getClient(options)
        detector.process(image)
            .addOnSuccessListener { faces ->
                mFaceButton!!.isEnabled = true
                processFaceContourDetectionResult(faces)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                mFaceButton!!.isEnabled = true
                e.printStackTrace()
            }
    }

    private fun processFaceContourDetectionResult(faces: List<Face>) {
        // Task completed successfully
        if (faces.size == 0) {
            showToast("No face found")
            return
        }
        mGraphicOverlay!!.clear()
        for (i in faces.indices) {
            val face = faces[i]
            val faceGraphic = FaceContourGraphic(mGraphicOverlay)
            mGraphicOverlay!!.add(faceGraphic)
            faceGraphic.updateFace(face)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }// Calculate the max width in portrait mode. This is done lazily since we need to

    // wait for
    // a UI layout pass to get the right values. So delay it to first time image
    // rendering time.
    // Functions for loading images from app assets.
    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private val imageMaxWidth: Int
        private get() {
            if (mImageMaxWidth == null) {
                // Calculate the max width in portrait mode. This is done lazily since we need to
                // wait for
                // a UI layout pass to get the right values. So delay it to first time image
                // rendering time.
                mImageMaxWidth = mImageView!!.width
            }
            return mImageMaxWidth!!
        }// Calculate the max width in portrait mode. This is done lazily since we need to

    // wait for
    // a UI layout pass to get the right values. So delay it to first time image
    // rendering time.
    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private val imageMaxHeight: Int
        private get() {
            if (mImageMaxHeight == null) {
                // Calculate the max width in portrait mode. This is done lazily since we need to
                // wait for
                // a UI layout pass to get the right values. So delay it to first time image
                // rendering time.
                mImageMaxHeight = mImageView!!.height
            }
            return mImageMaxHeight!!
        }

    // Gets the targeted width / height.
    private val targetedWidthHeight: Pair<Int, Int>
        private get() {
            val targetWidth: Int
            val targetHeight: Int
            val maxWidthForPortraitMode = imageMaxWidth
            val maxHeightForPortraitMode = imageMaxHeight
            targetWidth = maxWidthForPortraitMode
            targetHeight = maxHeightForPortraitMode
            return Pair(targetWidth, targetHeight)
        }

    override fun onItemSelected(parent: AdapterView<*>?, v: View, position: Int, id: Long) {
        mGraphicOverlay!!.clear()
        when (position) {
            0 -> mSelectedImage = getBitmapFromAsset(this, "Please_walk_on_the_grass.jpg")
            1 ->                 // Whatever you want to happen when the thrid item gets selected
                mSelectedImage = getBitmapFromAsset(this, "grace_hopper.jpg")
        }
        if (mSelectedImage != null) {
            // Get the dimensions of the View
            val targetedSize = targetedWidthHeight
            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor = Math.max(
                mSelectedImage!!.width.toFloat() / targetWidth.toFloat(),
                mSelectedImage!!.height.toFloat() / maxHeight.toFloat()
            )
            val resizedBitmap = Bitmap.createScaledBitmap(
                mSelectedImage,
                (mSelectedImage!!.width / scaleFactor).toInt(),
                (mSelectedImage!!.height / scaleFactor).toInt(),
                true
            )
            mImageView!!.setImageBitmap(resizedBitmap)
            mSelectedImage = resizedBitmap
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing
    }

    companion object {
        private const val TAG = "MainActivity"

        /**
         * Number of results to show in the UI.
         */
        private const val RESULTS_TO_SHOW = 3

        /**
         * Dimensions of inputs.
         */
        private const val DIM_IMG_SIZE_X = 224
        private const val DIM_IMG_SIZE_Y = 224
        fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
            val assetManager = context.assets
            val `is`: InputStream
            var bitmap: Bitmap? = null
            try {
                `is` = assetManager.open(filePath)
                bitmap = BitmapFactory.decodeStream(`is`)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return bitmap
        }
    }
}