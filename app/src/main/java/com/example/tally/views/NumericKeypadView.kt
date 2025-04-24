package com.example.tally.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.example.tally.R

class NumericKeypadView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val buttons = mutableListOf<Button>()
    private var onKeyPressListener: ((Int) -> Unit)? = null
    private var onBackspaceListener: (() -> Unit)? = null
    
    init {
        // Inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.view_numeric_keypad, this, true)
        
        // Setup digit buttons (0-9)
        setupDigitButton(view.findViewById(R.id.btnZero), 0)
        setupDigitButton(view.findViewById(R.id.btnOne), 1)
        setupDigitButton(view.findViewById(R.id.btnTwo), 2)
        setupDigitButton(view.findViewById(R.id.btnThree), 3)
        setupDigitButton(view.findViewById(R.id.btnFour), 4)
        setupDigitButton(view.findViewById(R.id.btnFive), 5)
        setupDigitButton(view.findViewById(R.id.btnSix), 6)
        setupDigitButton(view.findViewById(R.id.btnSeven), 7)
        setupDigitButton(view.findViewById(R.id.btnEight), 8)
        setupDigitButton(view.findViewById(R.id.btnNine), 9)
        
        // Setup backspace button
        val backspaceButton = view.findViewById<Button>(R.id.btnBackspace)
        backspaceButton.setOnClickListener {
            onBackspaceListener?.invoke()
        }
    }
    
    private fun setupDigitButton(button: Button, digit: Int) {
        buttons.add(button)
        button.setOnClickListener {
            onKeyPressListener?.invoke(digit)
        }
    }
    
    /**
     * Set a listener for digit button presses
     */
    fun setOnKeyPressListener(listener: (Int) -> Unit) {
        onKeyPressListener = listener
    }
    
    /**
     * Set a listener for backspace button presses
     */
    fun setOnBackspaceListener(listener: () -> Unit) {
        onBackspaceListener = listener
    }
    
    /**
     * Enable or disable all keypad buttons
     */
    fun setButtonsEnabled(enabled: Boolean) {
        buttons.forEach { it.isEnabled = enabled }
        findViewById<Button>(R.id.btnBackspace).isEnabled = enabled
    }
} 