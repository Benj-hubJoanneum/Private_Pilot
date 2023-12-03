package at.privatepilot.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.widget.ProgressBar
import android.widget.RelativeLayout

class ProgressBarDialog(context: Context) {
    private val dialog: AlertDialog

    init {
        val progressBar = ProgressBar(context)
        val layout = RelativeLayout(context)
        layout.addView(progressBar)

        dialog = AlertDialog.Builder(context)
            .setView(layout)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun show() {
        dialog.show()
    }

    fun hide() {
        dialog.dismiss()
    }
}
