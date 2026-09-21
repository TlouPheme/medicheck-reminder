package com.example.medicheckreminder.ui.health

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemHealthMeasurementBinding
import com.example.medicheckreminder.domain.model.HealthMeasurement
import com.example.medicheckreminder.domain.model.MeasurementType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthMeasurementAdapter :
    ListAdapter<HealthMeasurement, HealthMeasurementAdapter.Holder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHealthMeasurementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(
        private val binding: ItemHealthMeasurementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HealthMeasurement) {
            binding.textValue.text = formatValue(item)
            binding.textRecordedAt.text = DATE_FORMAT.format(Date(item.recordedAtMillis))
            binding.root.contentDescription = itemView.context.getString(
                R.string.a11y_health_reading,
                formatValue(item),
                DATE_FORMAT.format(Date(item.recordedAtMillis))
            )
        }

        private fun formatValue(item: HealthMeasurement): String {
            return when (item.type) {
                MeasurementType.WEIGHT ->
                    itemView.context.getString(R.string.health_value_weight, item.value)
                MeasurementType.BLOOD_PRESSURE ->
                    itemView.context.getString(
                        R.string.health_value_bp,
                        item.value.toInt(),
                        item.secondaryValue?.toInt() ?: 0
                    )
                MeasurementType.BLOOD_SUGAR ->
                    itemView.context.getString(R.string.health_value_sugar, item.value.toInt())
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<HealthMeasurement>() {
        override fun areItemsTheSame(
            oldItem: HealthMeasurement,
            newItem: HealthMeasurement
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: HealthMeasurement,
            newItem: HealthMeasurement
        ): Boolean = oldItem == newItem
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault())
    }
}
