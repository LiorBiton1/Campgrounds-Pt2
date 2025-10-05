package com.codepath.campgrounds

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codepath.campgrounds.databinding.ActivityMainBinding
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Headers
import org.json.JSONException

fun createJson() = Json {
    isLenient = true
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

private const val TAG = "CampgroundsMain/"
private val PARKS_API_KEY = BuildConfig.API_KEY
private val CAMPGROUNDS_URL =
    "https://developer.nps.gov/api/v1/campgrounds?api_key=${PARKS_API_KEY}"

class MainActivity : AppCompatActivity() {
    private lateinit var campgroundsRecyclerView: RecyclerView
    private lateinit var binding: ActivityMainBinding
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var offlineBanner: TextView

    // TODO: Create campgrounds list
    private val campgrounds = mutableListOf<Campground>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        campgroundsRecyclerView = findViewById(R.id.campgrounds)
        swipeContainer = findViewById(R.id.swipeContainer)

        offlineBanner = findViewById(R.id.offline_banner)
        networkMonitor = NetworkMonitor(this)

        // TODO: Set up CampgroundAdapter with campgrounds
        val campgroundAdapter = CampgroundAdapter(this, campgrounds)
        campgroundsRecyclerView.adapter = campgroundAdapter


        campgroundsRecyclerView.layoutManager = LinearLayoutManager(this).also {
            val dividerItemDecoration = DividerItemDecoration(this, it.orientation)
            campgroundsRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        swipeContainer.setOnRefreshListener {
            fetchCampgroundsAsync(campgroundAdapter)
        }

        swipeContainer.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Start monitoring network changes
        networkMonitor.startMonitoring { isConnected ->
            runOnUiThread {
                if(isConnected) {
                    // Hide the offline banner
                    offlineBanner.visibility = android.view.View.GONE
                    // Reload the data when connection returns
                    fetchCampgroundsAsync(campgroundAdapter)
                }
                else {
                    // Show the offline banner
                    offlineBanner.visibility = android.view.View.VISIBLE
                }
            }
        }

        // TODO: Step 7: Load new items from our database
        if(isCachingEnabled()) {
            lifecycleScope.launch {
                (application as CampgroundApplication).db.campgroundDao().getAll()
                    .collect { databaseList ->
                        databaseList.map { entity ->
                            Campground(
                                name = entity.name,
                                description = entity.description,
                                latLong = entity.latLong,
                                images = entity.imageUrl?.let { url ->
                                    listOf(CampgroundImage(url = url, title = null))
                                }
                            )
                        }.also { mappedList ->
                            campgroundAdapter.clear()
                            campgroundAdapter.addAll(mappedList)
                        }
                    }
            }
        }
        fetchCampgroundsAsync(campgroundAdapter)

        // Check initial network state
        if(!networkMonitor.isNetworkAvailable()) {
            offlineBanner.visibility = android.view.View.VISIBLE
        }
    }

    // SharedPreferences helpers
    private fun getAppPreferences(): SharedPreferences {
        return getSharedPreferences("CampgroundPrefs", Context.MODE_PRIVATE)
    }

    private fun isCachingEnabled(): Boolean {
        return getAppPreferences().getBoolean("enable_caching", true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            // When the user submits a query, filter the campgrounds list
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            // When the text changes, filter the campgrounds list
            override fun onQueryTextChange(newText: String?): Boolean {
                val campgroundAdapter = campgroundsRecyclerView.adapter as CampgroundAdapter
                campgroundAdapter.filter(newText ?: "")
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchCampgroundsAsync(campgroundAdapter: CampgroundAdapter) {
        val client = AsyncHttpClient()
        client.get(CAMPGROUNDS_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(statusCode: Int, headers: Headers?, response: String?, throwable: Throwable?) {
                Log.e(TAG, "Failed to fetch campgrounds: $statusCode")
                swipeContainer.setRefreshing(false)
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "Successfully fetched campgrounds: $json")
                try {
                    // TODO: Create the parsedJSON
                    val parsedJson = createJson().decodeFromString(
                        CampgroundResponse.serializer(),
                        json.jsonObject.toString()
                    )

                    // TODO: Do something with the returned json (contains campground information)
                    parsedJson.data?.let { list ->
                        if(isCachingEnabled()) {
                            lifecycleScope.launch(IO) {
                                (application as CampgroundApplication).db.campgroundDao().deleteAll()
                                (application as CampgroundApplication).db.campgroundDao().insertAll(list.map {
                                        CampgroundEntity(
                                            name = it.name,
                                            description = it.description,
                                            latLong = it.latLong,
                                            imageUrl = it.imageUrl
                                        )
                                })
                            }
                        }
                        // Manually update the adapter if caching is disabled
                        else {
                            campgroundAdapter.clear()
                            campgroundAdapter.addAll(list)
                        }
                    }

                    // TODO: Save the campgrounds and reload the screen
                    swipeContainer.setRefreshing(false)

                } catch (e: JSONException) {
                    Log.e(TAG, "Exception: $e")
                    swipeContainer.setRefreshing(false)
                }
            }
        })
    }

    // Clean up the monitor to prevent memory leaks
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
}
