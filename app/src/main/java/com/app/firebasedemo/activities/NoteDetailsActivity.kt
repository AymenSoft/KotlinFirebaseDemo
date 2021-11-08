package com.app.firebasedemo.activities

import android.Manifest.permission.*
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import com.app.firebasedemo.databinding.ActivityNoteDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import android.os.Environment
import java.text.SimpleDateFormat
import java.util.*
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.app.firebasedemo.FirebaseDemoApplication
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.*
import java.io.File.separator
import com.app.firebasedemo.models.Notes

/**
 * add, update or delete notes
 * @param action to add or update notes
 * @param noteId to get note from database
 * @author Aymen Masmoudi[08.11.2021]
 * */
@SuppressLint("SetTextI18n")
class NoteDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailsBinding

    private lateinit var intentAction: String

    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser!!.uid

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val storageReference: StorageReference = Firebase.storage.reference

    private lateinit var noteId: String
    private var imageURL: String = ""
    private var imagePath: String = ""
    private lateinit var imageBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            if (binding.etTitle.text.isNotEmpty() && binding.etText.text.isNotEmpty()) {
                binding.btnSave.isEnabled = false
                binding.tvMessage.text = "please wait"
                binding.tvMessage.visibility = View.VISIBLE
                //if user select image, upload it first
                if (imagePath.isNotEmpty()) {
                    uploadImage()
                } else {
                    //if no images selected, save note
                    saveNote()
                }
                //if user offline, finish activity and wait internet to sync
                if (!FirebaseDemoApplication().isUserConnected){
                    finish()
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            //show alert before deleting notes
            val alert = AlertDialog.Builder(this)
            alert.setTitle("Notes")
            alert.setMessage("you want to delete this note?")
            alert.setNegativeButton("no") { _, _ ->
                alert.setCancelable(true)
            }
            alert.setPositiveButton("yes") { _, _ ->
                //delete image from storage first
                if (imageURL.isNotEmpty()) {
                    deleteImage()
                } else {
                    //delete note if no images found with note
                    deleteNote()
                }
            }.show()
        }

        //check permissions
        if (!requestPermission()) {
            binding.imgPicture.isEnabled = false
        }

        /*
        * if action = add -> generate new note id to be saved in database
        * if action = update -> get noteId from intent and import note from database
        * */
        intentAction = intent.action!!
        when (intentAction) {
            "add" -> {
                noteId = System.currentTimeMillis().toString()
                binding.btnDelete.visibility = View.GONE
            }
            "update" -> {
                noteId = intent.getStringExtra("noteId")!!
                Log.e("noteId", noteId)
                Log.e("userId", userId)
                showNote()
            }
        }

    }

    //show popup menu to select picture from gallery or start camera
    fun imageMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, "Gallery")
        popup.menu.add(0, 1, 0, "Camera")
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                0 -> {
                    val galleryIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    galleryForResult.launch(galleryIntent)
                }
                1 -> {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraForResult.launch(cameraIntent)
                }
            }
            false
        }
        popup.show()
    }

    //show note
    private fun showNote() {
        databaseReference.child("notes")
            .child(userId).child(noteId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    Log.e("error", error.message)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.etTitle.setText(snapshot.child("title").value.toString())
                    binding.etText.setText(snapshot.child("text").value.toString())
                    imageURL = snapshot.child("image").value.toString()
                    if (imageURL.isNotEmpty()) {
                        storageReference.child(imageURL).downloadUrl.addOnSuccessListener {
                            Glide.with(this@NoteDetailsActivity)
                                .load(it)
                                .into(binding.imgPicture)
                        }
                    }
                }
            })
    }

    //save note on realtime database
    private fun saveNote() {
        //create note object
        val note = Notes(
            noteId,
            binding.etTitle.text.toString(),
            binding.etText.text.toString(),
            imageURL
        )
        //save note object
        databaseReference.child("notes").child(userId).child(noteId)
            .setValue(note)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener {
                binding.btnSave.isEnabled = true
                binding.tvMessage.text = "note not saved"
                Toast.makeText(
                    this@NoteDetailsActivity,
                    it.message.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    //upload image
    private fun uploadImage() {
        val file = Uri.fromFile(File(imagePath))
        val fileName = file.lastPathSegment
        imageURL = "/$userId/$noteId/$fileName"
        Log.e("name", imageURL)
        val riversRef = storageReference.child(imageURL)
        val uploadTask = riversRef.putFile(file)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            riversRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveNote()
            } else {
                binding.btnSave.isEnabled = true
                binding.tvMessage.text = "image not uploaded"
            }
        }
    }

    //delete image
    private fun deleteImage() {
        storageReference.child(imageURL).delete().addOnSuccessListener {
                deleteNote()
            }
            .addOnFailureListener {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = "can't delete picture"
            }
    }

    //delete note
    private fun deleteNote() {
        databaseReference.child("notes")
            .child(userId).child(noteId).removeValue().addOnSuccessListener {
                finish()
            }
            .addOnFailureListener {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = "can't delete note"
            }
    }

    /*
    * get storage and camera permissions
    * if not granted, then use request permissions
    * */
    private fun requestPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, CAMERA),
                2
            )
            return false
        }
        return true
    }

    //get request permissions result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
            && grantResults[2] == PackageManager.PERMISSION_GRANTED
        ) {
            binding.imgPicture.isEnabled = true
        }
    }

    //get gallery activity result to show user selected picture from gallery
    private val galleryForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val selectedImage = result.data!!.data
            var filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                filePathColumn = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
            }
            val cursor = contentResolver.query(
                selectedImage!!,
                filePathColumn,
                null,
                null,
                null,
                null
            )
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            imagePath = cursor.getString(columnIndex)
            cursor.close()
            imageBitmap = BitmapFactory.decodeFile(imagePath)
            binding.imgPicture.setImageBitmap(imageBitmap)
        }
    }

    //get camera activity result to show image captured by camera
    private val cameraForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == RESULT_OK && result.data != null){
            imageBitmap = result.data!!.extras!!.get("data") as Bitmap
            imageBitmap.saveToGallery()
            //imageBitmap = data.extras!!.get("data") as Bitmap
            binding.imgPicture.setImageBitmap(imageBitmap)
        }
    }

    //create file to save image from camera to gallery
    @Throws(IOException::class)
    private fun createImagineFile(): File {
        var path: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + separator.toString() + "FirebaseDemo"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + separator.toString() + "FirebaseDemo"
        }
        val outputDir = File(path)
        outputDir.mkdir()
        val timeStamp: String = SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(Date())
        val fileName = "FirebaseDemo_$timeStamp.jpg"
        val image = File(path + separator.toString() + fileName)
        imagePath = image.absolutePath
        Log.e("imagePath", imagePath)
        Log.e("imageAbsolute", image.absolutePath.toString())
        return image
    }

    //save bitmap to gallery
    private fun Bitmap.saveToGallery(): Uri? {
        val file = createImagineFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, file.absolutePath)
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)

            val uri: Uri? =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(this, contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
                return uri
            }
        } else {
            saveImageToStream(this, FileOutputStream(file))
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            return Uri.fromFile(file)
        }

        return null
    }

    //save image to stream
    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}