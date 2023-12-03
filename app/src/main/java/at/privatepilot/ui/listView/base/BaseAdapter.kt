package at.privatepilot.ui.listView.base

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import at.privatepilot.R
import at.privatepilot.model.FileType
import at.privatepilot.model.nodeItem.viewmodel.NodeItemViewModel
import at.privatepilot.restapi.service.NodeRepository
import at.privatepilot.ui.dialog.NodeDialogFragment
import com.google.android.material.textfield.TextInputEditText

abstract class BaseAdapter(
    protected var itemList: List<NodeItemViewModel>,
    protected val mainActivity: at.privatepilot.MainActivity
) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder>(), ActionMode.Callback  {

    protected val nodeRepository = NodeRepository.getInstance()
    private var selectedItems = nodeRepository.selectedItems
    protected var actionMode: ActionMode? = null
    private var cutItems = nodeRepository.cutItems
    var hitBoxHitted = false

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val currentItem = itemList[position]
        holder.bind(currentItem)

        if (cutItems.size == 0) {
            // Set the item's selection state
            val isSelected = selectedItems.contains(position)
            holder.itemView.isSelected = isSelected
            holder.itemView.setBackgroundColor(
                if (isSelected) {
                    ContextCompat.getColor(holder.itemView.context, R.color.selectedItemBackground)
                } else {
                    holder.setTextColor(1F)
                    Color.TRANSPARENT
                }
            )
        } else {
            if (cutItems.contains(currentItem.path)) {
                holder.setTextColor(0.5F)
            } else{
                holder.setTextColor(1F)
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    fun updateList(newItemList: List<NodeItemViewModel>) {
        if (selectedItems.size < 1 && cutItems.size < 1) {
            actionMode?.finish()
        }
        itemList = newItemList
        notifyDataSetChanged()
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.action_mode_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_delete -> {
                deleteSelectedItems()
                mode?.finish()
                return true
            }
            R.id.menu_cut -> {
                cutSelectedItems()
                item.isVisible = false
                mode?.menu?.findItem(R.id.menu_move)?.isVisible = true
                return true
            }
            R.id.menu_move -> {
                moveSelectedItems()
                mode?.finish()
                return true
            }
            R.id.menu_edit -> {
                showEditDialog()
                mode?.finish()
                return true
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
        mainActivity.invalidateOptionsMenu()
        if (!hitBoxHitted) { //to clear selection after pressing "back" BUTTON
            selectedItems.clear()
            cutItems.clear()
        }
        notifyDataSetChanged()
    }

    private fun deleteSelectedItems() {
        selectedItems.forEach{position ->
            nodeRepository.deleteNode(itemList[position].path)
        }
        selectedItems.clear()
    }

    private fun cutSelectedItems() {
        selectedItems.forEach{position ->
            val item = itemList[position]
            cutItems.add(item.path)
        }
        selectedItems.clear()
        nodeRepository.cutItems = cutItems
        notifyDataSetChanged()
    }

    private fun moveSelectedItems() {
        cutItems.forEach{path ->
            nodeRepository.moveNodes(mainActivity, path)
        }
    }

    private fun editSelectedItem(newName: String) {
        if (selectedItems.size == 1)
            nodeRepository.moveNodes(mainActivity, itemList[selectedItems.first()].path, newName)
    }

    private fun showEditDialog() {
        if (selectedItems.size == 1) {
            val position = selectedItems.first()
            val fileItem = itemList[position]

            val builder = AlertDialog.Builder(mainActivity)
            val inflater = mainActivity.layoutInflater
            val dialogView = inflater.inflate(R.layout.edit_dialog, null)

            val editText = dialogView.findViewById<TextInputEditText>(R.id.editText)
            editText.setText(fileItem.name)

            builder.setView(dialogView)
                .setTitle("Edit Item Name")
                .setPositiveButton("OK") { _, _ ->
                    val newName = editText.text?.toString() ?: ""
                    editSelectedItem(newName)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }

            builder.create().show()
        }
    }

    abstract inner class BaseViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        fun setTextColor(float: Float) {
            binding.root.alpha = float
        }


        private fun toggleSelection(position: Int) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
                if (selectedItems.size < 1 && cutItems.size < 1) {
                    actionMode?.finish()
                }
            } else {
                selectedItems.add(position)
                if (actionMode == null) {
                    actionMode = mainActivity.startActionMode(this@BaseAdapter)
                }
            }
            notifyItemChanged(position)
        }

        override fun onClick(v: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return

            val fileItem = itemList[adapterPosition]

            if (selectedItems.size < 1) {
                if (fileItem.type == FileType.FOLDER)
                    if (!cutItems.contains(fileItem.path)/*fileItem.image?.alpha!! == 255*/) {
                        nodeRepository.readNode(fileItem.path)
                    } else {
                        Toast.makeText(v.context, "Can't access 'cut' directories", Toast.LENGTH_SHORT).show()
                    }
                else {
                    NodeDialogFragment(mainActivity, fileItem).show(mainActivity.supportFragmentManager, "NodeDialog")
                }

            } else toggleSelection(adapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                toggleSelection(position)
                return true
            }
            return false
        }
        protected fun getItemImage(fileItem: NodeItemViewModel): Drawable? {
            return AppCompatResources.getDrawable(itemView.context, fileItem.drawable)
        }

        abstract fun bind(fileItem: NodeItemViewModel)
    }
}
