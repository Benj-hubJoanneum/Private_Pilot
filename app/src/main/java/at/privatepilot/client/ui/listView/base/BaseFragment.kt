package at.privatepilot.ui.listView.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.privatepilot.R
import at.privatepilot.databinding.FragmentListviewBinding
import at.privatepilot.restapi.service.NodeRepository
import at.privatepilot.ui.listView.breadcrumbs.BreadcrumbViewModel
import at.privatepilot.ui.listView.breadcrumbs.BreadcrumbsAdapter

abstract class BaseFragment : Fragment() {
    private var _binding: FragmentListviewBinding? = null
    val binding get() = _binding!!
    private lateinit var adapter : BaseAdapter
    lateinit var recyclerViewModel: RecyclerViewModel
    private val nodeRepository = NodeRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListviewBinding.inflate(inflater, container, false)
        val root: View = binding.root
        recyclerViewModel = ViewModelProvider(this)[RecyclerViewModel::class.java]
        val breadcrumbViewModel = ViewModelProvider(this)[BreadcrumbViewModel::class.java]

        // Initialize fileAdapter
        adapter = createAdapter()

        // Setup RecyclerView
        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = createLayoutManager()
        recyclerView.adapter = adapter

        val horizontalRecyclerView: RecyclerView = binding.horizontalView

        val breadcrumbsAdapter = BreadcrumbsAdapter()
        horizontalRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        horizontalRecyclerView.adapter = breadcrumbsAdapter

        breadcrumbViewModel.itemList.observe(viewLifecycleOwner) { breadcrumbsAdapter.updateList(it) }
        nodeRepository.directoryPointer.observe(viewLifecycleOwner) {
            breadcrumbViewModel.loadList(it)
        }

        val textView: TextView = binding.listDescription
        recyclerViewModel.text.observe(viewLifecycleOwner) { text ->
            textView.text = text
        }

        val imageView: ImageView = binding.listDescriptionImage
        recyclerViewModel.imageResource.observe(viewLifecycleOwner) { resource ->
            imageView.setImageResource(resource)
        }

        val hitBox = binding.listDescriptionBox
        hitBox.setOnClickListener {
            adapter.hitBoxHitted = true
            // Check the current fragment and navigate to the other fragment
            val currentFragmentId = requireActivity().findNavController(R.id.nav_host_fragment_content_main).currentDestination?.id
            val newFragmentId = if (currentFragmentId == R.id.nav_listview) {
                R.id.nav_gridview // Switch to the GridFragment
            } else {
                R.id.nav_listview // Switch to the ListFragment
            }
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(newFragmentId)
        }

        nodeRepository.displayedList.observe(viewLifecycleOwner) {
            recyclerViewModel.loadFileList(it)
        }

        recyclerViewModel.itemList.observe(viewLifecycleOwner) { newItemList ->
            adapter.updateList(newItemList)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    abstract fun createAdapter(): BaseAdapter
    abstract fun createLayoutManager(): RecyclerView.LayoutManager

}
