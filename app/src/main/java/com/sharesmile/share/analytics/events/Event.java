package com.sharesmile.share.analytics.events;

/**
 * Created by ankitm on 11/04/16.
 */
public enum Event {

    SESSION_START(AnalyticsEvent.CATEGORY_GENERAL),
    FIRST_LAUNCH_AFTER_INSTALL(AnalyticsEvent.CATEGORY_GENERAL),
    LAUNCH_APP(AnalyticsEvent.CATEGORY_GENERAL),
    ON_DRAWER_OPEN(AnalyticsEvent.CATEGORY_GENERAL),

    ON_LOAD_LOGIN_SCREEN(AnalyticsEvent.CATEGORY_LOGIN),
    ON_CLICK_LOGIN_SKIP(AnalyticsEvent.CATEGORY_LOGIN),
    ON_CLICK_LOGIN_BUTTON(AnalyticsEvent.CATEGORY_LOGIN),
    ON_LOGIN_SUCCESS(AnalyticsEvent.CATEGORY_LOGIN),
    ON_LOGIN_FAILED(AnalyticsEvent.CATEGORY_LOGIN),

    ON_LOAD_CAUSE_SELECTION(AnalyticsEvent.CATEGORY_CAUSE),
    ON_CLICK_LETS_GO(AnalyticsEvent.CATEGORY_CAUSE),
    ON_CLICK_CAUSE_CARD(AnalyticsEvent.CATEGORY_CAUSE),
    ON_LOAD_CAUSE_DETAILS(AnalyticsEvent.CATEGORY_CAUSE),

    ACTIVITY_RCOGNIZED_IN_VEHICLE(AnalyticsEvent.CATEGORY_VIGILANCE),
    ON_USAIN_BOLT_ALERT(AnalyticsEvent.CATEGORY_VIGILANCE),
    DISP_YOU_ARE_DRIVING_NOTIF(AnalyticsEvent.CATEGORY_VIGILANCE),
    DISP_YOU_ARE_STILL_NOTIF(AnalyticsEvent.CATEGORY_VIGILANCE),
    DETECTED_GPS_SPIKE(AnalyticsEvent.CATEGORY_VIGILANCE),

    ON_LOAD_TRACKER_SCREEN(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_CLICK_BEGIN_RUN(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_LOAD_COUNTDOWN_SCREEN(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_SKIP_COUNTDOWN(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_CLICK_RESUME_RUN(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_CLICK_STOP_RUN(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_WORKOUT_START(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_WORKOUT_END(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_WORKOUT_PAUSE(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_WORKOUT_UPDATE(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_WORKOUT_COMPLETE(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_WORKOUT_RESUME(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_LOAD_FINISH_RUN_POPUP(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_LOAD_TOO_SHORT_POPUP(AnalyticsEvent.CATEGORY_WORKOUT),
    ON_RUN_SYNC(AnalyticsEvent.CATEGORY_SYNC),
    ON_START_LOCATION_AFTER_RESUME(AnalyticsEvent.CATEGORY_WORKOUT),


    ON_CLICK_PAUSE_RUN(AnalyticsEvent.CATEGORY_WORKOUT),


    ON_LOAD_SHARE_SCREEN(AnalyticsEvent.CATEGORY_SHARE);

    private String category;

    Event(String category){
        this.category = category;
    }

    public String getCategory(){
        return category;
    }

}

