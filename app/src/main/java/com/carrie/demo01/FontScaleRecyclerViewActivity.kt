package com.carrie.demo01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FontScaleRecyclerViewActivity : ComponentActivity() {

    private lateinit var demoAdapter: DemoListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_font_scale_recycler_view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.font_scale_demo_root)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        demoAdapter = DemoListAdapter(
            initialScale = savedInstanceState?.getFloat(KEY_TEXT_SCALE) ?: 1f,
        )
        findViewById<RecyclerView>(R.id.demo_recycler_view).apply {
            layoutManager = LinearLayoutManager(this@FontScaleRecyclerViewActivity)
            adapter = demoAdapter
            itemAnimator = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putFloat(KEY_TEXT_SCALE, demoAdapter.currentScale)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_TEXT_SCALE = "text_scale"
    }
}
