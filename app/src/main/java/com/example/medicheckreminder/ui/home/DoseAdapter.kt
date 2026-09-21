package com.example.medicheckreminder.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemDoseCardBinding
import com.example.medicheckreminder.databinding.ItemTimeHeaderBinding
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.ui.motion.applyPressScale
import com.example.medicheckreminder.ui.motion.cancelPressScale

sealed class DoseListItem {
    data class Header(val time: String) : DoseListItem()
    data class DoseItem(val dose: Dose) : DoseListItem()
}

class DoseAdapter(
    private val onTakenClick: (Dose) -> Unit,
    private val onSnoozeClick: (Dose) -> Unit,
    private val onSkipClick: (Dose) -> Unit
) : ListAdapter<DoseListItem, RecyclerView.ViewHolder>(DoseDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DoseListItem.Header -> TYPE_HEADER
            is DoseListItem.DoseItem -> TYPE_DOSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemTimeHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_DOSE -> DoseViewHolder(
                ItemDoseCardBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DoseListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is DoseListItem.DoseItem -> (holder as DoseViewHolder).bind(item.dose, animateTaken = false)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder is DoseViewHolder && payloads.contains(PAYLOAD_TAKEN)) {
            holder.bind((getItem(position) as DoseListItem.DoseItem).dose, animateTaken = true)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is DoseViewHolder) holder.resetMotion()
        super.onViewRecycled(holder)
    }

    class HeaderViewHolder(
        private val binding: ItemTimeHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: DoseListItem.Header) {
            binding.textTimeHeader.text = header.time
            ViewCompat.setAccessibilityHeading(binding.textTimeHeader, true)
        }
    }

    inner class DoseViewHolder(
        private val binding: ItemDoseCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dose: Dose, animateTaken: Boolean) {
            binding.textMedicationName.text = dose.medicationName
            binding.textDosage.text = dose.dosage
            binding.textScheduledTime.text = itemView.context.getString(
                R.string.home_scheduled_for,
                dose.scheduledTime
            )

            val isActionable = dose.status == Dose.Status.PENDING ||
                dose.status == Dose.Status.SNOOZED
            val restAlpha = if (isActionable) 1f else 0.6f
            binding.btnTaken.isEnabled = isActionable
            binding.btnSnooze.isEnabled = isActionable
            binding.btnSkip.isEnabled = isActionable
            binding.layoutActions.isVisible = isActionable
            binding.root.applyPressScale(restAlpha = restAlpha)

            val statusRes = when (dose.status) {
                Dose.Status.TAKEN -> R.string.status_taken
                Dose.Status.SKIPPED -> R.string.status_skipped
                Dose.Status.SNOOZED -> R.string.status_snoozed
                Dose.Status.PENDING -> R.string.status_pending
                Dose.Status.MISSED -> R.string.status_missed
            }
            binding.root.contentDescription = itemView.context.getString(
                R.string.a11y_dose_card,
                dose.medicationName,
                dose.dosage,
                dose.scheduledTime,
                itemView.context.getString(statusRes)
            )
            ViewCompat.setStateDescription(
                binding.root,
                itemView.context.getString(statusRes)
            )

            binding.btnTaken.setOnClickListener { onTakenClick(dose) }
            binding.btnSnooze.setOnClickListener { onSnoozeClick(dose) }
            binding.btnSkip.setOnClickListener { onSkipClick(dose) }

            if (animateTaken) {
                binding.root.alpha = 1f
                binding.root.animate()
                    .alpha(restAlpha)
                    .setStartDelay(TAKEN_ANIM_MS)
                    .setDuration(180)
                    .start()
            } else {
                binding.root.alpha = restAlpha
            }
        }

        fun resetMotion() {
            binding.root.cancelPressScale()
        }
    }

    private class DoseDiffCallback : DiffUtil.ItemCallback<DoseListItem>() {
        override fun areItemsTheSame(oldItem: DoseListItem, newItem: DoseListItem): Boolean {
            return when {
                oldItem is DoseListItem.Header && newItem is DoseListItem.Header ->
                    oldItem.time == newItem.time
                oldItem is DoseListItem.DoseItem && newItem is DoseListItem.DoseItem ->
                    oldItem.dose.id == newItem.dose.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DoseListItem, newItem: DoseListItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: DoseListItem, newItem: DoseListItem): Any? {
            if (oldItem is DoseListItem.DoseItem && newItem is DoseListItem.DoseItem) {
                val becameTaken = oldItem.dose.status != Dose.Status.TAKEN &&
                    newItem.dose.status == Dose.Status.TAKEN
                if (becameTaken) return PAYLOAD_TAKEN
            }
            return null
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_DOSE = 1
        const val PAYLOAD_TAKEN = "taken"
        const val TAKEN_ANIM_MS = 520L
    }
}
