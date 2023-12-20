package at.privatepilot.ui.navView

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import at.privatepilot.model.directoryItem.viewmodel.DirectoryItemViewModel
import at.privatepilot.restapi.service.NodeRepository
import at.privatepilot.R
import at.privatepilot.databinding.DirectoryNodeBinding

class NavAdapter : RecyclerView.Adapter<NavAdapter.DirViewHolder>() {

    private var itemList: List<DirectoryItemViewModel> = emptyList()
    private var selectedFolder: DirectoryItemViewModel? = null
    private var nodeRepository = NodeRepository.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DirectoryNodeBinding.inflate(inflater, parent, false)
        return DirViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: DirViewHolder, position: Int) {
        val currentItem = itemList[position]
        holder.bind(currentItem)

        // Set the item's selection state
        val isSelected = currentItem.path == selectedFolder?.path
        holder.itemView.isSelected = isSelected
        holder.itemView.setBackgroundColor(
            if (isSelected) {
                ContextCompat.getColor(holder.itemView.context, R.color.selectedItemBackground)
            } else {
                Color.TRANSPARENT
            }
        )

        // Setting padding
        val density = holder.itemView.context.resources.displayMetrics.density
        val paddingLeftDp = currentItem.depth * 30 * density.toInt()
        holder.itemView.setPadding(paddingLeftDp, 0, 0, 0)
    }

    fun updateList(newItemList: List<DirectoryItemViewModel>) {
        itemList = newItemList
        notifyDataSetChanged()
    }

    fun updateSelectedFolder(newSelectedFolder: DirectoryItemViewModel?) {
        selectedFolder = newSelectedFolder
    }

    inner class DirViewHolder(private val binding: DirectoryNodeBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }
        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val clickedFolder = itemList[position]
                nodeRepository.readNode(clickedFolder.path)
            }
        }

        fun bind(directoryItem: DirectoryItemViewModel) {
            binding.viewModel = directoryItem
            binding.executePendingBindings()
        }
    }
}
