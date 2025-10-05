package com.codepath.campgrounds

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.ImageView
import com.bumptech.glide.Glide

private const val TAG = "CampgroundAdapter"

class CampgroundAdapter(private val context: Context, private val campgrounds: MutableList<Campground>) : RecyclerView.Adapter<CampgroundAdapter.ViewHolder>() {

    // New list to hold the filtered campgrounds
    private val filteredCampgrounds = mutableListOf<Campground>()

    // Initialize the filtered list with all campgrounds
    init {
        filteredCampgrounds.addAll(campgrounds)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_campground, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // TODO: Get the individual campground and bind to holder
        val campground = filteredCampgrounds[position] // holds the filtered campground now
        holder.bind(campground)
    }

    // Returns the size of the filtered list
    override fun getItemCount() = filteredCampgrounds.size

    // Clean all elements of the recycler and both lists
    fun clear() {
        campgrounds.clear()
        filteredCampgrounds.clear()
        notifyDataSetChanged()
    }

    // Add a list of items to the regular list and the filtered list
    fun addAll(campgroundsList: List<Campground>) {
        campgrounds.addAll(campgroundsList)
        filteredCampgrounds.addAll(campgroundsList)
        notifyDataSetChanged()
    }

    // Filter that searches through the campgrounds list
    fun filter(query: String) {
        filteredCampgrounds.clear()
        if(query.isEmpty()) {
            // If the query is empty, show all campgrounds
            filteredCampgrounds.addAll(campgrounds)
        }
        else {
            // Filter the campgrounds on either, name, description, or location
            val filteredList = campgrounds.filter { campground ->
                campground.name?.contains(query, ignoreCase = true) == true ||
                        campground.description?.contains(query, ignoreCase = true) == true ||
                        campground.latLong?.contains(query, ignoreCase = true) == true
            }
            // Add the filtered campgrounds to the filtered list
            filteredCampgrounds.addAll(filteredList)
        }
        // Update the RecyclerView to show filtered results
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

            // TODO: Create member variables for any view that will be set
            private val nameTextView = itemView.findViewById<TextView>(R.id.campgroundName)
            private val descriptionTextView = itemView.findViewById<TextView>(R.id.campgroundDescription)
            private val locationTextView = itemView.findViewById<TextView>(R.id.campgroundLocation)
            private val imageView = itemView.findViewById<ImageView>(R.id.campgroundImage)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(campground: Campground) {
            // TODO: Set item views based on views and data model
            nameTextView.text = campground.name
            descriptionTextView.text = campground.description
            locationTextView.text = campground.latLong

            Glide.with(context).load(campground.imageUrl).into(imageView)

        }

        override fun onClick(v: View?) {
            // TODO: Get selected campground
            val campground = filteredCampgrounds[absoluteAdapterPosition] // uses the filtered list now

            // TODO: Navigate to Details screen and pass selected campground
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(CAMPGROUND_EXTRA, campground)
            context.startActivity(intent)
        }
    }
}
