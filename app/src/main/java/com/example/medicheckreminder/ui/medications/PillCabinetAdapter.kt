package com.example.medicheckreminder.ui.medications

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemPillCabinetBinding
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.domain.refill.RefillCalculator
import com.google.android.material.color.MaterialColors
import java.text.DateFormat
import java.util.Date
import com.example.medicheckreminder.ui.motion.NavMotion
import com.example.medicheckreminder.util.ColorGenerator

class PillCabinetAdapter(
    private val onClick: (Medication, View) -> Unit,
    private val onLongClick: (Medication) -> Unit
) : ListAdapter<Medication, PillCabinetAdapter.PillViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PillViewHolder {
        val binding = ItemPillCabinetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PillViewHolder(
        private val binding: ItemPillCabinetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medication: Medication) {
            val swatch = ColorGenerator.fromName(medication.name)
            binding.cardPillIcon.setCardBackgroundColor(swatch.background)
            binding.imagePillIcon.imageTintList = ColorStateList.valueOf(swatch.onBackground)

            binding.textMedicationName.text = medication.name
            binding.textStockBadge.text = medication.stockCount.toString()
            binding.textStockBadge.contentDescription = itemView.context.getString(
                R.string.cd_stock_count,
                medication.stockCount
            )
            binding.cardPillIcon.transitionName = NavMotion.pillTransitionName(medication.id)
            val refill = refillLabel(medication)
            binding.textRefill.isVisible = refill != null
            binding.textRefill.text = refill
            binding.textRefill.setTextColor(
                MaterialColors.getColor(
                    binding.textRefill,
                    if (RefillCalculator.isLow(medication)) {
                        com.google.android.material.R.attr.colorError
                    } else {
                        com.google.android.material.R.attr.colorOnSurfaceVariant
                    }
                )
            )
            binding.textInteraction.isVisible = medication.interactionNotes.isNotBlank()
            binding.textInteraction.text = medication.interactionNotes

            binding.root.contentDescription = listOfNotNull(
                itemView.context.getString(
                    R.string.cd_medication_item,
                    medication.name,
                    medication.stockCount
                ),
                refill,
                medication.interactionNotes.takeIf { it.isNotBlank() }
            ).joinToString(". ")

            binding.root.setOnClickListener { onClick(medication, binding.cardPillIcon) }
            binding.root.setOnLongClickListener {
                onLongClick(medication)
                true
            }
        }
    }

    /** Refill calculator line: hidden when stock is not tracked or the medication is as needed. */
    private fun PillViewHolder.refillLabel(medication: Medication): String? {
        if (medication.stockCount <= 0) return null
        val days = RefillCalculator.daysRemaining(medication) ?: return null
        val context = itemView.context
        if (RefillCalculator.isLow(medication)) {
            return context.resources.getQuantityString(R.plurals.refill_low, days, days)
        }
        val date = RefillCalculator.refillDateMillis(medication, System.currentTimeMillis()) ?: return null
        return context.getString(R.string.refill_by, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(date)))
    }

    private class DiffCallback : DiffUtil.ItemCallback<Medication>() {
        override fun areItemsTheSame(oldItem: Medication, newItem: Medication): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medication, newItem: Medication): Boolean {
            return oldItem == newItem
        }
    }
}
