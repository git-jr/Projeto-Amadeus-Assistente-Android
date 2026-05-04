package com.paradoxo.amadeus.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paradoxo.amadeus.R
import com.paradoxo.amadeus.modelo.ActionIntent
import com.paradoxo.amadeus.modelo.ActionType

class AdapterAcoes(
    private val acoes: MutableList<ActionIntent>,
    private val onPickReference: (onPicked: (String) -> Unit) -> Unit
) : RecyclerView.Adapter<AdapterAcoes.ViewHolder>() {

    private val actionTypeNames = ActionType.values().map { it.name }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stepNumber: android.widget.TextView = view.findViewById(R.id.stepNumberTextView)
        val spinner: Spinner = view.findViewById(R.id.actionTypeSpinner)
        val targetLayout: TextInputLayout = view.findViewById(R.id.targetLayout)
        val targetEditText: TextInputEditText = view.findViewById(R.id.targetEditText)
        val valueLayout: TextInputLayout = view.findViewById(R.id.valueLayout)
        val valueEditText: TextInputEditText = view.findViewById(R.id.valueEditText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteActionButton)
        val insertReferenceButton: MaterialButton = view.findViewById(R.id.insertReferenceButton)

        var targetWatcher: TextWatcher? = null
        var valueWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_acao_macro, parent, false)
        val holder = ViewHolder(view)

        val spinnerAdapter = ArrayAdapter(
            parent.context,
            android.R.layout.simple_spinner_item,
            actionTypeNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        holder.spinner.adapter = spinnerAdapter

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val acao = acoes[position]
        holder.stepNumber.text = "Ação ${position + 1}"

        // Remove watchers before updating fields to avoid triggering callbacks during bind
        holder.targetEditText.removeTextChangedListener(holder.targetWatcher)
        holder.valueEditText.removeTextChangedListener(holder.valueWatcher)

        // Set spinner without triggering listener
        holder.spinner.onItemSelectedListener = null
        holder.spinner.setSelection(ActionType.values().indexOf(acao.type))

        holder.targetEditText.setText(acao.target)
        holder.valueEditText.setText(acao.value)

        updateFieldVisibility(holder, acao.type)

        // Re-attach listeners
        holder.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val adapterPos = holder.adapterPosition
                if (adapterPos == RecyclerView.NO_ID.toInt()) return
                val newType = ActionType.values()[pos]
                acoes[adapterPos] = acoes[adapterPos].copy(type = newType)
                updateFieldVisibility(holder, newType)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.targetWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val adapterPos = holder.adapterPosition
                if (adapterPos == RecyclerView.NO_ID.toInt()) return
                acoes[adapterPos] = acoes[adapterPos].copy(target = s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.targetEditText.addTextChangedListener(holder.targetWatcher)

        holder.valueWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val adapterPos = holder.adapterPosition
                if (adapterPos == RecyclerView.NO_ID.toInt()) return
                acoes[adapterPos] = acoes[adapterPos].copy(value = s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.valueEditText.addTextChangedListener(holder.valueWatcher)

        holder.deleteButton.setOnClickListener {
            val adapterPos = holder.adapterPosition
            if (adapterPos == RecyclerView.NO_ID.toInt()) return@setOnClickListener
            acoes.removeAt(adapterPos)
            notifyItemRemoved(adapterPos)
            // Atualiza numeração dos itens abaixo
            notifyItemRangeChanged(adapterPos, acoes.size - adapterPos)
        }

        holder.insertReferenceButton.setOnClickListener {
            onPickReference { entityName ->
                val editText = holder.valueEditText
                val start = editText.selectionStart.coerceAtLeast(0)
                val end = editText.selectionEnd.coerceAtLeast(start)
                editText.text?.replace(start, end, "{{$entityName}}")
                val adapterPos = holder.adapterPosition
                if (adapterPos != RecyclerView.NO_ID.toInt()) {
                    acoes[adapterPos] = acoes[adapterPos].copy(value = editText.text?.toString() ?: "")
                }
            }
        }
    }

    private fun updateFieldVisibility(holder: ViewHolder, type: ActionType) {
        val (showTarget, showValue) = when (type) {
            ActionType.CLICK -> true to false
            ActionType.TYPE -> false to true
            ActionType.OPEN_APP -> true to false
            ActionType.SCROLL -> false to true
            ActionType.SCROLL_HORIZONTAL -> false to true
            ActionType.WAIT -> false to true
            else -> false to false // IME_ACTION, BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS
        }
        holder.targetLayout.visibility = if (showTarget) View.VISIBLE else View.GONE
        holder.valueLayout.visibility = if (showValue) View.VISIBLE else View.GONE
        holder.insertReferenceButton.visibility = if (type == ActionType.TYPE) View.VISIBLE else View.GONE
        holder.valueLayout.hint = when (type) {
            ActionType.WAIT -> "Tempo em ms (ex: 2000)"
            ActionType.SCROLL, ActionType.SCROLL_HORIZONTAL -> "Direção (up/down ou left/right)"
            else -> "Valor"
        }

        if (!showTarget) holder.targetEditText.setText("")
        if (!showValue) holder.valueEditText.setText("")
    }

    override fun getItemCount(): Int = acoes.size

    fun addAcao(acao: ActionIntent = ActionIntent(ActionType.CLICK)) {
        acoes.add(acao)
        notifyItemInserted(acoes.size - 1)
    }

    fun getAcoes(): List<ActionIntent> = acoes.toList()
}
