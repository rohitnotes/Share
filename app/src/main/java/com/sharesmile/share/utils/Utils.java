package com.sharesmile.share.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sharesmile.share.BuildConfig;
import com.sharesmile.share.MainApplication;
import com.sharesmile.share.Workout;
import com.sharesmile.share.core.Constants;
import com.sharesmile.share.gps.activityrecognition.ActivityDetector;
import com.sharesmile.share.rfac.models.Run;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by ankitmaheshwari1 on 08/01/16.
 */
public class Utils {

    private static final String TAG = "Utils";

    /* a utility to validate Indian phone number example - 03498985532, 5389829422 **/
    public static boolean isValidPhoneNumber(String number) {
        if (!TextUtils.isEmpty(number)) {
            return number.matches("^0?(\\d{10})");
        }
        return false;
    }

    public static boolean isCollectionFilled(Collection<?> collection) {
        return null != collection && collection.isEmpty() == false;
    }

    public static boolean compareLists(List<String> list1, List<String> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        Collections.sort(list1);
        Collections.sort(list2);
        for (int index = 0; index < list1.size(); index++) {
            if (list1.get(index).equals(list2.get(index)) == false) {
                return false;
            }
        }
        return true;
    }

    public static String formatToKmsWithOneDecimal(float distanceInMeters){
        DecimalFormat df = new DecimalFormat("0.0");
        df.setGroupingUsed(false);
        return df.format(distanceInMeters / 1000);
    }

    public static String formatWithOneDecimal(float distance){
        DecimalFormat df = new DecimalFormat("0.0");
        df.setGroupingUsed(false);
        return df.format(distance);
    }

    public static String formatIndianCommaSeparated(float value){
        return NumberFormat.getNumberInstance(Locale.ENGLISH).format((int) value);
    }

