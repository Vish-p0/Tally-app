package com.example.tally.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.example.tally.R

class PinIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val indicators = mutableListOf<View>()
    private var pinLength = 4
    private var currentLength = 0
    
    init {
        orientation = HORIZONTAL
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_pin_indicator, this, true)
        
        // Get pin indicators
        indicators.add(findViewById(R.id.pinDot1))
        indicators.add(findViewById(R.id.pinDot2))
        indicators.add(findViewById(R.id.pinDot3))
        indicators.add(findViewById(R.id.pinDot4))
        
        // Set initial state
        updateIndicators()
    }
    
    /**
     * Set the number of digits entered (0-4)
     */
    fun setFilledCount(count: Int) {
        currentLength = count.coerceIn(0, pinLength)
        updateIndicators()
    }
    
    /**
     * Get the current number of digits entered
     */
    fun getFilledCount(): Int {
        return currentLength
    }
    
    /**
     * Update the visual indicators based on current state
     */
    private fun updateIndicators() {
        for (i in 0 until pinLength) {
            val isFilled = i < currentLength
            indicators[i].setBackgroundResource(
                if (isFilled) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            )
        }
    }
} 