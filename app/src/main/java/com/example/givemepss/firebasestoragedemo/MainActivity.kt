package com.example.givemepss.firebasestoragedemo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    private var mStorageRef: StorageReference? = null
    private var imgPath: String = ""
    private var riversRef: StorageReference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initData()
        initView()
    }

    private fun initData() {
        mStorageRef = FirebaseStorage.getInstance().reference
    }

    private fun checkPermission() {
        val permission = ActivityCompat.checkSelfPermission(this@MainActivity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //未取得權限，向使用者要求允許權限
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_EXTERNAL_STORAGE
            )
        } else {
            getLocalImg()
        }

    }

    private fun initView() {
        pick_button.setOnClickListener { checkPermission() }
        upload_button.setOnClickListener {
            if (imgPath.isNotEmpty()) {
                upload_progress.visibility = View.VISIBLE
                uploadImg(imgPath)
            } else {
                Toast.makeText(this@MainActivity, R.string.plz_pick_img, Toast.LENGTH_SHORT).show()
            }
        }
        download_button.setOnClickListener { downloadImg(riversRef) }
        delete_button.setOnClickListener { deleteImg(riversRef) }
    }

    private fun deleteImg(ref: StorageReference?) {
        if (ref == null) {
            Toast.makeText(this@MainActivity, R.string.plz_upload_img, Toast.LENGTH_SHORT).show()
            return
        }
        ref.delete().addOnSuccessListener { Toast.makeText(this@MainActivity, R.string.delete_success, Toast.LENGTH_SHORT).show() }.addOnFailureListener { exception -> Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_SHORT).show() }
    }

    private fun downloadImg(ref: StorageReference?) {
        if (ref == null) {
            Toast.makeText(this@MainActivity, R.string.plz_upload_img, Toast.LENGTH_SHORT).show()
            return
        }
        ref.downloadUrl.addOnSuccessListener {
            Glide.with(this@MainActivity)
                    .load(imgPath)
                    .into(download_img)
            download_info_text.setText(R.string.download_success)
        }.addOnFailureListener { exception -> download_info_text.text = exception.message }
    }

    private fun uploadImg(path: String) {
        val file = Uri.fromFile(File(path))
        val metadata = StorageMetadata.Builder()
                .setContentDisposition("universe")
                .setContentType("image/jpg")
                .build()
        riversRef = mStorageRef?.child(file.lastPathSegment ?: "")
        val uploadTask = riversRef?.putFile(file, metadata)
        uploadTask?.addOnFailureListener { exception ->
            upload_info_text.text = exception.message
        }?.addOnSuccessListener {
            upload_info_text.setText(R.string.upload_success)
        }?.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            upload_progress.progress = progress
            if (progress >= 100) {
                upload_progress.visibility = View.GONE
            }
        }
    }

    private fun getLocalImg() {
        ImagePicker.with(this)
                .crop()                    //Crop image(Optional), Check Customization for more option
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                .start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocalImg()
                } else {
                    Toast.makeText(this@MainActivity, R.string.do_nothing, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                val filePath: String = ImagePicker.getFilePath(data) ?: ""
                if (filePath.isNotEmpty()) {
                    imgPath = filePath
                    Toast.makeText(this@MainActivity, imgPath, Toast.LENGTH_SHORT).show()
                    Glide.with(this@MainActivity).load(filePath).into(pick_img)
                } else {
                    Toast.makeText(this@MainActivity, R.string.load_img_fail, Toast.LENGTH_SHORT).show()
                }
            }
            ImagePicker.RESULT_ERROR -> Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 200
    }

}
