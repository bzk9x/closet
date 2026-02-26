package com.bzk9x.closet

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bzk9x.closet.utils.ensureMinTouchTarget

class ClosetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_closet)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val addOutfitButton: ImageView = findViewById(R.id.tab_add_button)
        val viewArchiveButton: ImageView = findViewById(R.id.tab_archive_button)

        addOutfitButton.ensureMinTouchTarget(48)
        viewArchiveButton.ensureMinTouchTarget(48)
    }
}