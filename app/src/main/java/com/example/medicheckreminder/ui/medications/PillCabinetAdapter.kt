package com.example.medicheckreminder.ui.medications

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemPillCabinetBinding
import com.example.medicheckreminder.domain.model.Medication
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
            binding.root.contentDescription = itemView.context.getString(
                R.string.cd_medication_item,
                medication.name,
                medication.stockCount
            )

            binding.root.setOnClickListener { onClick(medication, binding.cardPillIcon) }
            binding.root.setOnLongClickListener {
                onLongClick(medication)
                true
            }
        }
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
