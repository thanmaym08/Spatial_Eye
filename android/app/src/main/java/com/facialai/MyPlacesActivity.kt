package com.facialai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facialai.api.RetrofitClient
import com.facialai.databinding.ActivityMyPlacesBinding
import com.facialai.models.Place
import com.facialai.utils.SpeechHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyPlacesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPlacesBinding
    private lateinit var speechHelper: SpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechHelper = SpeechHelper(this)
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        
        loadPlaces()
    }

    private fun loadPlaces() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getPlaces()
                }
                if (response.isSuccessful) {
                    val places = response.body() ?: emptyList()
                    if (places.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        speechHelper.speak("You have no saved places.")
                    } else {
                        binding.recyclerView.adapter = PlacesAdapter(places)
                        speechHelper.speak("Loaded ${places.size} saved places.")
                    }
                } else {
                    Toast.makeText(this@MyPlacesActivity, "Failed to load places", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyPlacesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    inner class PlacesAdapter(private val places: List<Place>) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {
        inner class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPlaceName)
            val tvDetails: TextView = view.findViewById(R.id.tvDetails)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
            return PlaceViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
            val place = places[position]
            holder.tvName.text = place.placeName
            holder.tvDetails.text = holder.itemView.context.getString(
                R.string.place_details_format,
                place.createdAt.take(10),
                place.objectCount
            )
            
            holder.itemView.setOnClickListener {
                speechHelper.speak("Place: ${place.placeName}")
            }
            holder.itemView.setOnLongClickListener {
                deletePlace(place.id)
                true
            }
        }

        override fun getItemCount() = places.size
    }

    private fun deletePlace(placeId: String) {
        speechHelper.speak("Deleting place")
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.deletePlace(placeId)
                }
                if (response.isSuccessful) {
                    Toast.makeText(this@MyPlacesActivity, "Place deleted", Toast.LENGTH_SHORT).show()
                    loadPlaces()
                } else {
                    Toast.makeText(this@MyPlacesActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyPlacesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechHelper.shutdown()
    }
}
