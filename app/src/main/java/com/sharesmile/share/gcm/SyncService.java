package com.sharesmile.share.gcm;

import android.os.Bundle;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.google.gson.Gson;
import com.sharesmile.share.Events.DBEvent;
import com.sharesmile.share.Events.GlobalLeaderBoardDataUpdated;
import com.sharesmile.share.Events.LeagueBoardDataUpdated;
import com.sharesmile.share.Events.TeamLeaderBoardDataFetched;
import com.sharesmile.share.LeaderBoardDataStore;
import com.sharesmile.share.MainApplication;
import com.sharesmile.share.Workout;
import com.sharesmile.share.WorkoutDao;
import com.sharesmile.share.analytics.events.AnalyticsEvent;
import com.sharesmile.share.analytics.events.Event;
import com.sharesmile.share.core.Constants;
import com.sharesmile.share.network.NetworkDataProvider;
import com.sharesmile.share.network.NetworkException;
import com.sharesmile.share.network.NetworkUtils;
import com.sharesmile.share.rfac.models.CauseList;
import com.sharesmile.share.rfac.models.FraudData;
import com.sharesmile.share.rfac.models.LeaderBoardList;
import com.sharesmile.share.rfac.models.Run;
import com.sharesmile.share.rfac.models.RunList;
import com.sharesmile.share.rfac.models.UserDetails;
import com.sharesmile.share.sync.SyncHelper;
import com.sharesmile.share.utils.DateUtil;
import com.sharesmile.share.utils.Logger;
import com.sharesmile.share.utils.ServerTimeKeeper;
import com.sharesmile.share.utils.SharedPrefsManager;
import com.sharesmile.share.utils.Urls;
import com.sharesmile.share.utils.Utils;
import com.squareup.okhttp.Response;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Models.TeamBoard;
import Models.TeamLeaderBoard;

/**
 * Created by Shine on 15/05/16.
 */
public class SyncService extends GcmTaskService {
    private static final String TAG = SyncService.class.getSimpleName();

