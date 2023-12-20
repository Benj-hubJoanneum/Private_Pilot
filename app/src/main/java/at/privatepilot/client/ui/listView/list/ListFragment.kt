package at.privatepilot.client.ui.listView.list

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.privatepilot.MainActivity
import at.privatepilot.R
import at.privatepilot.client.ui.listView.base.BaseAdapter
import at.privatepilot.client.ui.listView.base.BaseFragment

class ListFragment : BaseFragment() {
    override fun createAdapter(): BaseAdapter {
        return ListAdapter(emptyList(), requireActivity() as MainActivity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewModel.setValues("List View", R.drawable.ic_list)
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireContext())
    }
}
