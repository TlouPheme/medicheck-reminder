package com.example.medicheckreminder.ui.history

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemDoseLogBinding
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Locale

class DoseLogAdapter : ListAdapter<DoseLog, DoseLogAdapter.LogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemDoseLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(
        private val binding: ItemDoseLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: DoseLog) {
            binding.textMedicationName.text = log.medicationName
            binding.textDosage.text = log.dosage
            binding.textScheduledTime.text = itemView.context.getString(
                R.string.history_log_when,
                formatDate(log.dateKey),
                log.scheduledTime
            )

            val (labelRes, colorAttr) = when (log.status) {
                Dose.Status.TAKEN -> R.string.status_taken to com.google.android.material.R.attr.colorSecondary
                Dose.Status.SKIPPED -> R.string.status_skipped to com.google.android.material.R.attr.colorError
                Dose.Status.SNOOZED -> R.string.status_snoozed to com.google.android.material.R.attr.colorPrimary
                Dose.Status.PENDING -> R.string.status_pending to com.google.android.material.R.attr.colorOnSurfaceVariant
                Dose.Status.MISSED -> R.string.status_missed to com.google.android.material.R.attr.colorError
            }
            val color = MaterialColors.getColor(binding.chipStatus, colorAttr)
            binding.chipStatus.text = itemView.context.getString(labelRes)
            binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color, 32)
            )
            binding.chipStatus.setTextColor(color)
            binding.root.contentDescription = itemView.context.getString(
                R.string.a11y_dose_card,
                log.medicationName,
                log.dosage,
                log.scheduledTime,
                itemView.context.getString(labelRes)
            )
        }

        private fun formatDate(dateKey: String): String {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) ?: return dateKey
            return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(parsed)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DoseLog>() {
        override fun areItemsTheSame(oldItem: DoseLog, newItem: DoseLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DoseLog, newItem: DoseLog): Boolean {
            return oldItem == newItem
        }
    }
}
