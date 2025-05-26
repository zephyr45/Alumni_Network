package com.example.alumni_network.bottomfragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alumni_network.R
import org.w3c.dom.Text
import javax.crypto.spec.PSource

class LanguageAdapterForJoinedNetwork(
    private val dataList:ArrayList<LanguageData>,
    private val listener: OnItemClickListener)
    :RecyclerView.Adapter<LanguageAdapterForJoinedNetwork.ViewHolderClass>()
{
    interface OnItemClickListener {
        fun NetworkClick(position: Int)
        fun onMembershipClick(position: Int)
        fun onClick2(position: Int)
        fun onClick3(position: Int)
        fun ondeleteClick(position: Int)
    }
    class ViewHolderClass(itemView:View,private val listener: OnItemClickListener):RecyclerView.ViewHolder(itemView){
        val logoforJoined=itemView.findViewById<ImageView>(R.id.networklogoforJoined)
        val titleforJoined=itemView.findViewById<TextView>(R.id.networkTitleforJoined)
        val addressforJoined=itemView.findViewById<TextView>(R.id.networkAddressForJoined)
        val clickingtoopen=itemView.findViewById<LinearLayout>(R.id.clickingtoopen)
        val membershipClick=itemView.findViewById<TextView>(R.id.membership)
        val click2=itemView.findViewById<TextView>(R.id.click2)
        val click3=itemView.findViewById<TextView>(R.id.click3)
        val deleteClick=itemView.findViewById<ImageView>(R.id.deleteJoinedNetwork)
        init {
            clickingtoopen.setOnClickListener {
                listener.NetworkClick(adapterPosition)
            }
            membershipClick.setOnClickListener {
                listener.onMembershipClick(adapterPosition)
            }
            click2.setOnClickListener {
                listener.onClick2(adapterPosition)
            }
            click3.setOnClickListener {
                listener.onClick3(adapterPosition)
            }
            deleteClick.setOnClickListener {
                listener.ondeleteClick(adapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val itemView=LayoutInflater.from(parent.context).inflate(R.layout.cardsofnetworkjoined,parent,false)
        return ViewHolderClass(itemView,listener)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem=dataList[position]
        Glide.with(holder.itemView.context)
            .load(currentItem.logo)
            .into(holder.logoforJoined)
        holder.titleforJoined.text=currentItem.title
        holder.addressforJoined.text=currentItem.address

    }

}