    /**
     * gets screen height in pixels, Application Context should be used
     */
    public static int getScreenHeightUsingDisplayMetrics(Context context) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }

    /**
     * gets screen width in pixels, Application Context should be used
     */
    public static int getScreenWidthUsingDisplayMetrics(Context context) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    public static float convertDpToPixel(Context context, float dp) {
        Context localContext = context;
        DisplayMetrics displayMetrics = localContext.getResources().getDisplayMetrics();
        return dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static String createJSONStringFromObject(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }

    public static <T> T createObjectFromJSONString(String jsonString, Class<T> clazz)
            throws JsonSyntaxException {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, clazz);

    }

    public static void setStaticGoogleMap(int width, int height, ImageView staticMapView,
                                          List<LatLng> points) {
        if (isCollectionFilled(points) && points.size() >= 2) {

            String staticMapUrl = Constants.STATIC_GOOGLE_MAP_BASE_URL + "size=" + width + "x" + height
                    + Constants.STATIC_GOOGLE_MAP_COMMON_PARAMS
                    + "&scale=" + (isScreenTooLarge(staticMapView.getContext()) ? 2 : 1)
                    + getMarkerParams(points.get(0), points.get(points.size() - 1))
                    + getPathParams(points)
                    + "&key=" + Constants.STATIC_GOOGLE_MAP_API_KEY;
            Logger.i(TAG, "Hitting Static Map API with URL: " + staticMapUrl);
            ShareImageLoader.getInstance().loadImage(staticMapUrl, staticMapView);
        }
    }

    public static boolean isScreenTooLarge(Context context) {
        int screenLayoutWithMask = context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK;
        switch (screenLayoutWithMask) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                return true;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                return true;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return false;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                return false;
        }
        return false;
    }

    public static String getMarkerParams(LatLng startPoint, LatLng endPoint) {
        String firstMarker = "color:blue|label:S|" + startPoint.latitude + "," + startPoint.longitude;
        String secondMarker = "color:red|label:E|" + endPoint.latitude + "," + endPoint.longitude;
        try {
            return "&markers=" + URLEncoder.encode(firstMarker, "UTF-8") + "&markers="
                    + URLEncoder.encode(secondMarker, "UTF-8");
        } catch (UnsupportedEncodingException usee) {
            Logger.e(TAG, usee.getMessage(), usee);
        }
        return "";
    }

    public static String getPathParams(List<LatLng> points) {
        String prefix = "&path=";
        StringBuilder sb = new StringBuilder();
        sb.append("color:0x00ff0080|weight:6");
        for (LatLng point : points) {
            sb.append("|").append(point.latitude).append(",").append(point.longitude);
        }
        try {
            return prefix + URLEncoder.encode(sb.toString(), "UTF-8");
        } catch (UnsupportedEncodingException usee) {
            Logger.e(TAG, usee.getMessage(), usee);
        }
        return "";
    }

    public static final String secondsToHHMMSS(int secs) {

        if (secs >= 3600) {
            int sec = secs % 60;
            int totalMins = secs / 60;
            int hour = totalMins / 60;
            int min = totalMins % 60;
            return String.format("%02d:%02d:%02d", hour, min, sec);
        } else {
            return String.format("%02d:%02d", secs / 60, secs % 60);
        }
    }

    public static final String secondsToHoursAndMins(int secs) {
        if (secs >= 3600) {
            int totalMins = secs / 60;
            int hour = totalMins / 60;
            int min = totalMins % 60;
            return String.format("%d hr %d min", hour, min);
        } else {
            int totalMins = secs / 60;
            return String.format("%d min", totalMins);
        }
    }

    public static String createPrettyJSONStringFromObject(Object object) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(object);
    }

    public static final long stringToSec(String time) {

        String[] timeArray = time.split("[:\\s]");
        int j = 1;
        long sec = 0;
        for (int i = timeArray.length - 1; i >= 0; i--) {
            int duration = Integer.parseInt(timeArray[i]);
            sec = duration * j + sec;
            j = j * 60;

        }
        return sec;
    }

    public static void share(Context context, String shareTemplate) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareTemplate);
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "send"));
    }

    public static void share(Context context, Uri uri, String shareTemplate) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareTemplate);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "send"));
    }


    public static Uri getLocalBitmapUri(Bitmap bmp, Context context) {
        Uri bmpUri = null;
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + new Date().getTime() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            bmpUri = Uri.fromFile(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmpUri;
    }

    public static void redirectToPlayStore(Context context) {
        final String appPackageName = BuildConfig.APPLICATION_ID;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public static void hideKeyboard(View view, Context context) {
        if (view == null || context == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);

        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static Run convertWorkoutToRun(Workout workout){
        Logger.d(TAG, "convertWorkoutToRun");
        Run run = new Run();
        run.setId(workout.getId());
        run.setCauseName(workout.getCauseBrief());
        run.setDistance(workout.getDistance());
        if (workout.getBeginTimeStamp() != null){
            Logger.d(TAG, "BeginTimeStamp is present, will set start_time of run");
            run.setStartTime(DateUtil.getDefaultFormattedDate(new Date(workout.getBeginTimeStamp())));
        }
        if (workout.getEndTimeStamp() != null){
            run.setEndTime(DateUtil.getDefaultFormattedDate(new Date(workout.getEndTimeStamp())));
        }
        run.setRunAmount(workout.getRunAmount() == null ? 0 : workout.getRunAmount());
        run.setRunDuration(workout.getElapsedTime());
        run.setNumSteps(workout.getSteps() == null ? 0 : workout.getSteps());
        run.setAvgSpeed(workout.getAvgSpeed());
        run.setClientRunId(workout.getWorkoutId());
        if (workout.getStartPointLatitude() != null){
            run.setStartLocationLat(workout.getStartPointLatitude());
        }
        if (workout.getStartPointLongitude() != null){
            run.setStartLocationLong(workout.getStartPointLongitude());
        }
        if (workout.getEndPointLatitude() != null){
            run.setEndLocationLat(workout.getEndPointLatitude());
        }
        if (workout.getEndPointLongitude() != null){
            run.setEndLocationLong(workout.getEndPointLongitude());
        }
        run.setIsFlag(!workout.getIsValidRun());

        return run;
    }

    /**
     * Calculates DeltaCalories as per METS formula
     * @param deltaTimeMillis time interval in millis in which this distance is covered
     * @param deltaSpeed speed in m/s during which the distance was covered
     * @return Kcal calculated using METS formula
     */
    public static double getDeltaCaloriesMets(long deltaTimeMillis, float deltaSpeed){
        double mets = getMetsValue(deltaSpeed);
        float bodyWeightKgs = MainApplication.getInstance().getUserDetails().getBodyWeight();
        // TODO: CaloriesCalculation Put a check for 0 bodyweight over here
        return mets * bodyWeightKgs * ( ((double)(deltaTimeMillis)) / (1000*60*60) );
    }

    public static double getMetsValue(float deltaSpeed){
        double mph = 2.23694*deltaSpeed;

        // Referring Compendium of Physical Activities over here
        // https://sites.google.com/site/compendiumofphysicalactivities/Activity-Categories/walking
        // and here
        // https://sites.google.com/site/compendiumofphysicalactivities/Activity-Categories/running

        if (mph <= 0) {
            return 0;
        }else if (mph <= 1){
            return 1.3;
        }else if (mph <= 2){
            return (1.3 + 1.5*(mph - 1)); // 2.8 at 2 mph
        }else if (mph <= 2.5){
            return 2.8 + 0.4*(mph - 2); // 3.0 at 2.5 mph
        }else if (mph <= 3.5){
            return 3.0 + 1.3*(mph - 2.5); // 4.3 at 3.5 mph
        }else if (mph <= 4){
            if (ActivityDetector.getInstance().getRunningConfidence()
                    >= ActivityDetector.getInstance().getWalkingConfidence()){
                // User is running
                return 4.5 + 3*(mph - 3.5); // 6.0 at 4 mph
            }else {
                // User is Walking
                return 4.3 + 1.4*(mph - 3.5); // 5.0 at 4 mph
            }
        }
        /// All walking values uptill here, range 4-5 mph is ambiguous range need to decide between running and walking
        else if (mph <= 5){
            if (ActivityDetector.getInstance().getRunningConfidence()
                    >= ActivityDetector.getInstance().getWalkingConfidence()){
                // User is running
                return 6 + 2.3*(mph - 4); // 8.3 at 5 mph
            }else {
                // User is Walking
                if (mph <= 4.5){
                    // 4 - 4.5 mph
                    return 5 + 4*(mph - 4); // 7.0 at 4.5 mph
                }else {
                    // 4.5 - 5 mph
                    return 7 + 2.6*(mph - 4.5); // 8.3 at 5 mph
                }
            }
        }else if (mph <= 6){
            return 8.3 + 1.5*(mph - 5); // 9.8 at 6 mph
        }else if (mph <= 7){
            return 9.8 + 1.2*(mph - 6); // 11.0 at 7mph
        }else if (mph <= 7.7){
            return 11 + 1.143*(mph - 7); // 11.8 at 7.7 mph
        }else if (mph <= 9){
            return 11.8 + 0.77*(mph - 7.7); // 12.8 at 9 mph
        }else if (mph <= 10){
            return 12.8 + 1.7*(mph - 9); // 14.5 at 10 mph
        }else if (mph <= 11){
            return 14.5 + 1.5*(mph - 10); // 16 at 11 mph
        }else if (mph <= 12){
            return 16 + 3*(mph - 11); // 19 at 12 mph
        }else if (mph <= 14){
            return 19 + 2*(mph - 12); // 23 at 14 mph
        }else if (mph <= 15){
            return 23 + 1*(mph - 14); // 24 at 15 mph
        }else {
            // For speeds greater than 15 mph (23 kmph) we assume that the person is driving so we don't add calories
            return 1.3;
        }
    }

    public static double getDeltaCaloriesKarkanen(long deltaTimeMillis, float deltaSpeed){
        double mph = 2.23694*deltaSpeed;
        float bodyWeightKgs = MainApplication.getInstance().getUserDetails().getBodyWeight();
        if (bodyWeightKgs == 0){
            return 0;
        }
        double bodyWeightLbs = 2.205*bodyWeightKgs;
        double mins = ((double) deltaTimeMillis) / (1000 * 60);

        if (mph <= 0){
            return 0;
        }else if (mph <= 1){
            // METS formula for the case when speed is extremely low
            return 1.3 * bodyWeightKgs * (mins / 60);
        }else if (mph <= 3){
            return bodyWeightLbs * mins * karkanenCalorieRateForWalking(mph, bodyWeightLbs);
        }else if (mph <= 5){
            // Range 3-5 mph is ambiguous range need to decide between running and walking
            if (ActivityDetector.getInstance().getRunningConfidence()
                    >= ActivityDetector.getInstance().getWalkingConfidence()){
                // User is Running
                return bodyWeightLbs * mins * karkanenCalorieRateForRunning(mph, bodyWeightLbs);
            }else {
                // User is Walking
                return bodyWeightLbs * mins * karkanenCalorieRateForWalking(mph, bodyWeightLbs);
            }
        }else if (mph <= 14){
            // User is assumed to be running
            return bodyWeightLbs * mins * karkanenCalorieRateForRunning(mph, bodyWeightLbs);
        }else {
            // Too fast, user must be in a vehicle
            // Hence METS formula for calculating calories burned for a static user
            return 1.3 * bodyWeightKgs * (mins / 60);
        }

    }

    /**
     * Calculates Karkanen rate of burning calories per lb per min for a Walking user
     * @param speed in mph
     * @param weightInLbs in lbs
     * @return Returns Kcal/lb-min
     */
    public static double karkanenCalorieRateForWalking(double speed, double weightInLbs){
        double a = 0.0195;
        double b = (-1)*0.00436;
        double c = 0.00245;
        double d = ( 0.000801*Math.pow(weightInLbs/154, 0.425) ) / weightInLbs;
        return a + b * speed + c * Math.pow(speed, 2) + d * Math.pow(speed, 3);
    }

    /**
     * Calculates Karkanen rate of burning calories per lb per min for a Running user
     * @param speed in mph
     * @param weightInLbs in lbs
     * @return Returns Kcal/lb-min
     */
    public static double karkanenCalorieRateForRunning(double speed, double weightInLbs){
        double a = 0.0395;
        double b = 0.00327;
        double c = 0.000455;
        double d = ( 0.00801*Math.pow(weightInLbs/154, 0.425) ) / weightInLbs;
        return a + b * speed + c * Math.pow(speed, 2) + d * Math.pow(speed, 3);
    }

}
