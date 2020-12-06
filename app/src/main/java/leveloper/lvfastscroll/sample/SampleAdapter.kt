package leveloper.lvfastscroll.sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_view.view.*

class SampleAdapter: RecyclerView.Adapter<SampleAdapter.SampleViewHolder>() {

    var items: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SampleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        return SampleViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SampleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class SampleViewHolder(view: View): RecyclerView.ViewHolder(view) {
        fun bind(data: String) {
            itemView.text_view.text = data
        }
    }
}