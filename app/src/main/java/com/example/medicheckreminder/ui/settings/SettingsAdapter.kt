package com.example.medicheckreminder.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.ItemSettingDangerBinding
import com.example.medicheckreminder.databinding.ItemSettingDropdownBinding
import com.example.medicheckreminder.databinding.ItemSettingHeaderBinding
import com.example.medicheckreminder.databinding.ItemSettingInfoBinding
import com.example.medicheckreminder.databinding.ItemSettingNavigationBinding
import com.example.medicheckreminder.databinding.ItemSettingTextBinding
import com.example.medicheckreminder.databinding.ItemSettingToggleBinding

class SettingsAdapter(
    private val onDropdownClick: (SettingRow.Dropdown) -> Unit,
    private val onToggle: (String, Boolean) -> Unit,
    private val onNavigationClick: (String) -> Unit,
    private val onTextChanged: (String, String) -> Unit,
    private val onDangerClick: (String) -> Unit
) : ListAdapter<SettingRow, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SettingRow.Header -> TYPE_HEADER
        is SettingRow.Dropdown -> TYPE_DROPDOWN
        is SettingRow.Toggle -> TYPE_TOGGLE
        is SettingRow.Navigation -> TYPE_NAV
        is SettingRow.TextField -> TYPE_TEXT
        is SettingRow.Info -> TYPE_INFO
        is SettingRow.DangerButton -> TYPE_DANGER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemSettingHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_DROPDOWN -> DropdownViewHolder(
                ItemSettingDropdownBinding.inflate(inflater, parent, false)
            )
            TYPE_TOGGLE -> ToggleViewHolder(
                ItemSettingToggleBinding.inflate(inflater, parent, false)
            )
            TYPE_NAV -> NavigationViewHolder(
                ItemSettingNavigationBinding.inflate(inflater, parent, false)
            )
            TYPE_TEXT -> TextFieldViewHolder(
                ItemSettingTextBinding.inflate(inflater, parent, false)
            )
            TYPE_INFO -> InfoViewHolder(
                ItemSettingInfoBinding.inflate(inflater, parent, false)
            )
            TYPE_DANGER -> DangerViewHolder(
                ItemSettingDangerBinding.inflate(inflater, parent, false)
            )
            else -> error("Unknown settings row type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SettingRow.Header -> (holder as HeaderViewHolder).bind(item)
            is SettingRow.Dropdown -> (holder as DropdownViewHolder).bind(item)
            is SettingRow.Toggle -> (holder as ToggleViewHolder).bind(item)
            is SettingRow.Navigation -> (holder as NavigationViewHolder).bind(item)
            is SettingRow.TextField -> (holder as TextFieldViewHolder).bind(item)
            is SettingRow.Info -> (holder as InfoViewHolder).bind(item)
            is SettingRow.DangerButton -> (holder as DangerViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemSettingHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingRow.Header) {
            binding.textHeader.text = item.title
            ViewCompat.setAccessibilityHeading(binding.textHeader, true)
        }
    }

    inner class DropdownViewHolder(
        private val binding: ItemSettingDropdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingRow.Dropdown) {
            binding.textTitle.text = item.title
            binding.textValue.text = item.valueLabel
            binding.root.isEnabled = item.enabled
            binding.root.alpha = if (item.enabled) 1f else 0.4f
            ViewCompat.setStateDescription(binding.root, item.valueLabel)
            binding.root.setOnClickListener {
                if (item.enabled) onDropdownClick(item)
            }
        }
    }

    inner class ToggleViewHolder(
        private val binding: ItemSettingToggleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingRow.Toggle) {
            binding.textTitle.text = item.title
            binding.textSubtitle.isVisible = !item.subtitle.isNullOrBlank()
            binding.textSubtitle.text = item.subtitle
            binding.switchSetting.isClickable = false
            binding.switchSetting.isFocusable = false
            binding.switchSetting.isChecked = item.checked
            binding.switchSetting.isEnabled = item.enabled
            binding.root.isEnabled = item.enabled
            binding.root.alpha = if (item.enabled) 1f else 0.4f
            val state = binding.root.context.getString(
                if (item.checked) R.string.a11y_on else R.string.a11y_off
            )
            ViewCompat.setStateDescription(binding.root, state)
            binding.root.setOnClickListener {
                if (!item.enabled) return@setOnClickListener
                val checked = !binding.switchSetting.isChecked
                binding.switchSetting.isChecked = checked
                ViewCompat.setStateDescription(
                    binding.root,
                    binding.root.context.getString(
                        if (checked) R.string.a11y_on else R.string.a11y_off
                    )
                )
                onToggle(item.id, checked)
            }
        }
    }

    inner class NavigationViewHolder(
        private val binding: ItemSettingNavigationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingRow.Navigation) {
            binding.textTitle.text = item.title
            binding.textValue.isVisible = item.valueLabel.isNotBlank()
            binding.textValue.text = item.valueLabel
            binding.root.isEnabled = item.enabled
            binding.root.alpha = if (item.enabled) 1f else 0.4f
            if (item.valueLabel.isNotBlank()) {
                ViewCompat.setStateDescription(binding.root, item.valueLabel)
            }
            binding.root.setOnClickListener {
                if (item.enabled) onNavigationClick(item.id)
            }
        }
    }

    inner class TextFieldViewHolder(
        private val binding: ItemSettingTextBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.input.doAfterTextChanged { text ->
                val id = binding.root.tag as? String ?: return@doAfterTextChanged
                if (binding.input.hasFocus()) {
                    onTextChanged(id, text?.toString().orEmpty())
                }
            }
        }

        fun bind(item: SettingRow.TextField) {
            binding.root.tag = item.id
            binding.layout.hint = item.title
            binding.input.inputType = item.inputType
            if (!binding.input.hasFocus() && binding.input.text?.toString() != item.value) {
                binding.input.setText(item.value)
            }
            binding.layout.error = item.error
        }
    }

    inner class InfoViewHolder(
        private val binding: ItemSettingInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingRow.Info) {
            binding.textTitle.text = item.title
            binding.textValue.text = item.value
        }
    }

    inner class DangerViewHolder(
        private val binding: ItemSettingDangerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingRow.DangerButton) {
            binding.btnDanger.text = item.title
            binding.btnDanger.setOnClickListener { onDangerClick(item.id) }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_DROPDOWN = 1
        const val TYPE_TOGGLE = 2
        const val TYPE_NAV = 3
        const val TYPE_TEXT = 4
        const val TYPE_INFO = 5
        const val TYPE_DANGER = 6

        val DiffCallback = object : DiffUtil.ItemCallback<SettingRow>() {
            override fun areItemsTheSame(oldItem: SettingRow, newItem: SettingRow): Boolean {
                return oldItem.id == newItem.id && oldItem::class == newItem::class
            }

            override fun areContentsTheSame(oldItem: SettingRow, newItem: SettingRow): Boolean {
                return oldItem == newItem
            }
        }
    }
}
