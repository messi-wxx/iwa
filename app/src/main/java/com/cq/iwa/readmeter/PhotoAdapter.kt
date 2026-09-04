package com.cq.iwa.readmeter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val onAdd: () -> Unit,
    private val onPreview: (ImageView, Int, List<String>) -> Unit,
    private val onDelete: (Int) -> Unit,
) : RecyclerView.Adapter<PhotoAdapter.Holder>() {

    private val items = mutableListOf<String>()

    fun submit(list: List<String>) {
        if (items == list) return
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun paths(): List<String> = items.toList()

    fun imageAt(recyclerView: RecyclerView, position: Int): ImageView? {
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? Holder
        return holder?.binding?.ivPhoto
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (position == items.size) {
            holder.binding.ivPhoto.bindMeterPhoto(null)
            holder.binding.addContainer.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onAdd() }
            holder.itemView.setOnLongClickListener(null)
        } else {
            val path = items[position]
            holder.binding.addContainer.visibility = View.GONE
            holder.binding.ivPhoto.bindMeterPhoto(path)
            holder.itemView.setOnClickListener {
                onPreview(holder.binding.ivPhoto, position, items.toList())
            }
            holder.itemView.setOnLongClickListener {
                onDelete(position)
                true
            }
        }
    }

    class Holder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root)
}
