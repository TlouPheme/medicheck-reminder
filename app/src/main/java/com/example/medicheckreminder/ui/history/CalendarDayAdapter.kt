package com.example.medicheckreminder.ui.history

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemCalendarDayBinding
import com.google.android.material.color.MaterialColors

class CalendarDayAdapter(
    private val onDateClick: (String) -> Unit
) : ListAdapter<CalendarDay, CalendarDayAdapter.DayViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DayViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay) {
            if (day.isPlaceholder) {
                binding.root.isClickable = false
                binding.root.isFocusable = false
                binding.root.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                binding.textDay.isInvisible = true
                binding.bgSelected.isVisible = false
                binding.dotAdherence.isInvisible = true
                return
            }

            binding.root.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
            binding.root.isFocusable = true
            binding.textDay.isVisible = true
            binding.textDay.text = day.dayOfMonth?.toString().orEmpty()
            binding.bgSelected.isVisible = day.isSelected

            val colorOnSurface = MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnSurface
            )
            val colorPrimary = MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorPrimary
            )
            binding.textDay.setTextColor(
                when {
                    day.isSelected -> MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr.colorOnPrimaryContainer
                    )
                    day.isToday -> colorPrimary
                    else -> colorOnSurface
                }
            )
            binding.textDay.alpha = if (day.isInFilterRange) 1f else 0.38f

            when (day.adherence) {
                DayAdherence.NONE -> binding.dotAdherence.isInvisible = true
                DayAdherence.GOOD -> {
                    binding.dotAdherence.isVisible = true
                    binding.dotAdherence.backgroundTintList = ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.root,
                            com.google.android.material.R.attr.colorSecondary
                        )
                    )
                }
                DayAdherence.MISSED -> {
                    binding.dotAdherence.isVisible = true
                    binding.dotAdherence.backgroundTintList = ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.root,
                            com.google.android.material.R.attr.colorError
                        )
                    )
                }
            }

            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { day.dateKey?.let(onDateClick) }
            val details = buildList {
                if (day.isToday) add(binding.root.context.getString(R.string.a11y_calendar_today))
                if (day.isSelected) add(binding.root.context.getString(R.string.a11y_calendar_selected))
                add(
                    when (day.adherence) {
                        DayAdherence.GOOD -> binding.root.context.getString(R.string.a11y_calendar_taken)
                        DayAdherence.MISSED -> binding.root.context.getString(R.string.a11y_calendar_missed)
                        DayAdherence.NONE -> binding.root.context.getString(R.string.a11y_calendar_no_doses)
                    }
                )
            }.joinToString(", ")
            binding.root.contentDescription = binding.root.context.getString(
                R.string.cd_calendar_day_details,
                day.dayOfMonth ?: 0,
                details
            )
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.cellIndex == newItem.cellIndex
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}
