package leveloper.lvfastscroll.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import leveloper.lvfastscroll.LvFastScroll
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var sampleAdapter: SampleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_item)
        val fastScroll = findViewById<LvFastScroll>(R.id.fast_scroll)

        sampleAdapter = SampleAdapter().apply {
            items = getListItems()
        }

        recyclerView.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = sampleAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    (layoutManager as? LinearLayoutManager)?.let {
                        val firstVisiblePosition = it.findFirstCompletelyVisibleItemPosition()
                        if (firstVisiblePosition <= 0) {
                            return
                        }
                        val item = sampleAdapter.items[firstVisiblePosition]
                        println("item: $item")

                        println("sub: ${item.first()}")
                        fastScroll.setBubbleText(item.first().toString())
                    }
                }
            })
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