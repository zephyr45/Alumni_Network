package com.example.alumni_network

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class ForMember(
    val id:String,
    val profileImage: String,
    val name: String,
    val graduationYear: String,
    val phoneNo: String,
    val currentlyWorking:String,
    val jobTitle: String,
    val companyName: String,
    val associateType:String,
    val course:String,
    val city:String,
    val networkId:String
)

class AdapterForMembers(
    var mList: ArrayList<ForMember>,
    private val onItemClickListener: OnItemClick
) : RecyclerView.Adapter<AdapterForMembers.ViewHolderClass>() {

    var filteredList = ArrayList<ForMember>()

    init {
        filteredList.addAll(mList)  // Initialize the filtered list with the full list
    }

    interface OnItemClick {
        fun onMemberClick(position: Int)
    }

    // Single definition of setFilteredList
    fun updateFilteredList(filteredList: ArrayList<ForMember>) {
        this.filteredList = filteredList
        notifyDataSetChanged()  // Notify the adapter to refresh the RecyclerView
    }


    class ViewHolderClass(itemView: View, private val onItemClickListener: OnItemClick) : RecyclerView.ViewHolder(itemView) {
        val profileForMember = itemView.findViewById<ImageView>(R.id.memberImage)
        val nameOfMember = itemView.findViewById<TextView>(R.id.nameofMember)
        val graduationYearOfMember = itemView.findViewById<TextView>(R.id.graduationYearOfMember)
        val memberLayout = itemView.findViewById<LinearLayout>(R.id.memberLayout)

        init {
            memberLayout.setOnClickListener {
                onItemClickListener.onMemberClick(adapterPosition) // Corrected usage
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardsformemberdesign, parent, false)
        return ViewHolderClass(itemView, onItemClickListener) // Pass listener here
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem = filteredList[position]  // Use filteredList here instead of mList
        Glide.with(holder.itemView.context)
            .load(currentItem.profileImage)
            .placeholder(R.drawable.profiledummy)
            .error(R.drawable.profiledummy)
            .into(holder.profileForMember)
        holder.nameOfMember.text = currentItem.name
        if(currentItem.currentlyWorking=="Yes"){
            holder.graduationYearOfMember.text = "${currentItem.graduationYear}, ${currentItem.jobTitle.toUpperCase()} At ${currentItem.companyName.toUpperCase()}"
        }else{
            holder.graduationYearOfMember.text = "${currentItem.graduationYear},${currentItem.associateType}"
        }

    }
}

