package com.app.firebasedemo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.app.firebasedemo.R
import com.app.firebasedemo.models.Notes
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

/**
 * notes adapter to show notes on list view
 * @author Aymen Masmoudi[08.11.2021]
 * */
class NotesAdapter(private val context: Context) : BaseAdapter(), Filterable {

    private var notesList: ArrayList<Notes>
    private var defNotesList: ArrayList<Notes>
    private val storageReference: StorageReference = Firebase.storage.reference

    init {
        notesList = ArrayList()
        defNotesList = ArrayList()
    }

    //set notes arrayList, and refresh list
    fun setItems(notes: List<Notes>) {
        defNotesList = ArrayList(notes)
        notesList = defNotesList
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return notesList.size
    }

    override fun getItem(position: Int): Notes {
        return notesList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder = Holder()
        if (view == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_notes, parent, false)
            holder.tvTitle = view.findViewById(R.id.tvTitle)
            holder.tvText = view.findViewById(R.id.tvText)
            holder.imgPicture = view.findViewById(R.id.imgPicture)
            view!!.tag = holder
        } else {
            holder = view.tag as Holder
        }

        val note = notesList[position]

        holder.tvTitle!!.text = note.title
        holder.tvText!!.text = note.text

        val imageName = note.image
        if (imageName.isNotEmpty()) {
            holder.imgPicture!!.visibility = View.VISIBLE
            storageReference.child(imageName).downloadUrl.addOnSuccessListener {
                Glide.with(context)
                    .load(it)
                    .into(holder.imgPicture!!)
            }
        } else {
            holder.imgPicture!!.visibility = View.GONE
        }

        return view
    }

    private class Holder {
        var tvTitle: TextView? = null
        var tvText: TextView? = null
        var imgPicture: ImageView? = null
    }

    //filter notes
    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val result = ArrayList<Notes>()
                if (constraint!!.isNotEmpty()){
                    for (note in defNotesList){
                        if (note.title.lowercase().contains(constraint.toString().lowercase())){
                            result.add(note)
                        }
                    }
                } else {
                    result.addAll(defNotesList)
                }
                val filterResult = FilterResults()
                filterResult.values = result
                filterResult.count = result.size
                return filterResult
            }
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notesList = results!!.values as ArrayList<Notes>
                notifyDataSetChanged()
            }

        }
    }
}