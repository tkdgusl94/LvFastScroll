package leveloper.lvfastscroll.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import leveloper.lvfastscroll.LvFastScroll

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_item)
        val fastScroll = findViewById<LvFastScroll>(R.id.fast_scroll)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SampleAdapter().apply {
            val items = getListItems()
            this.setData(items)
        }
        fastScroll.recyclerView = recyclerView
    }

    private fun getListItems(): List<String> {
        val items = mutableListOf<String>()

        for (i in 0..1000) {
            items.add(i.toString())
        }

        return items.sorted()
    }
}