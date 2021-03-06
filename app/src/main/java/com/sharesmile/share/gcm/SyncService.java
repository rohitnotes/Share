package com.sharesmile.share.gcm;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sharesmile.share.CustomJSONObject;
import com.sharesmile.share.MainApplication;
import com.sharesmile.share.User;
import com.sharesmile.share.UserDao;
import com.sharesmile.share.Workout;
import com.sharesmile.share.WorkoutDao;
import com.sharesmile.share.analytics.events.AnalyticsEvent;
import com.sharesmile.share.analytics.events.Event;
import com.sharesmile.share.core.Constants;
import com.sharesmile.share.network.NetworkDataProvider;
import com.sharesmile.share.network.NetworkException;
import com.sharesmile.share.rfac.models.Run;
import com.sharesmile.share.sync.SyncHelper;
import com.sharesmile.share.utils.DateUtil;
import com.sharesmile.share.utils.Logger;
import com.sharesmile.share.utils.SharedPrefsManager;
import com.sharesmile.share.utils.Urls;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Created by Shine on 15/05/16.
 */
public class SyncService extends GcmTaskService {
    private static final String TAG = SyncService.class.getSimpleName();

    @Override
    public int onRunTask(TaskParams taskParams) {
        Logger.d(TAG, "runtask started ");
        if (!SharedPrefsManager.getInstance().getBoolean(Constants.PREF_IS_LOGIN, false)) {
            return GcmNetworkManager.RESULT_FAILURE;
        }

        if (taskParams.getTag().equalsIgnoreCase(TaskConstants.UPLOAD_WORKOUT_DATA)) {
            return uploadWorkoutData();
        } else if (taskParams.getTag().equalsIgnoreCase(TaskConstants.UPDATE_WORKOUT_DATA)) {
            //  WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
            //long count = mWorkoutDao.queryBuilder().where(WorkoutDao.Properties.Is_sync.eq(true)).count();
            return SyncHelper.pullRunData();
        } else if (taskParams.getTag().equalsIgnoreCase(TaskConstants.UPLOAD_USER_DATA)) {
            return uploadUserData();
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private int uploadUserData() {

        int user_id = MainApplication.getInstance().getUserID();
        try {

            UserDao mUserDao = MainApplication.getInstance().getDbWrapper().getDaoSession().getUserDao();
            User user;
            List<User> userList = mUserDao.queryBuilder().where(UserDao.Properties.Id.eq(user_id)).list();
            if (userList != null && !userList.isEmpty()) {
                user = userList.get(0);
            } else {
                return GcmNetworkManager.RESULT_FAILURE;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("first_name", user.getName());
            jsonObject.put("gender_user", user.getGender());
            jsonObject.put("phone_number", user.getMobileNO());
            jsonObject.put("birthday", user.getBirthday());
            Logger.d(TAG, jsonObject.toString());

            CustomJSONObject response = NetworkDataProvider.doPutCall(Urls.getUserUrl(user_id), jsonObject, CustomJSONObject.class);

            Logger.d(TAG, response.toString());
            return GcmNetworkManager.RESULT_SUCCESS;

        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException" + e.getMessageFromServer());
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

        int user_id = SharedPrefsManager.getInstance().getInt(Constants.PREF_USER_ID);
        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", user_id);
            jsonObject.put("cause_run_title", workout.getCauseBrief());
            jsonObject.put("distance", workout.getDistance());
            jsonObject.put("start_time", DateUtil.getDefaultFormattedDate(new Date(workout.getBeginTimeStamp())));
            jsonObject.put("end_time", DateUtil.getDefaultFormattedDate(new Date(workout.getEndTimeStamp())));
            jsonObject.put("run_amount", workout.getRunAmount());
            jsonObject.put("run_duration", workout.getElapsedTime());
            jsonObject.put("no_of_steps", workout.getSteps());
            jsonObject.put("avg_speed", workout.getAvgSpeed());
            jsonObject.put("client_run_id", workout.getWorkoutId());
            jsonObject.put("start_location_lat", workout.getStartPointLatitude());
            jsonObject.put("start_location_long", workout.getStartPointLongitude());
            jsonObject.put("end_location_lat", workout.getEndPointLatitude());
            jsonObject.put("end_location_long", workout.getEndPointLongitude());
            Logger.d(TAG, jsonObject.toString());

            Run response = NetworkDataProvider.doPostCall(Urls.getRunUrl(), jsonObject, Run.class);

            WorkoutDao mWorkoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
            //delete row
            mWorkoutDao.delete(workout);
            workout.setId(response.getId());
            workout.setIs_sync(true);
            workout.setIsValidRun(!response.isFlag());
            mWorkoutDao.insertOrReplace(workout);
            AnalyticsEvent.create(Event.ON_RUN_SYNC)
                    .put("upload_result", "success")
                    .put("client_run_id", workout.getWorkoutId())
                    .buildAndDispatch();

            return true;

        } catch (NetworkException e) {
            e.printStackTrace();
            Logger.d(TAG, "NetworkException: " + e.getMessageFromServer());
            Crashlytics.log("Run sync networkException, messageFromServer: " + e.getMessageFromServer());
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
}
