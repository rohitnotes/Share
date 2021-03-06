package com.sharesmile.share.sync;

import android.content.Context;
import android.text.TextUtils;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.onesignal.OneSignal;
import com.sharesmile.share.Events.DBEvent;
import com.sharesmile.share.LeaderBoardDao;
import com.sharesmile.share.MainApplication;
import com.sharesmile.share.MessageDao;
import com.sharesmile.share.Workout;
import com.sharesmile.share.WorkoutDao;
import com.sharesmile.share.core.Constants;
import com.sharesmile.share.gcm.SyncService;
import com.sharesmile.share.gcm.TaskConstants;
import com.sharesmile.share.network.NetworkDataProvider;
import com.sharesmile.share.network.NetworkException;
import com.sharesmile.share.pushNotification.NotificationConsts;
import com.sharesmile.share.rfac.models.LeaderBoardData;
import com.sharesmile.share.rfac.models.LeaderBoardList;
import com.sharesmile.share.rfac.models.RunList;
import com.sharesmile.share.utils.Logger;
import com.sharesmile.share.utils.SharedPrefsManager;
import com.sharesmile.share.utils.Urls;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import Models.CampaignList;
import Models.MessageList;

/**
 * Created by Shine on 20/07/16.
 */
public class SyncHelper {

    private static final String TAG = SyncHelper.class.getSimpleName();

    public static void syncRunData() {
        fetchRunData();
        pushRunData();
    }

    public static void fetchRunData() {
        OneoffTask task = new OneoffTask.Builder()
                .setService(SyncService.class)
                .setTag(TaskConstants.UPDATE_WORKOUT_DATA)
                .setExecutionWindow(0L, 1L)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED).setPersisted(true)
                .build();

        GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(MainApplication.getContext().getApplicationContext());
        mGcmNetworkManager.schedule(task);
    }

    public static void pushRunData() {
        OneoffTask task = new OneoffTask.Builder()
                .setService(SyncService.class)
                .setTag(TaskConstants.UPLOAD_WORKOUT_DATA)
                .setExecutionWindow(0L, 60L)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED).setPersisted(true)
                .build();

        GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(MainApplication.getContext().getApplicationContext());
        mGcmNetworkManager.schedule(task);
    }

    public static int pullRunData() {
        int result = updateWorkoutData(false);
        int flaggedResult = updateWorkoutData(true);

        return (result == GcmNetworkManager.RESULT_RESCHEDULE || flaggedResult == GcmNetworkManager.RESULT_RESCHEDULE) ? GcmNetworkManager.RESULT_RESCHEDULE : GcmNetworkManager.RESULT_SUCCESS;

    }

    public static int updateWorkoutData(boolean fetch_flagged_run) {

        WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
        long workoutCount;
        String runUrl;
        if (fetch_flagged_run) {
            workoutCount = mWorkoutDao.queryBuilder().where(WorkoutDao.Properties.Is_sync.eq(true), WorkoutDao.Properties.IsValidRun.eq(false)).count();
            runUrl = Urls.getFlaggedRunUrl(fetch_flagged_run);
        } else {
            workoutCount = mWorkoutDao.queryBuilder().where(WorkoutDao.Properties.Is_sync.eq(true)).count();
            runUrl = Urls.getFlaggedRunUrl(fetch_flagged_run);
        }
        return updateWorkoutData(runUrl, workoutCount);
    }

    private static int updateWorkoutData(String runUrl, long workoutCount) {

        try {
            RunList runList = NetworkDataProvider.doGetCall(runUrl, RunList.class);
            if (workoutCount >= runList.getTotalRunCount()) {
                Logger.d(TAG, "update success" + workoutCount + " : " + runList.getTotalRunCount());
                EventBus.getDefault().post(new DBEvent.RunDataUpdated());
                updateUserImpact();
                return GcmNetworkManager.RESULT_SUCCESS;
            } else {
                WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
                mWorkoutDao.insertOrReplaceInTx(runList);
                SharedPrefsManager.getInstance().setBoolean(Constants.PREF_HAS_RUN, true);
                Logger.d(TAG, "update success" + runList.toString());
                if (!TextUtils.isEmpty(runList.getNextUrl())) {
                    updateWorkoutData(runList.getNextUrl(), workoutCount);
                } else {
                    updateUserImpact();
                }
            }
        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "update NetworkException" + e.getMessageFromServer() + e.getMessage());
        }
        EventBus.getDefault().post(new DBEvent.RunDataUpdated());

        return 0;
    }

    public static void syncMessageCenterData(Context context) {
        SyncTaskManger.fetchMessageData(context);
    }

    public static boolean fetchMessage() {
        MessageDao messageDao = MainApplication.getInstance().getDbWrapper().getDaoSession().getMessageDao();
        long messageCount = messageDao.queryBuilder().count();
        String url = Urls.getMessageUrl();
        return fetchMessages(url, messageCount);
    }

    private static boolean fetchMessages(String url, long messageCount) {

        try {
            MessageList messageList = NetworkDataProvider.doGetCall(url, MessageList.class);
            if (messageCount >= messageList.getTotalMessageCount()) {
                Logger.d(TAG, "update success" + messageList + " : " + messageList.getTotalMessageCount());
                EventBus.getDefault().post(new DBEvent.MessageDataUpdated());
                return true;
            } else {
                MessageDao messageDao = MainApplication.getInstance().getDbWrapper().getDaoSession().getMessageDao();
                messageDao.insertOrReplaceInTx(messageList);
                SharedPrefsManager.getInstance().setBoolean(Constants.PREF_UNREAD_MESSAGE, true);
                Logger.d(TAG, "Message fetch success" + messageList.toString());
                if (!TextUtils.isEmpty(messageList.getNextUrl())) {
                    return fetchMessages(messageList.getNextUrl(), messageCount);
                }
            }
        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException" + e.getMessageFromServer() + e.getMessage());
            return false;
        }
        EventBus.getDefault().post(new DBEvent.MessageDataUpdated());

        return true;
    }

    // get user total Impact
    public static void updateUserImpact() {
        WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
        List<Workout> list = mWorkoutDao.queryBuilder().where(WorkoutDao.Properties.IsValidRun.eq(true)).list();
        int workoutCount = list.size();
        float totalImpact = 0;
        for (Workout data : list) {
            totalImpact = totalImpact + data.getRunAmount();
        }

        SharedPrefsManager.getInstance().setInt(Constants.PREF_TOTAL_RUN, workoutCount);
        SharedPrefsManager.getInstance().setInt(Constants.PREF_TOTAL_IMPACT, (int) totalImpact);

        OneSignal.sendTag(NotificationConsts.UserTag.RUN_COUNT, String.valueOf(workoutCount));
    }


    public static void syncLeaderBoardData(Context context) {
        SyncTaskManger.fetchLeaderBoardData(context);
    }

    // get leader board for the list
    public static boolean fetchLeaderBoard() {
//        LeaderBoardDao leaderBoardDao = MainApplication.getInstance().getDbWrapper().getDaoSession().getLeaderBoardDao();
        String url = Urls.getLeaderboardUrl();
        return fetchLeaderBoardList(url);

    }


    private static boolean fetchLeaderBoardList(String url) {

        try {
            LeaderBoardDao mLeaderBoardDao = MainApplication.getInstance().getDbWrapper().getLeaderBoardDao();
            LeaderBoardList leaderBoardlist = NetworkDataProvider.doGetCall(url, LeaderBoardList.class);
            LeaderBoardList activeLeaderBoardList = new LeaderBoardList();
            activeLeaderBoardList.setLeaderBoardList(new ArrayList<LeaderBoardData>());

            for (LeaderBoardData data : leaderBoardlist.getLeaderBoardList()) {

                mLeaderBoardDao.insertOrReplaceInTx(data.getLeaderBoardDbObject());
            }
//
//            com.sharesmile.share.LeaderBoardData lb = new com.sharesmile.share.LeaderBoardData();
//            mLeaderBoardDao.insertOrReplace(leaderBoardlist);
            Logger.d(TAG, "leaderboard fetch success" + leaderBoardlist + " : ");
            EventBus.getDefault().post(new DBEvent.LeaderBoardDataUpdated());
            return true;

        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException" + e.getMessageFromServer() + e.getMessage());
            return false;
        }
    }


    public static void syncCampaignData(Context context) {
        SyncTaskManger.startCampaign(context);
    }


    public static void fetchCampaign(Context context) {

        CampaignList.Campaign campaign = null;
        CampaignList.Campaign oldCampaign = SharedPrefsManager.getInstance().getObject(Constants.PREF_CAMPAIGN_DATA, CampaignList.Campaign.class);
        try {
            CampaignList campaignList = NetworkDataProvider.doGetCall(Urls.getCampaignUrl(), CampaignList.class);
            if (campaignList.getTotalCount() > 0) {
                SharedPrefsManager.getInstance().setObject(Constants.PREF_CAMPAIGN_DATA, campaignList.getCampaignList().get(0));
                campaign = campaignList.getCampaignList().get(0);
                Picasso.with(context).load(campaign.getImageUrl()).fetch();
                if (oldCampaign != null && oldCampaign.getId() != campaign.getId()) {
                    SharedPrefsManager.getInstance().setBoolean(Constants.PREF_CAMPAIGN_SHOWN_ONCE, false);
                }
            } else {
                SharedPrefsManager.getInstance().removeKey(Constants.PREF_CAMPAIGN_DATA);
            }

        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException" + e.getMessageFromServer() + e.getMessage());
            campaign = SharedPrefsManager.getInstance().getObject(Constants.PREF_CAMPAIGN_DATA, CampaignList.Campaign.class);
        }
        if (campaign != null) {
            EventBus.getDefault().post(new DBEvent.CampaignDataUpdated(campaign));
        }
    }

}
