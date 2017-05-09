package com.sharesmile.share.gps.models;

import android.location.Location;
import android.os.Build;

import com.sharesmile.share.core.Config;
import com.sharesmile.share.core.UnObfuscable;
import com.sharesmile.share.utils.DateUtil;

import java.io.Serializable;

/**
 * Created by ankitmaheshwari1 on 21/02/16.
 */
public class DistRecord implements UnObfuscable, Serializable{

    private static final String TAG = "DistRecord";

    private Location location;
    private Location prevLocation;
    private float dist; // in meters
    private long interval; // in millis
    private float speed; // in m/s
    private float bearing; // bearing of location in degrees, irrespective of the prevLocation


    public DistRecord(Location location){
        this.location = location;
    }

    public DistRecord(Location location, Location prevLocation, float dist){
        this.location = location;
        this.prevLocation = prevLocation;
        bearing = location.getBearing();
        // distanceTo() method, though not blocking but is very computationally intensive
        this.dist = dist;
        interval = getElapsedTimeMs();
        speed = (dist * 1000) / getElapsedTimeMs();
    }

    private long getElapsedTimeMs(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            return (location.getElapsedRealtimeNanos() - prevLocation.getElapsedRealtimeNanos()) / 1000000;
        }else{
            return (location.getTime() - prevLocation.getTime());
        }
    }

    @Override
    public String toString() {
        return "DistRecord{" +
                "  location=" + location +
                ", prevLocation=" + prevLocation +
                ", dist=" + dist +
                ", interval=" + interval +
                ", speed=" + speed +
                ", bearing=" + bearing +
                '}';
    }

    public boolean isFirstRecordAfterResume(){
        if (location != null && prevLocation == null){
            return true;
        }
        return false;
    }

    public Location getLocation() {
        return location;
    }

    public long getTimeStamp(){
        return location.getTime();
    }

    public boolean isTooOld(){
        return (DateUtil.getServerTimeInMillis() - getTimeStamp() > Config.CURRENT_SPEED_VALIDITY_THRESHOLD_INTERVAL);
    }

    public float getDist() {
        return dist;
    }

    /**
     * @return interval in millis between two geolocation points in this DistRecord
     */
    public long getInterval() {
        return interval;
    }

    public float getSpeed() {
        return speed;
    }

    public float getBearing() {
        return bearing;
    }

    public Location getPrevLocation() {
        return prevLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistRecord that = (DistRecord) o;

        if (Float.compare(that.dist, dist) != 0) return false;
        if (interval != that.interval) return false;
        if (!location.equals(that.location)) return false;
        return prevLocation != null ? prevLocation.equals(that.prevLocation) : that.prevLocation == null;

    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + (prevLocation != null ? prevLocation.hashCode() : 0);
        result = 31 * result + (dist != +0.0f ? Float.floatToIntBits(dist) : 0);
        result = 31 * result + (int) (interval ^ (interval >>> 32));
        return result;
    }
}
