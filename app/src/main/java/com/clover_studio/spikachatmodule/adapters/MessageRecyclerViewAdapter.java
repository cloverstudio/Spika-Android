package com.clover_studio.spikachatmodule.adapters;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.clover_studio.spikachatmodule.R;
import com.clover_studio.spikachatmodule.lazy.ImageLoaderSpice;
import com.clover_studio.spikachatmodule.models.Message;
import com.clover_studio.spikachatmodule.models.User;
import com.clover_studio.spikachatmodule.utils.Const;
import com.clover_studio.spikachatmodule.utils.MessageSortByCreated;
import com.clover_studio.spikachatmodule.utils.Tools;
import com.clover_studio.spikachatmodule.utils.VCardParser;

import java.util.Collections;
import java.util.List;

/**
 * Created by ubuntu_ivo on 17.07.15..
 */
public class MessageRecyclerViewAdapter extends RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder>{

    private List<Message> data;
    private User myUser;
    private OnLastItemAndOnClickListener lastItemListener;

    public MessageRecyclerViewAdapter (List<Message> data, User myUser){
        this.data = data;
        this.myUser = myUser;
    }

    /**
     * add sent message to adapter
     *
     * @param message sent message
     */
    public void addSentMessage(Message message){
        message.user = myUser;

        data.add(message);
        notifyDataSetChanged();
    }

    /**
     *  add received message to adapter
     *
     * @param message received message
     */
    public void addReceivedMessage(Message message){
        data.add(message);
        notifyDataSetChanged();
    }

