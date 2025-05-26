package com.example.alumni_network.bottomfragment

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alumni_network.R

class LanguageAdapter(var mList:List<LanguageData>,
    private val onItemClickListener:OnItemClickListener):
    RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>(){
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
    inner class LanguageViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val logo:ImageView=itemView.findViewById(R.id.networklogo)
        val titlename:TextView=itemView.findViewById(R.id.networkDescription)
        val address:TextView=itemView.findViewById(R.id.networkaddress)
        init {
            itemView.setOnClickListener {
                onItemClickListener.onItemClick(adapterPosition)
            }
        }
    }
    fun setFilteredList(mList: List<LanguageData>){
        this.mList=mList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.eachitemfornetwork,parent,false)
        return LanguageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(mList[position].logo)
            .into(holder.logo)

        holder.titlename.text=mList[position].title
            holder.address.text=mList[position].address
    }


}