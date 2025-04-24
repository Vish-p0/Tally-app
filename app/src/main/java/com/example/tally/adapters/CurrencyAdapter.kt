package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.models.Currency

class CurrencyAdapter(
    private val currencies: List<Currency>,
    private val onCurrencySelected: (Currency) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency, parent, false)
        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currency = currencies[position]
        holder.bind(currency)
        holder.itemView.setOnClickListener {
            onCurrencySelected(currency)
        }
    }

    override fun getItemCount(): Int = currencies.size

    class CurrencyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flagTextView: TextView = itemView.findViewById(R.id.tvCurrencyFlag)
        private val nameTextView: TextView = itemView.findViewById(R.id.tvCurrencyName)
        private val codeTextView: TextView = itemView.findViewById(R.id.tvCurrencyCode)
        private val symbolTextView: TextView = itemView.findViewById(R.id.tvCurrencySymbol)

        fun bind(currency: Currency) {
            flagTextView.text = currency.flag
            nameTextView.text = currency.name
            codeTextView.text = currency.code
            symbolTextView.text = currency.symbol
        }
    }
} 