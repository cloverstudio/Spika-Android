package com.clover_studio.spikachatmodule.models;

import android.content.res.Resources;
import android.text.TextUtils;

import com.clover_studio.spikachatmodule.R;
import com.clover_studio.spikachatmodule.base.BaseModel;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by ubuntu_ivo on 17.07.15..
 */
public class Message extends BaseModel {

    public String _id;
    public String userID;
    public String roomID;
    public User user;
    public int type;
    public String roomId;
    public String message;
    public long created;
    public FileModel file;
    public String localID;
    public LocationModel location;
    public List<SeenByModel> seenBy;
    public long deleted = -1;

    public int status;
    public String timestampFormatted;

    public void copyMessage(Message message){
        _id = message._id;
        userID = message.userID;
        roomID = message.roomID;
        user = message.user;
        type = message.type;
        roomId = message.roomId;
        this.message = message.message;
        created = message.created;
        file = message.file;
        localID = message.localID;
        location = message.location;
        seenBy = message.seenBy;
        deleted = message.deleted;
        status = message.status;
    }

    @Override
    public String toString() {
        return "Message{" +
                "_id='" + _id + '\'' +
                ", userID='" + userID + '\'' +
                ", roomID='" + roomID + '\'' +
                ", user=" + user +
                ", type=" + type +
                ", roomId='" + roomId + '\'' +
                ", message='" + message + '\'' +
                ", created=" + created +
                ", file=" + file +
                ", localID='" + localID + '\'' +
                ", location=" + location +
                ", seenBy=" + seenBy +
                ", status=" + status +
                '}';
    }

    public String getTimeCreated(Resources res){
        if(!TextUtils.isEmpty(timestampFormatted)){
            return timestampFormatted;
        }else{
            timestampFormatted = formatTime(created, res);
            return timestampFormatted;
        }
    }

    private String formatTime(long time, Resources res) {
        long currentTime = System.currentTimeMillis();

        long currentTimeDay = currentTime / 86400000;
        long timeDay = time / 86400000;
        if (currentTimeDay == timeDay) {
            return justTime(time);
        } else {
            return timeWithDate(time);
        }
    }

    private String justTime(long time){
        try {

            Timestamp stamp = new Timestamp(time);
            Date date = new Date(stamp.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private String timeWithDate(long time){
        try {

            Timestamp stamp = new Timestamp(time);
            Date date = new Date(stamp.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