    /**
     * replaced delivered message with sent message
     *
     * @param message delivered message
     */
    public void setDeliveredMessage(Message message){
        for(Message item : data){
            if(item.localID != null && item.localID.equals(message.localID)){
                item.status = Const.MessageStatus.DELIVERED;
                item._id = message._id;
                item.created = message.created;
                item.timestampFormatted = "";
                break;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * add messages to adapter
     *
     * @param data messages to add
     */
    public void addMessages (List<Message> data){
        this.data.addAll(data);
        Collections.sort(this.data, new MessageSortByCreated());
        notifyDataSetChanged();
    }

    /**
     * update messages with new given messages
     *
     * @param messagesForUpdate message for update
     */
    public void updateMessages (List<Message> messagesForUpdate){
        for(Message item : data){
            for(Message itemNew : messagesForUpdate){
                if(item._id != null && item._id.equals(itemNew._id)){
                    item.copyMessage(itemNew);
                    continue;
                }
            }
        }
        notifyDataSetChanged();
    }

    public void clearMessages(){
        data.clear();
        notifyDataSetChanged();
    }

    /**
     * set on last item visible and on click listener
     *
     * @param listener
     */
    public void setLastItemListener(OnLastItemAndOnClickListener listener){
        lastItemListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View parentView;

        //my views
        TextView myMessage;
        RelativeLayout myRelative;
        View myMessageStatus;
        ImageView myAvatar;
        ImageView myInfoImage;
        ImageView myImage;
        RelativeLayout myImageLayout;
        RelativeLayout myFile;
        TextView myNameAndTime;

        //you views
        TextView youMessage;
        RelativeLayout youRelative;
        ImageView youAvatar;
        ImageView youInfoImage;
        ImageView youImage;
        RelativeLayout youImageLayout;
        RelativeLayout youFile;
        TextView youNameAndTime;

        ViewHolder(View v) {
            super(v);

            parentView = v.findViewById(R.id.parentView);

            myMessage = (TextView) v.findViewById(R.id.myMessage);
            myRelative = (RelativeLayout) v.findViewById(R.id.myRelativeLayout);
            myMessageStatus = v.findViewById(R.id.myMessageStatus);
            myAvatar = (ImageView) v.findViewById(R.id.myAvatar);
            myImageLayout = (RelativeLayout) v.findViewById(R.id.myImageLayout);
            myImage = (ImageView) v.findViewById(R.id.myImage);
            myInfoImage = (ImageView) v.findViewById(R.id.myImageInfo);
            myFile = (RelativeLayout) v.findViewById(R.id.myFile);
            myNameAndTime = (TextView) v.findViewById(R.id.myNameAndTime);

            youMessage = (TextView) v.findViewById(R.id.youMessage);
            youRelative = (RelativeLayout) v.findViewById(R.id.youRelativeLayout);
            youAvatar = (ImageView) v.findViewById(R.id.youAvatar);
            youImageLayout = (RelativeLayout) v.findViewById(R.id.youImageLayout);
            youImage = (ImageView) v.findViewById(R.id.youImage);
            youInfoImage = (ImageView) v.findViewById(R.id.youImageInfo);
            youFile = (RelativeLayout) v.findViewById(R.id.youFile);
            youNameAndTime = (TextView) v.findViewById(R.id.youNameAndTime);

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Message message = data.get(position);

        //set images to null and hide views
        holder.myAvatar.setImageDrawable(null);
        holder.myImage.setImageDrawable(null);
        holder.youAvatar.setImageDrawable(null);

        holder.myImageLayout.setVisibility(View.GONE);
        holder.youImageLayout.setVisibility(View.GONE);
        holder.myFile.setVisibility(View.GONE);
        holder.youFile.setVisibility(View.GONE);
        holder.myMessage.setVisibility(View.GONE);
        holder.youMessage.setVisibility(View.GONE);
        //*******************************

        TextView messageTV = null;
        ImageView imageIV = null;
        RelativeLayout imageLayout = null;
        ImageView info = null;
        RelativeLayout fileRL = null;

        boolean isMyMessage = isMessageFromUser(message, myUser);

        if(isMyMessage){
            holder.myRelative.setVisibility(View.VISIBLE);
            holder.youRelative.setVisibility(View.GONE);

            messageTV = holder.myMessage;
            imageIV = holder.myImage;
            imageLayout = holder.myImageLayout;
            fileRL = holder.myFile;
        }else{
            holder.myRelative.setVisibility(View.GONE);
            holder.youRelative.setVisibility(View.VISIBLE);

            messageTV = holder.youMessage;
            imageIV = holder.youImage;
            imageLayout = holder.youImageLayout;
            fileRL = holder.youFile;
        }

        if(message.deleted != -1 && message.deleted != 0){
            messageTV.setVisibility(View.VISIBLE);
            messageTV.setText(messageTV.getContext().getString(R.string.message_deleted_at) + " " + Tools.generateDate(Const.DateFormats.USER_JOINED_DATE_FORMAT, message.deleted));
        }else if(message.type == Const.MessageType.TYPE_TEXT){

            messageTV.setVisibility(View.VISIBLE);
            messageTV.setText(message.message);

        }else if(message.type == Const.MessageType.TYPE_NEW_USER){

            messageTV.setVisibility(View.VISIBLE);
            messageTV.setText(message.user.name + " " + messageTV.getContext().getString(R.string.joined_to_conversation));

        }else if(message.type == Const.MessageType.TYPE_USER_LEAVE){

            messageTV.setVisibility(View.VISIBLE);
            messageTV.setText(message.user.name + " " + messageTV.getContext().getString(R.string.left_from_conversation));

        }else if(message.type == Const.MessageType.TYPE_FILE){

            //if is image
            if(Tools.isMimeTypeImage(message.file.file.mimeType)){
                imageLayout.setVisibility(View.VISIBLE);
                ImageLoaderSpice.getInstance(imageIV.getContext()).displayImage(imageIV, Tools.getFileUrlFromId(message.file.thumb.id), 0);

                if(isMyMessage){
                    info = holder.myInfoImage;
                }else{
                    info = holder.youInfoImage;
                }

            }else{
                fileRL.setVisibility(View.VISIBLE);
                showAllChildrenFromLayout(fileRL);

                if(isMyMessage){
                    info = (ImageView) fileRL.getChildAt(Const.ChildrenInFileLayout.INFO);
                }else{
                    info = (ImageView) fileRL.getChildAt(Const.ChildrenInFileLayout.INFO);
                }

                ((TextView)fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_NAME)).setText(message.file.file.name);
                ((TextView)fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_SIZE)).setText(Tools.readableFileSize(Long.valueOf(message.file.file.size)));
                ((TextView)fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_DOWNLOAD)).setText(fileRL.getContext().getResources().getString(R.string.download));

                int drawable = 0;
                if(Tools.isMimeTypeVideo(message.file.file.mimeType)){

                    if(isMyMessage){
                        drawable = R.drawable.ic_video_label_white;
                    }else{
                        drawable = R.drawable.ic_video_label;
                    }
                }else if(Tools.isMimeTypeAudio(message.file.file.mimeType)){

                    if(isMyMessage){
                        drawable = R.drawable.ic_audio_label_white;
                    }else{
                        drawable = R.drawable.ic_audio_label;
                    }
                }else{

                    if(isMyMessage){
                        drawable = R.drawable.ic_file_label_white;
                    }else{
                        drawable = R.drawable.ic_file_label;
                    }
                }
                ((ImageView)fileRL.getChildAt(Const.ChildrenInFileLayout.ICON)).setImageResource(drawable);
            }

        }else if(message.type == Const.MessageType.TYPE_LOCATION){

            fileRL.setVisibility(View.VISIBLE);
            showAllChildrenFromLayout(fileRL);

            if(isMyMessage){
                info = (ImageView) fileRL.getChildAt(Const.ChildrenInFileLayout.INFO);
            }else{
                info = (ImageView) fileRL.getChildAt(Const.ChildrenInFileLayout.INFO);
            }

            int drawable = 0;
            if(isMyMessage){
                drawable = R.drawable.ic_location_label_white;
            }else{
                drawable = R.drawable.ic_location_label;
            }
            ((ImageView)fileRL.getChildAt(Const.ChildrenInFileLayout.ICON)).setImageResource(drawable);
            ((TextView)fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_NAME)).setText(message.message);
            ((TextView)fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_DOWNLOAD)).setText(fileRL.getContext().getResources().getString(R.string.show_location_on_map));
            fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_SIZE).setVisibility(View.GONE);

        }else if(message.type == Const.MessageType.TYPE_CONTACT){

            fileRL.setVisibility(View.VISIBLE);
            showAllChildrenFromLayout(fileRL);

            if(isMyMessage){
                info = (ImageView) fileRL.getChildAt(Const.ChildrenInFileLayout.INFO);
            }else{
                info = (ImageView) fileRL.getChildAt(Const.ChildrenInFileLayout.INFO);
            }

            int drawable = 0;
            if(isMyMessage){
                drawable = R.drawable.ic_contact_label_white;
            }else{
                drawable = R.drawable.ic_contact_label;
            }
            ((ImageView)fileRL.getChildAt(Const.ChildrenInFileLayout.ICON)).setImageResource(drawable);
            String[] contactData = VCardParser.getNameAndFirstPhoneAndFirstEmail(message.message);
            ((TextView) fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_NAME)).setText(contactData[Const.ContactData.NAME]);
            if(contactData[Const.ContactData.EMAIL] != null){
                ((TextView) fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_DOWNLOAD)).setText(contactData[Const.ContactData.EMAIL]);
            }else{
               fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_DOWNLOAD).setVisibility(View.GONE);
            }
            if(contactData[Const.ContactData.PHONE] != null){
                ((TextView) fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_SIZE)).setText(contactData[Const.ContactData.PHONE]);
            }else{
                fileRL.getChildAt(Const.ChildrenInFileLayout.FILE_SIZE).setVisibility(View.GONE);
            }

        }

        if(isMyMessage){
            if(message.status == Const.MessageStatus.SENT){
                holder.myMessageStatus.setBackgroundResource(R.drawable.icon_sent);
            } else if (message.status == Const.MessageStatus.DELIVERED) {
                holder.myMessageStatus.setBackgroundResource(R.drawable.icon_delivered);
            } else {
                holder.myMessageStatus.setBackgroundResource(R.drawable.icon_delivered);
            }
        }

        if(isMyMessage){
            if(position != 0 && isMessageFromUser(data.get(position - 1), myUser)){
                holder.myAvatar.setVisibility(View.GONE);
                holder.myNameAndTime.setVisibility(View.GONE);
            }else{
                holder.myAvatar.setVisibility(View.VISIBLE);
                holder.myNameAndTime.setVisibility(View.VISIBLE);
                ImageLoaderSpice.getInstance(holder.myAvatar.getContext()).displayImage(holder.myAvatar, myUser.avatarURL, 0);

                String name = myUser.name;
                String time = message.getTimeCreated(holder.myNameAndTime.getResources());
                Spannable span = new SpannableString(name + " " +time);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                span.setSpan(new ForegroundColorSpan(holder.myNameAndTime.getResources().getColor(R.color.gray_light_color)), name.length() + 1, span.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                holder.myNameAndTime.setText(span);
            }
        }else {
            if(position != 0 && !isMessageFromUser(data.get(position - 1), myUser)){
                holder.youAvatar.setVisibility(View.INVISIBLE);
                holder.youNameAndTime.setVisibility(View.GONE);
            }else{
                holder.youAvatar.setVisibility(View.VISIBLE);
                holder.youNameAndTime.setVisibility(View.VISIBLE);
                ImageLoaderSpice.getInstance(holder.youAvatar.getContext()).displayImage(holder.youAvatar, message.user.avatarURL, 0);

                String name = message.user.name;
                String time = message.getTimeCreated(holder.myNameAndTime.getResources());
                Spannable span = new SpannableString(name + " " +time);
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                span.setSpan(new ForegroundColorSpan(holder.myNameAndTime.getResources().getColor(R.color.gray_light_color)), name.length() + 1, span.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                holder.youNameAndTime.setText(span);
            }
        }

        holder.parentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(lastItemListener != null){
                    lastItemListener.onClickItem(message);
                }
            }
        });

        // info is null when type is text, user leave or user join
        if(info != null){
            info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lastItemListener != null) {
                        lastItemListener.onInfoClick(message);
                    }
                }
            });
        }

        if(position == 0){
            if(lastItemListener != null){
                lastItemListener.onLastItem();
            }
        }

    }

    /**
     * check is message from user
     * @param message - message with user
     * @param user - user for check
     * @return boolean is message from user
     */
    private boolean isMessageFromUser(Message message, User user){
        String userId = null;
        boolean isMyMessage = false;
        if(message.user != null && message.user.userID != null){
            userId = message.user.userID;
        }else{
            userId = message.userID;
        }

        if(userId.equals(user.userID)){
            isMyMessage = true;
        }

        return isMyMessage;
    }

    /**
     * set visibility of all view from given ViewGroup to visible
     * @param layout
     */
    private void showAllChildrenFromLayout(ViewGroup layout){
        for(int i = 0; i < layout.getChildCount(); i++){
            layout.getChildAt(i).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * listener for last item showed and for click on item
     */
    public interface OnLastItemAndOnClickListener{
        /**
         * triggered when last item show in adapter
         */
        public void onLastItem();

        /**
         * triggered when user click on item
         *
         * @param item data from clicked item
         */
        public void onClickItem(Message item);

        /**
         * triggered when user click on info button
         *
         * @param item data from clicked item
         */
        public void onInfoClick(Message item);
    }

}
