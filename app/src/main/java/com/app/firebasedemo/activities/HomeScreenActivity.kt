package com.app.firebasedemo.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.app.firebasedemo.adapters.NotesAdapter
import com.app.firebasedemo.databinding.ActivityHomeScreenBinding
import com.app.firebasedemo.messaging.Token
import com.app.firebasedemo.models.Notes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * show notes on a list view
 * use floating button to add notes
 * click on a note to update or delete it
 * use filtering to search in notes titles
 * @author Aymen Masmoudi[08.11.2021]
 * */
@SuppressLint("SetTextI18n")
class HomeScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeScreenBinding

    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser!!.uid

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    private lateinit var notesAdapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notesAdapter = NotesAdapter(this@HomeScreenActivity)
        binding.lvData.adapter = notesAdapter
        binding.lvData.setOnItemClickListener { _, _, position, _ ->
            //send "update" action and note id to NoteDetailsActivity
            val intent = Intent(this@HomeScreenActivity, NoteDetailsActivity::class.java)
            intent.action = "update"
            intent.putExtra("noteId", notesAdapter.getItem(position).id)
            startActivity(intent)
        }

        binding.fabAdd.setOnClickListener {
            //send "add" action to NoteDetailsActivity
            val intent = Intent(this@HomeScreenActivity, NoteDetailsActivity::class.java)
            intent.action = "add"
            startActivity(intent)
        }

        //filter notes list
        binding.etSearch.addTextChangedListener {
            try {
                notesAdapter.filter.filter(it)
            }catch (error: Throwable){
                Log.e("searchError", error.message.toString())
            }
        }

        //show notes
        showNotes()

        //get firebase messaging token
        Token().getToken()

    }

    //show notes
    private fun showNotes() {
        databaseReference.child("notes").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val arrayList = ArrayList<Notes>()
                    snapshot.children.forEach {
                        val note = Notes(
                            it.child("id").value.toString(),
                            it.child("title").value.toString(),
                            it.child("text").value.toString(),
                            it.child("image").value.toString(),
                        )
                        arrayList.add(note)
                    }
                    notesAdapter.setItems(arrayList)
                    if (arrayList.isEmpty()) {
                        binding.tvLoading.visibility = View.VISIBLE
                        binding.tvLoading.text = "no data found"
                    } else {
                        binding.tvLoading.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

}