    @Override
    public int onRunTask(TaskParams taskParams) {
        Logger.d(TAG, "runtask started: " + taskParams.getTag());
        if (!SharedPrefsManager.getInstance().getBoolean(Constants.PREF_IS_LOGIN, false)) {
            return GcmNetworkManager.RESULT_FAILURE;
        }
        if (taskParams.getTag().equalsIgnoreCase(TaskConstants.PUSH_WORKOUT_DATA)) {
            return uploadWorkoutData();
        } else if (taskParams.getTag().equalsIgnoreCase(TaskConstants.FORCE_REFRESH_ENTIRE_WORKOUT_HISTORY)) {
            return forceRefreshEntireWorkoutHistory();
        } else if (taskParams.getTag().equalsIgnoreCase(TaskConstants.UPLOAD_USER_DATA)) {
            return uploadUserData();
        } else if (taskParams.getTag().equalsIgnoreCase(TaskConstants.SYNC_DATA)){
            return syncData();
        }
        else if (taskParams.getTag().equalsIgnoreCase(TaskConstants.PUSH_FRAUD_DATA)) {
            Bundle extras = taskParams.getExtras();
            String fraudDataString = extras.getString(TaskConstants.FRAUD_DATA_JSON);
            pushFraudData(fraudDataString);
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
        MainApplication.getInstance().startSyncTasks();
    }

    private int syncData(){

        syncServerTime() ;
        syncLeaderBoardData();
        updateCauseData();
        syncWorkoutData();

        // Returning success as result does not matter
        return GcmNetworkManager.RESULT_SUCCESS;
    }


    public static int syncServerTime(){
        ServerTimeKeeper.getInstance().syncTimerWithServerTime();
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    public static int syncLeaderBoardData(){
        Logger.d(TAG, "syncLeaderBoardData");

        if (!MainApplication.isLogin()){
            return GcmNetworkManager.RESULT_FAILURE;
        }

        int result = GcmNetworkManager.RESULT_SUCCESS;

        try {
            Logger.d(TAG, "Will sync GlobalLeaderBoard");
            LeaderBoardList leaderBoardList = NetworkDataProvider.doGetCall(Urls.getLeaderboardUrl(), LeaderBoardList.class);
            // Store this in LeaderBoardDataStore
            LeaderBoardDataStore.getInstance().setGlobalLeaderBoardData(leaderBoardList);
            // Notify LeaderBoardFragment about it
            EventBus.getDefault().post(new GlobalLeaderBoardDataUpdated(true));
        } catch (NetworkException e) {
            Logger.e(TAG, "Exception occurred while syncing GlobalLeaderBoardData data from network: " + e);
            e.printStackTrace();
            result = GcmNetworkManager.RESULT_FAILURE;
        }

        if (LeaderBoardDataStore.getInstance().getLeagueBoard() != null
                && LeaderBoardDataStore.getInstance().toShowLeague()){
            // Go for sync only when an active league is present and is still visible to team members
            try {
                Logger.d(TAG, "Will sync LeagueBoard");
                TeamBoard leagueBoard = NetworkDataProvider.doGetCall(Urls.getTeamBoardUrl(), TeamBoard.class);
                // Store this in LeaderBoardDataStore
                LeaderBoardDataStore.getInstance().setLeagueBoardData(leagueBoard);
                // Notify LeaderBoardFragment about it
                EventBus.getDefault().post(new LeagueBoardDataUpdated(true));
            } catch (NetworkException e) {
                Logger.e(TAG, "Exception occurred while syncing LeagueBoard data from network: " + e);
                e.printStackTrace();
                result = GcmNetworkManager.RESULT_FAILURE;
            }

            try {
                Logger.d(TAG, "Will sync MyTeamLeaderBoard");
                int leagueTeamId = LeaderBoardDataStore.getInstance().getMyTeamId();
                if (leagueTeamId > 0){
                    Map<String, String> queryParams = new HashMap<>();
                    queryParams.put("team_id", String.valueOf(leagueTeamId));
                    TeamLeaderBoard myTeamLeaderBoard = NetworkDataProvider
                            .doGetCall(Urls.getTeamLeaderBoardUrl(), queryParams, TeamLeaderBoard.class);
                    // Store this in LeaderBoardDataStore
                    LeaderBoardDataStore.getInstance().setMyTeamLeaderBoardData(myTeamLeaderBoard);
                    // Notify LeaderBoardFragment about it
                    EventBus.getDefault().post(new TeamLeaderBoardDataFetched(leagueTeamId, true, myTeamLeaderBoard));
                }
            } catch (NetworkException e) {
                Logger.e(TAG, "Exception occurred while syncing MyTeamLeaderBoardData from network: " + e);
                e.printStackTrace();
                result = GcmNetworkManager.RESULT_FAILURE;
            }

        }

        return result;

    }

    public static int updateCauseData() {
        Logger.d(TAG, "updateCauseData");
        try {
            CauseList causeList = NetworkDataProvider.doGetCall(Urls.getCauseListUrl(), CauseList.class);
            MainApplication.getInstance().updateCauseList(causeList);
            EventBus.getDefault().post(new DBEvent.CauseDataUpdated(causeList));
            return GcmNetworkManager.RESULT_SUCCESS;
        } catch (NetworkException e) {
            Logger.e(TAG, "Exception occurred while fetching updated cause list from network");
            e.printStackTrace();
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }


    private int syncWorkoutData(){
        WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
        long clientVersion = SharedPrefsManager.getInstance().getLong(Constants.PREF_WORKOUT_DATA_SYNC_VERSION);
        boolean isWorkoutDataUpToDate = SharedPrefsManager.getInstance().getBoolean(Constants.PREF_IS_WORKOUT_DATA_UP_TO_DATE_IN_DB, false);
        if (isWorkoutDataUpToDate && clientVersion > 0){
            String syncUrl;
            syncUrl = Urls.getSyncRunUrl(clientVersion);
            Logger.d(TAG, "Starting sync with client_version: " + clientVersion);
            syncWorkoutTimeStamp = 0;
            return syncWorkoutData(syncUrl, mWorkoutDao);
        }else {
            // Need to force refresh Workout Data
            Logger.e(TAG, "Must fetch historical runs before");
            SyncHelper.forceRefreshEntireWorkoutHistory();
            return  GcmNetworkManager.RESULT_FAILURE;
        }
    }

    private long syncWorkoutTimeStamp;

    private int syncWorkoutData(String syncUrl, WorkoutDao mWorkoutDao){
        try {
            Response response = NetworkDataProvider.getResponseForGetCall(syncUrl);
            if (syncWorkoutTimeStamp == 0){
                if (response.headers().getDate("Date") != null){
                    syncWorkoutTimeStamp = response.headers().getDate("Date").getTime();
                }else {
                    syncWorkoutTimeStamp = DateUtil.getServerTimeInMillis();
                }
            }
            RunList runList = NetworkUtils.handleResponse(response, RunList.class);
            Gson gson = new Gson();
            Logger.d(TAG, "Syncing these runs in DB " + gson.toJson(runList));
            mWorkoutDao.insertOrReplaceInTx(runList);
            // Update User's track record
            Utils.updateTrackRecordFromDb();

            if (!TextUtils.isEmpty(runList.getNextUrl())) {
                // Recursive call to fetch the runs of next page
                return syncWorkoutData(runList.getNextUrl(), mWorkoutDao);
            } else {
                Logger.d(TAG, "syncWorkoutData, Setting SyncedTimeStampMillis as: " + syncWorkoutTimeStamp);
                SharedPrefsManager.getInstance().setLong(Constants.PREF_WORKOUT_DATA_SYNC_VERSION,
                        (syncWorkoutTimeStamp / 1000));
                return GcmNetworkManager.RESULT_SUCCESS;
            }
        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException while syncing runs with URL: "+syncUrl+", Exception: " + e);
            return GcmNetworkManager.RESULT_RESCHEDULE;
        }
    }

    private int pushFraudData(String fraudDataString){
        if (TextUtils.isEmpty(fraudDataString)){
            Logger.d(TAG, "Can't push FraudDtaString in TaskParams is empty");
            return GcmNetworkManager.RESULT_FAILURE;
        }
        Logger.d(TAG, "Will pushFraudData: " + fraudDataString);
        try {
            Gson gson = new Gson();
            FraudData fraudData = gson.fromJson(fraudDataString, FraudData.class);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", fraudData.getUserId());
            jsonObject.put("client_run_id", fraudData.getClientRunId());
            jsonObject.put("cause_id", fraudData.getCauseId());
            jsonObject.put("usain_bolt_count", fraudData.getUsainBoltCount());
            // Send team_id only when it is greater than 0
            if (fraudData.getTeamId() > 0){
                jsonObject.put("team_id", fraudData.getTeamId());
            }
            jsonObject.put("timestamp", fraudData.getTimeStamp());
            jsonObject.put("mock_location_used", fraudData.isMockLocationUsed());

            NetworkDataProvider.doPostCall(Urls.getFraudstersUrl(), jsonObject, FraudData.class);

            return GcmNetworkManager.RESULT_SUCCESS;

        }catch (JSONException e){
            e.printStackTrace();
            Logger.d(TAG, "JSONException: " + e.getMessage());
            Crashlytics.logException(e);
            return GcmNetworkManager.RESULT_RESCHEDULE;
        }catch (NetworkException ne){
            ne.printStackTrace();
            Logger.d(TAG, "NetworkException: " + ne);
            String log= "Couldn't post fraudData to URL: " + Urls.getFraudstersUrl() + ", FraudData: " + fraudDataString;
            Logger.e(TAG, log);
            Crashlytics.log(log);
            Crashlytics.log("Push fraud data networkException, messageFromServer: " + ne);
            Crashlytics.logException(ne);
            return GcmNetworkManager.RESULT_RESCHEDULE;
        }
    }


    private int uploadUserData() {
        int user_id = MainApplication.getInstance().getUserID();
        Logger.d(TAG, "uploadUserData for userId: " + user_id );
        try {

            UserDetails prev = MainApplication.getInstance().getUserDetails();
            if (prev == null){
                Logger.d(TAG, "Can't UPLOAD, UserDetails not present");
                return GcmNetworkManager.RESULT_FAILURE;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("first_name", prev.getFirstName());
            jsonObject.put("gender_user", prev.getGenderUser());
            jsonObject.put("phone_number", prev.getPhoneNumber());
            jsonObject.put("body_weight", prev.getBodyWeight());
            jsonObject.put("body_height", prev.getBodyHeight());
            jsonObject.put("user_id", user_id);

            Logger.d(TAG, "Syncing user with data " + jsonObject.toString());

            Gson gson = new Gson();
            UserDetails response = NetworkDataProvider.doPutCall(Urls.getUserUrl(user_id), jsonObject, UserDetails.class);
            Logger.d(TAG, "Response for getUser:" + gson.toJson(response));

            MainApplication.getInstance().setUserDetails(response);

            return GcmNetworkManager.RESULT_SUCCESS;

        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException" + e);
            return GcmNetworkManager.RESULT_FAILURE;
        } catch (JSONException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException");
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }

    private int uploadWorkoutData() {

        WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
        List<Workout> mWorkoutList = mWorkoutDao.queryBuilder().where(WorkoutDao.Properties.Is_sync.eq(false)).list();

        if (mWorkoutList != null && mWorkoutList.size() > 0) {

            boolean isSuccess = true;
            for (Workout workout : mWorkoutList) {
                isSuccess = isSuccess && uploadWorkoutData(workout);
            }
            return isSuccess ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_RESCHEDULE;
        } else {
            return GcmNetworkManager.RESULT_SUCCESS;
        }

    }

    private boolean uploadWorkoutData(Workout workout) {

        Logger.d(TAG, "uploadWorkoutData called for client_run_id: " + workout.getWorkoutId());
        int user_id = SharedPrefsManager.getInstance().getInt(Constants.PREF_USER_ID);
        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", user_id);
            jsonObject.put("cause_run_title", workout.getCauseBrief());
            jsonObject.put("distance", workout.getDistance());

            if (workout.getBeginTimeStamp() != null){
                jsonObject.put("start_time", DateUtil.getDefaultFormattedDate(new Date(workout.getBeginTimeStamp())));
                jsonObject.put("start_time_epoch", workout.getBeginTimeStamp());
            }else if (workout.getDate() != null){
                jsonObject.put("start_time", DateUtil.getDefaultFormattedDate(workout.getDate()));
                jsonObject.put("start_time_epoch", workout.getDate().getTime());
            }

            if (workout.getEndTimeStamp() != null){
                jsonObject.put("end_time", DateUtil.getDefaultFormattedDate(new Date(workout.getEndTimeStamp())));
                jsonObject.put("end_time_epoch", workout.getEndTimeStamp());
            }
            jsonObject.put("run_amount", workout.getRunAmount());
            jsonObject.put("run_duration", workout.getElapsedTime());
            jsonObject.put("run_duration_epoch", Utils.hhmmssToSecs(workout.getElapsedTime()));
            jsonObject.put("no_of_steps", workout.getSteps());
            jsonObject.put("avg_speed", workout.getAvgSpeed());
            jsonObject.put("client_run_id", workout.getWorkoutId());
            jsonObject.put("start_location_lat", workout.getStartPointLatitude());
            jsonObject.put("start_location_long", workout.getStartPointLongitude());
            jsonObject.put("end_location_lat", workout.getEndPointLatitude());
            jsonObject.put("end_location_long", workout.getEndPointLongitude());
            jsonObject.put("version", workout.getVersion());
            jsonObject.put("calories_burnt", workout.getCalories() == null ? 0 : workout.getCalories());
            if (workout.getTeamId() != null && workout.getTeamId() > 0){
                jsonObject.put("team_id", workout.getTeamId());
            }
            jsonObject.put("num_spikes", workout.getNumSpikes());

            Logger.d(TAG, "Will upload run: "+jsonObject.toString());

            Run response = NetworkDataProvider.doPostCall(Urls.getRunUrl(), jsonObject, Run.class);

            WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
            //delete row
            mWorkoutDao.delete(workout);
            workout.setId(response.getId());
            workout.setIs_sync(true);
            workout.setIsValidRun(!response.isFlag());
            workout.setVersion(response.getVersion());
            mWorkoutDao.insertOrReplace(workout);
            AnalyticsEvent.create(Event.ON_RUN_SYNC)
                    .put("upload_result", "success")
                    .put("client_run_id", workout.getWorkoutId())
                    .buildAndDispatch();

            return true;

        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException: " + e);
            Crashlytics.log("Run sync networkException, messageFromServer: " + e);
            Crashlytics.logException(e);
            AnalyticsEvent.create(Event.ON_RUN_SYNC)
                    .put("upload_result", "failure")
                    .put("client_run_id", workout.getWorkoutId())
                    .put("exception_message", e.getMessage())
                    .put("message_from_server", e.getMessageFromServer())
                    .put("http_status", e.getHttpStatusCode())
                    .buildAndDispatch();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            Logger.d(TAG, "JSONException");
            Crashlytics.log("Run sync JSONException");
            Crashlytics.logException(e);
            AnalyticsEvent.create(Event.ON_RUN_SYNC)
                    .put("upload_result", "JSONException ")
                    .buildAndDispatch();
            return false;
        }
    }

    public int forceRefreshEntireWorkoutHistory() {
        Logger.d(TAG, "forceRefreshEntireWorkoutHistory");
        String runUrl;
        runUrl = Urls.getFlaggedRunUrl(false);
        refreshAllTimeStamp = 0;
        return forceRefreshAllWorkoutData(runUrl);
    }

    private long refreshAllTimeStamp;

    private int forceRefreshAllWorkoutData(String runUrl) {

        try {
            Response response = NetworkDataProvider.getResponseForGetCall(runUrl);
            if (refreshAllTimeStamp == 0){
                if (response.headers().getDate("Date") != null){
                    refreshAllTimeStamp = response.headers().getDate("Date").getTime();
                }else {
                    refreshAllTimeStamp = DateUtil.getServerTimeInMillis();
                }
            }
            Logger.d(TAG, "forceRefreshAllWorkoutData, fetched SyncedTimeStampMillis as: " + refreshAllTimeStamp);
            RunList runList = NetworkUtils.handleResponse(response, RunList.class);

            Gson gson = new Gson();
            Logger.d(TAG, "Updating these runs in DB " + gson.toJson(runList));
            WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
            mWorkoutDao.insertOrReplaceInTx(runList);
            SharedPrefsManager.getInstance().setBoolean(Constants.PREF_HAS_RUN, true);
            if (!TextUtils.isEmpty(runList.getNextUrl())) {
                // Recursive call to fetch the runs of next page
                return forceRefreshAllWorkoutData(runList.getNextUrl());
            } else {
                // All the runs are pulled from server and written into DB
                Logger.d(TAG, "forceRefreshAllWorkoutData, Setting SyncedTimeStampMillis as: " + refreshAllTimeStamp);
                SharedPrefsManager.getInstance().setLong(Constants.PREF_WORKOUT_DATA_SYNC_VERSION, (refreshAllTimeStamp / 1000));
                SharedPrefsManager.getInstance().setBoolean(Constants.PREF_IS_WORKOUT_DATA_UP_TO_DATE_IN_DB, true);
                Utils.updateTrackRecordFromDb();
                EventBus.getDefault().post(new DBEvent.RunDataUpdated());
                return GcmNetworkManager.RESULT_SUCCESS;
            }
        } catch (NetworkException e) {
            Logger.d(TAG, "NetworkException while force refreshing all workoutData from server: " + e);
            e.printStackTrace();
            return GcmNetworkManager.RESULT_RESCHEDULE;
        }

    }

}
