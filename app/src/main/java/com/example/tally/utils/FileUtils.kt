package com.example.tally.utils

import android.content.Context
import java.io.IOException

object FileUtils {
    fun writeToFile(context: Context, filename: String, data: String) {
        try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(data.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readFromFile(context: Context, filename: String): String {
        return try {
            context.openFileInput(filename).bufferedReader().use {
                it.readText()
            }
        } catch (e: IOException) {
            ""
        }
    }
}