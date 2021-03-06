package com.sharesmile.share.rfac.models;

import com.google.gson.annotations.SerializedName;
import com.sharesmile.share.core.UnObfuscable;
import com.sharesmile.share.LeaderBoard;

import java.io.Serializable;
import java.util.List;

/**
 * Created by piyush on 9/6/16.
 */
public class LeaderBoardData implements UnObfuscable, Serializable{

    @SerializedName("user_id")
    private long userid;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("social_thumb")
    private String imageUrl;

    @SerializedName("last_week_distance")
    private LastWeekDistance lastWeekDistance;

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }



    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getImageUrl() {
        return imageUrl;
    }

    public float getLastWeekDistance() {
        return lastWeekDistance.getLastWeekDistance();
    }

    public long getUserid() {
        return userid;
    }

    public com.sharesmile.share.LeaderBoard getLeaderBoardDbObject()
    {
        com.sharesmile.share.LeaderBoard lb = new com.sharesmile.share.LeaderBoard((long) getUserid(), getFirstName(), getLastName(), getImageUrl(), getLastWeekDistance());
        return lb;
    }

    //    public float getLastWeekDistance() {
//        return lastWeekDistance;
//    }

}
