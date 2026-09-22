package com.gunz.makro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelectedAppAdapter(
    private val apps: List<AppInfo>,
    private val onAddClick: () -> Unit,
    private val onRemoveClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == apps.size) TYPE_ADD else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_app, parent, false)
            AddViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_app, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener { onAddClick() }
        } else if (holder is ItemViewHolder) {
            val app = apps[position]
            holder.tvAppName.text = app.appName
            holder.imgIcon.setImageDrawable(app.icon)
            holder.btnRemove.setOnClickListener { onRemoveClick(app) }
        }
    }

    override fun getItemCount(): Int = apps.size + 1

    class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
        val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        val btnRemove: View = itemView.findViewById(R.id.btnRemoveApp)
    }
}
