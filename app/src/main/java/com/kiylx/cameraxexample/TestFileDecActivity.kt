package com.kiylx.cameraxexample

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.kiylx.camerax_lib.main.manager.imagedetection.filevision.MyFileProcessor
import com.kiylx.store_lib.StoreX

class TestFileDecActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_file_dec)
        findViewById<Button>(R.id.select).setOnClickListener {
            selectFileShow()
        }
    }


    fun selectFileShow() {
        StoreX.with(this).safHelper.selectFile(fileType = "image/*") { uri ->
            val imageView = findViewById<ImageView>(R.id.imageView)
            MyFileProcessor.process(contentResolver, uri) {
                imageView.setImageBitmap(it)
            }
        }
    }

    companion object {
        const val TAG = "MAINACTIVITY"
    }
}