package at.privatepilot.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import at.privatepilot.R
import at.privatepilot.databinding.NodeDialogBinding
import at.privatepilot.model.nodeItem.viewmodel.NodeItemViewModel
import at.privatepilot.restapi.service.NodeRepository

class NodeDialogFragment (
    private val context: Context,
    private val node: NodeItemViewModel
) : DialogFragment(), NodeRepository.DownloadCallback {
    private var _binding: NodeDialogBinding? = null
    private val binding get() = _binding!!

    private val nodeRepository = NodeRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NodeDialogBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.viewModel = node

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.download.setOnClickListener {
            nodeRepository.downloadFile(node.path)
        }

        binding.openFile.setOnClickListener{
            nodeRepository.openFile(node.path)
        }
        binding.executePendingBindings()
        if (node.bitmap != null)
            binding.imageView.setImageBitmap(node.bitmap) // is not accepted yet

        nodeRepository.setDownloadCallback(this)
        switchButton()
        return root
    }

    override fun onResume() {
        super.onResume()

        // Calculate the desired width (screen width - 20sp)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val desiredWidth = screenWidth - resources.getDimensionPixelSize(R.dimen.dialog_margin)

        // Set the dialog's width
        dialog?.window?.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun switchButton() {
        // Use runOnUiThread to update UI elements on the main thread
        activity?.runOnUiThread {
            if (nodeRepository.fileExist(node.path, context)) {
                binding.download.visibility = View.GONE
                binding.openFile.visibility = View.VISIBLE
            } else {
                binding.download.visibility = View.VISIBLE
                binding.openFile.visibility = View.GONE
            }
        }
    }

    override fun onDownloadFinished() {
        // Use runOnUiThread to update UI elements on the main thread
        activity?.runOnUiThread {
            switchButton()
        }
    }
}
