import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.alumni_network.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    var messageId: String = ""  // Add a messageId field for easy identification
)
sealed class ChatItem {
    data class Message(val chatMessage: ChatMessage) : ChatItem()
    data class DateLabel(val date: String) : ChatItem()
}


private const val VIEW_TYPE_DATE_LABEL = 0
private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2


class CommunityChatsAdapter(
    private val messages: MutableList<ChatItem>,  // Make messages mutable to allow deletion
    private val currentUserId: String,
    private val onDeleteMessage: (messageId: String,senderId: String) -> Unit  // Callback to handle message deletion
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ViewHolder for sent messages
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.sendmessagetext)
        val messageTime: TextView = itemView.findViewById(R.id.sentTime)
    }
    inner class DateLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateLabelText)
    }

    // ViewHolder for received messages
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.receivemessagetext)
        val username: TextView = itemView.findViewById(R.id.sendername)
        val messageTime: TextView = itemView.findViewById(R.id.receiveTime)
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = messages[position]) {
            is ChatItem.DateLabel -> VIEW_TYPE_DATE_LABEL
            is ChatItem.Message -> {
                if (item.chatMessage.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
            else -> throw IllegalArgumentException("Unknown ChatItem type at position $position")
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_LABEL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.date_label, parent, false)
                DateLabelViewHolder(view)
            }
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.send_message, parent, false)
                SentMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.receive_message, parent, false)
                ReceivedMessageViewHolder(view)
            }
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = messages[position]
        when (holder) {
            is DateLabelViewHolder -> {
                val dateLabel = (item as ChatItem.DateLabel).date
                holder.dateText.text = dateLabel
            }
            is SentMessageViewHolder -> {
                val message = (item as ChatItem.Message).chatMessage
                holder.messageText.text = message.messageText
                holder.messageTime.text = formatTime(message.timestamp)
                holder.itemView.setOnLongClickListener {
                    if (message.senderId == currentUserId) showContextMenu(holder.itemView, message)
                    true
                }
            }
            is ReceivedMessageViewHolder -> {
                val message = (item as ChatItem.Message).chatMessage
                holder.messageText.text = message.messageText
                holder.username.text = message.senderName
                holder.messageTime.text = formatTime(message.timestamp)
            }
        }
    }
    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }




    override fun getItemCount(): Int = messages.size

    // Show a context menu (PopupMenu) for deleting the message
    private fun showContextMenu(view: View, message: ChatMessage) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.deletemenu, popupMenu.menu)

        // Set the delete option click listener
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {
                    if (message.messageId.isNotEmpty()) {
                        onDeleteMessage(message.messageId, message.senderId)  // Pass both messageId and senderId
                    } else {
                        Toast.makeText(view.context, "Message ID is missing", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }





    // Method to delete a message from the list
    fun deleteMessage(messageId: String) {
        val index = messages.indexOfFirst {
            it is ChatItem.Message && it.chatMessage.messageId == messageId
        }
        if (index != -1) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

}
