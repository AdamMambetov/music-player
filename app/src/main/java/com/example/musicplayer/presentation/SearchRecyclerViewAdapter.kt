package com.example.musicplayer.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.databinding.FragmentSearchItemBinding
import com.example.musicplayer.domain.MusicInfo
import com.example.musicplayer.placeholder.PlaceholderContent.PlaceholderItem


class SearchRecyclerViewAdapter(
    private val values: LiveData<List<MusicInfo>>
) : RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        FragmentSearchItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values.value!![position]
        holder.idView.text = item.id.toString()
        holder.contentView.text = item.title
    }

    override fun getItemCount(): Int = values.value!!.size

    inner class ViewHolder(binding: FragmentSearchItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}