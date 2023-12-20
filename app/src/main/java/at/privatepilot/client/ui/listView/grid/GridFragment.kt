package at.privatepilot.ui.listView.grid

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.privatepilot.R
import at.privatepilot.ui.listView.base.BaseAdapter
import at.privatepilot.ui.listView.base.BaseFragment

class GridFragment : BaseFragment() {

    override fun createAdapter(): BaseAdapter {
        return GridAdapter(emptyList(), requireActivity() as at.privatepilot.MainActivity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewModel.setValues("Grid View", R.drawable.ic_grid)
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return GridLayoutManager(requireContext(), 2)
    }
}
