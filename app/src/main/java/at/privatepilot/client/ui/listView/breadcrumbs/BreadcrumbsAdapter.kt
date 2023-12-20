package at.privatepilot.ui.listView.breadcrumbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.privatepilot.R
import at.privatepilot.databinding.BreadcrumbItemBinding
import at.privatepilot.model.directoryItem.viewmodel.DirectoryBreadcrumbViewModel
import at.privatepilot.restapi.service.NodeRepository

class BreadcrumbsAdapter : RecyclerView.Adapter<BreadcrumbsAdapter.HorizontalViewHolder>() {

    private var itemList: List<DirectoryBreadcrumbViewModel> = listOf()
    private var nodeRepository = NodeRepository.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = BreadcrumbItemBinding.inflate(inflater, parent, false)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.breadcrumb_item, parent, false)
        return HorizontalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HorizontalViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            nodeRepository.readNode(item.path)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun updateList(newItemList: List<DirectoryBreadcrumbViewModel>) {
        itemList = newItemList
        notifyDataSetChanged()
    }

    inner class HorizontalViewHolder(private val binding: BreadcrumbItemBinding) :
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

        fun bind(directoryItem: DirectoryBreadcrumbViewModel) {
            binding.viewModel = directoryItem
            binding.executePendingBindings()
        }
    }
}
