package at.privatepilot

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.privatepilot.databinding.ActivityMainBinding
import at.privatepilot.client.restapi.client.CredentialManager
import at.privatepilot.client.restapi.client.NetworkRepository
import at.privatepilot.client.restapi.service.NodeRepository
import at.privatepilot.client.ui.login.LoginActivity
import at.privatepilot.client.ui.navView.NavAdapter
import at.privatepilot.client.ui.navView.NavViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView

class MainActivity : AppCompatActivity(), NodeRepository.ConnectionCallback, NodeRepository.LoadingCallback  {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var actionMode: ActionMode? = null
    private val nodeRepository = NodeRepository.getInstance()
    private val currentDestination: NavDestination? = null

    private val openFileLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                nodeRepository.selectedFileUri = result.data?.data
                val file = nodeRepository.getThisFile(nodeRepository.selectedFileUri, this)
                nodeRepository.createNode(file)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nodeRepository.setWebsocketCallback(this)
        nodeRepository.setLoadingCallback(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val speedDialView: SpeedDialView = binding.appBarMain.fab
        speedDialView.setMainFabClosedDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_upload, null))
        speedDialView.setMainFabOpenedDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_close, null))

        // Configure SpeedDialView
        speedDialView.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_action1, R.drawable.ic_upload_file)
                .setLabel("upload File")
                .create()
        )

        speedDialView.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_action2, R.drawable.ic_folder_upload)
                .setLabel("create Folder")
                .create()
        )

        speedDialView.setOnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.fab_action1 -> {
                    nodeRepository.launchFileSelection(openFileLauncher)
                }
                R.id.fab_action2 -> {
                    showUploadDialog()
                }
            }
            false
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val drawerRecyclerView = binding.navView.findViewById<RecyclerView>(R.id.drawer_recyclerview)
        drawerRecyclerView.layoutManager = LinearLayoutManager(this)
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_listview, R.id.nav_gridview
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val currentDestination = navController.currentDestination

        val navViewModel = ViewModelProvider(this)[NavViewModel::class.java]
        val navAdapter = NavAdapter()
        drawerRecyclerView.adapter = navAdapter

        nodeRepository.displayedList.observe(this) {
            hideLoadingOverlay()
        }

        nodeRepository.directoryList.observe(this) { navViewModel.loadFolderList(it) }
        navViewModel.itemList.observe(this) { navAdapter.updateList(it) }

        nodeRepository.directoryPointer.observe(this) { navViewModel.setSelectedFolder(it) }
        navViewModel.selectedFolder.observe(this) {
            navAdapter.updateSelectedFolder(it)

            var title = it?.name ?: ""
            if (title.isBlank())
                title = "HOME"

            supportActionBar?.title = title
        }

        if (!CredentialManager.deviceauth) {
            redirectToLogin()
            return
        }

        viewModel.lan.observe(this) { lan ->
            findViewById<TextView>(R.id.lan).text = "Server LAN IP: ${lan}"
        }

        viewModel.wan.observe(this) { wan ->
            findViewById<TextView>(R.id.wan).text = "Server WAN IP: ${wan}"
        }

        viewModel.updateCredentials(CredentialManager.getStoredServerLANAddress(this), CredentialManager.getStoredServerWANAddress(this))

        nodeRepository.readNode("")

        handleIntent(intent)
        handleSendIntent(intent)

    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                onSearchQuery(query)
            }
        }
    }

    private fun handleSendIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action) {
            val type = intent.type
            if (type != null) {
                when {
                    type.startsWith("text/") -> handleSharedText(intent)
                    else -> handleSharedFile(intent) // Handle any other file type
                }
            }
        }
    }

    private fun handleSharedText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        // safekeeping for later
        showToast("Received shared text: $sharedText")
    }

    private fun handleSharedFile(intent: Intent) {
        val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (fileUri != null) {
            nodeRepository.createNode(fileUri, this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        // Set OnMenuItemClickListener for the search item
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                nodeRepository.readNode()
                return true
            }
        })

        // Set OnQueryTextListener for handling search queries
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    onSearchQuery(query)
                    searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        searchView.setOnCloseListener {
            nodeRepository.undoSearch("")
            true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onSearchQuery(query: String) {
        nodeRepository.onSearchQuery(query)
    }

    private fun showUploadDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.upload_dialog, null)

        val editText = view.findViewById<EditText>(R.id.editText)
        builder.setView(view)
            .setPositiveButton("Upload") { dialog, which ->
                val folderName = editText.text.toString()
                nodeRepository.createNode(folderName)
            }
            .setNegativeButton("Cancel") { dialog, which ->
            }
            .show()
    }

    private fun showSettingsDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_ip_settings, null)

        val ip = view.findViewById<EditText>(R.id.ip_field)

        builder.setView(view)
            .setPositiveButton("OK") { dialog, which ->
                val newIP = "${ip.text}"
                NetworkRepository.setWANIP(ip.text.toString(), this)
                showToast("new IP: $newIP")
                viewModel.updateCredentials(CredentialManager.getStoredServerLANAddress(this), newIP)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Handle cancel action if needed
            }
            .show()
    }

    private fun showToast(message: String?) {
        if (message != null) {
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private lateinit var loadingOverlay: View
    private var isOverlayVisible = false

    override fun showLoadingOverlay() {
        runOnUiThread {
            if (!isOverlayVisible) {
                val overlayInflater = LayoutInflater.from(this)
                loadingOverlay = overlayInflater.inflate(R.layout.loading_screen, null)
                val rootView = findViewById<ViewGroup>(android.R.id.content)
                rootView.addView(loadingOverlay)

                /*
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
                */

                isOverlayVisible = true
            }
        }
    }

    fun hideLoadingOverlay() {
        runOnUiThread {
            if (isOverlayVisible) {
                val rootView = findViewById<ViewGroup>(android.R.id.content)
                rootView.removeView(loadingOverlay)

                // Re-enable user interactions
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                isOverlayVisible = false
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        actionMode = mode
        supportActionBar?.hide()
    }

    override fun onActionModeFinished(mode: ActionMode) {
        super.onActionModeFinished(mode)
        actionMode = null
        supportActionBar?.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onConnection() {
        showToast("Connected to Server")
        hideLoadingOverlay()
    }

    override fun onConnectionCancel() {
        showToast("Connection Closed.")
    }

    override fun onConnectionFailure() {
        showToast("Connection failed. Please check your network.")
    }
}
