package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity(){

    lateinit var navHome:FrameLayout
    lateinit var navBrowse: FrameLayout
    private val navItems = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navItems.add(findViewById<FrameLayout>(R.id.nav_home))
        navItems.add(findViewById<FrameLayout>(R.id.nav_browse))
        navItems.add(findViewById<FrameLayout>(R.id.nav_radio))
        navItems.add(findViewById<FrameLayout>(R.id.nav_library))
        navItems.add(findViewById<FrameLayout>(R.id.nav_five))
        navItems.add(findViewById<FrameLayout>(R.id.nav_six))

        // 2. Set click listeners and initial selection
        for (item in navItems) {
            item.setOnClickListener {
                selectItem(it)
                // You would typically handle fragment navigation here
            }
        }

        // Set initial selection
        selectItem(navItems.first())
    }

    // MainActivity.kt

    // Helper class to hold all inner views of a custom item
    data class NavItemViews(
        val root: View,
        val container: ConstraintLayout,
        val icon: ImageView,
        val text: TextView
    )

    // Helper function to extract views from the FrameLayout
    private fun getNavItemViews(item: View): NavItemViews {
        val container = item.findViewById<ConstraintLayout>(R.id.item_container)
        return NavItemViews(
            root = item,
            container = container,
            icon = container.findViewById(R.id.item_icon),
            text = container.findViewById(R.id.item_text)
        )
    }

    private fun selectItem(selectedView: View) {
        for (item in navItems) {
            val views = getNavItemViews(item)

            if (item == selectedView) {
                // --- 1. SELECTED STATE: Horizontal Layout (Icon + Text) ---

                views.container.isSelected = true
                views.text.visibility = View.VISIBLE

                // To prevent overlap, we ensure the icon is constrained to the START
                // and the text is constrained to the END, making the container wrap the content.
                val iconParams = views.icon.layoutParams as ConstraintLayout.LayoutParams
                iconParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                iconParams.endToEnd = ConstraintLayout.NO_ID
                views.icon.layoutParams = iconParams

                // Reapply constraints just in case the text view was previously centered
                val textParams = views.text.layoutParams as ConstraintLayout.LayoutParams
                textParams.startToEnd = R.id.item_icon
                textParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                views.text.layoutParams = textParams

            } else {
                // --- 2. UNSELECTED STATE: Centered Icon Only ---

                views.container.isSelected = false
                views.text.visibility = View.GONE

                // We must adjust the ICON's constraints so it floats in the center
                // of the item_container (which still wraps the hidden text view, but that's fine).
                val iconParams = views.icon.layoutParams as ConstraintLayout.LayoutParams

                // Remove the explicit start constraint
                iconParams.startToStart = ConstraintLayout.NO_ID
                // Apply centering constraints (left-to-left and right-to-right)
                iconParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                iconParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

                views.icon.layoutParams = iconParams

                // Ensure the text view is still constrained relative to the icon
                val textParams = views.text.layoutParams as ConstraintLayout.LayoutParams
                textParams.startToEnd = R.id.item_icon
                textParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                views.text.layoutParams = textParams
            }
        }
    }
}
