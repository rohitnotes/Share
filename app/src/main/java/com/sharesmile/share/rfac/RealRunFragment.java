package com.sharesmile.share.rfac;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sharesmile.share.MainApplication;
import com.sharesmile.share.R;
import com.sharesmile.share.Workout;
import com.sharesmile.share.WorkoutDao;
import com.sharesmile.share.core.Constants;
import com.sharesmile.share.gps.models.WorkoutData;
import com.sharesmile.share.rfac.fragments.ShareFragment;
import com.sharesmile.share.rfac.models.CauseData;
import com.sharesmile.share.sync.SyncHelper;
import com.sharesmile.share.utils.Logger;
import com.sharesmile.share.utils.SharedPrefsManager;
import com.sharesmile.share.utils.Utils;
import com.squareup.picasso.Picasso;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by ankitm on 22/04/16.
 */
public class RealRunFragment extends RunFragment {

    private static final String TAG = "RealRunFragment";
    public static final String BUNDLE_CAUSE_DATA = "bundle_cause_data";

    TextView time;
    TextView distance;
    TextView impact;
    ProgressBar runProgressBar;
    Button pauseButton;
    Button stopButton;

    @BindView(R.id.img_sponsor_logo)
    ImageView mSponsorLogo;

    @BindView(R.id.timer_indicator)
    TextView mTimerIndicator;
    private CauseData mCauseData;
    MixpanelAPI mixpanel;

    public static final String RUPEES_IMPACT_ON_PAUSE = "rupees_impact_on_pause";
    public static final String DISTANCE_COVERED_ON_PAUSE = "distance_covered_on_pause";

    public static RealRunFragment newInstance(CauseData causeData) {
        RealRunFragment fragment = new RealRunFragment();
        Bundle args = new Bundle();
        args.putSerializable(BUNDLE_CAUSE_DATA, causeData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arg = getArguments();
        mCauseData = (CauseData) arg.getSerializable(BUNDLE_CAUSE_DATA);
        loadThankYouImage();
    }

    @Override
    protected void populateViews(View baseView) {
        ButterKnife.bind(this, baseView);
        time = (TextView) baseView.findViewById(R.id.tv_run_progress_timer);
        distance = (TextView) baseView.findViewById(R.id.tv_run_progress_distance);
        impact = (TextView) baseView.findViewById(R.id.tv_run_progress_impact);
        runProgressBar = (ProgressBar) baseView.findViewById(R.id.run_progress_bar);
        pauseButton = (Button) baseView.findViewById(R.id.btn_pause);
        stopButton = (Button) baseView.findViewById(R.id.btn_stop);
        pauseButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        Picasso.with(getContext()).load(mCauseData.getSponsor().getLogoUrl()).into(mSponsorLogo);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Will begin workout if not already active
        if (!myActivity.isWorkoutActive()) {
            beginRun();
        } else {
            continuedRun();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.run_progress;
    }

    @Override
    public void updateTimeView(String newTime) {
        time.setText(newTime);
        if (newTime.length() > 5) {
            mTimerIndicator.setText("HR:MIN:SEC");
        }
    }

    @Override
    public void onWorkoutResult(WorkoutData data) {
        //Workout completed and results obtained, time to show the next Fragment
        if (isAttachedToActivity()) {
            if (mCauseData.getMinDistance() > data.getDistance()) {
                myActivity.exit();
                return;
            }
            boolean isLogin = SharedPrefsManager.getInstance().getBoolean(Constants.PREF_IS_LOGIN);
            getFragmentController().replaceFragment(ShareFragment.newInstance(data, mCauseData, !isLogin), false);

            WorkoutDao workoutDao = MainApplication.getInstance().getDbWrapper().getWorkoutDao();
            Workout workout = new Workout();

            workout.setAvgSpeed(data.getAvgSpeed());
            workout.setDistance(data.getDistance() / 1000);
            workout.setElapsedTime(Utils.secondsToString((int) data.getElapsedTime()));

            //data.getDistance()
            String distDecimal = String.format("%1$,.1f", (data.getDistance() / 1000));
            int rupees = (int) Math.ceil(getConversionFactor() * Float.valueOf(distDecimal));

            workout.setRunAmount((float) rupees);
            workout.setRecordedTime(data.getRecordedTime());
            workout.setSteps(data.getTotalSteps());
            workout.setCauseBrief(mCauseData.getTitle());
            workout.setDate(Calendar.getInstance().getTime());
            workout.setIs_sync(false);
            workoutDao.insertOrReplace(workout);

            //update userImpact
            updateUserImpact(workout);

            mixpanel = MixpanelAPI.getInstance(getActivity().getBaseContext(), getString(R.string.mixpanel_project_token));
            try {
                JSONObject props = new JSONObject();
                props.put("End Run", "Clicked");
                props.put("RunAmount", rupees);
                props.put("CauseBrief", mCauseData.getTitle());
                props.put("Distance Ran", distDecimal);

                mixpanel.track("RealRunFragment - onWorkoutResult called", props);
            } catch (JSONException e) {
                Logger.e(TAG, "Unable to add properties to JSONObject", e);
            }

            SharedPrefsManager.getInstance().setBoolean(Constants.PREF_HAS_RUN, true);
            SyncHelper.pushRunData();
        }
    }

    private void updateUserImpact(Workout data) {
        int totalRun = SharedPrefsManager.getInstance().getInt(Constants.PREF_TOTAL_RUN, 0);
        float totalImpact = SharedPrefsManager.getInstance().getInt(Constants.PREF_TOTAL_IMPACT, 0);

        totalImpact = totalImpact + data.getRunAmount();
        totalRun = totalRun + 1;

        SharedPrefsManager.getInstance().setInt(Constants.PREF_TOTAL_RUN, totalRun);
        SharedPrefsManager.getInstance().setInt(Constants.PREF_TOTAL_IMPACT, (int)totalImpact);
    }

    @Override
    public void showUpdate(float speed, float distanceCovered, int elapsedTimeInSecs) {
        super.showUpdate(speed, distanceCovered, elapsedTimeInSecs);

        String distDecimal = String.format("%1$,.1f", (distanceCovered / 1000));
        distance.setText(distDecimal);

        int rupees = (int) Math.ceil(getConversionFactor() * Float.valueOf(distance.getText().toString()));
        impact.setText(String.valueOf(rupees));
    }

    @Override
    public void showSteps(int stepsSoFar, int elapsedTimeInSecs) {
        super.showSteps(stepsSoFar, elapsedTimeInSecs);
    }

    @Override
    protected void onEndRun() {
        clearState();
        // Will wait for workout result broadcast
    }

    @Override
    protected void onPauseRun() {
        pauseButton.setText(R.string.resume);
        runProgressBar.setVisibility(View.INVISIBLE);
        persistStateOnPause();
    }

    private void persistStateOnPause() {
        SharedPrefsManager.getInstance().setString(DISTANCE_COVERED_ON_PAUSE, distance.getText().toString());
        SharedPrefsManager.getInstance().setString(RUPEES_IMPACT_ON_PAUSE, impact.getText().toString());
    }

    private void clearState() {
        SharedPrefsManager.getInstance().removeKey(DISTANCE_COVERED_ON_PAUSE);
        SharedPrefsManager.getInstance().removeKey(RUPEES_IMPACT_ON_PAUSE);
    }

    @Override
    protected void onResumeRun() {
        pauseButton.setText(R.string.pause);
        runProgressBar.setVisibility(View.VISIBLE);
        clearState();
    }

    @Override
    protected void onBeginRun() {
        impact.setText("0");
        distance.setText("0.0");
        clearState();
    }

    @Override
    protected void onContinuedRun(boolean isPaused) {
        if (!isRunning()) {
            pauseButton.setText(R.string.resume);
            runProgressBar.setVisibility(View.INVISIBLE);
            impact.setText(SharedPrefsManager.getInstance().getString(RUPEES_IMPACT_ON_PAUSE));
            distance.setText(SharedPrefsManager.getInstance().getString(DISTANCE_COVERED_ON_PAUSE));
        } else {
//            refreshWorkoutData();
        }
    }

    @Override
    public void showErrorMessage(String msg) {
        showErrorDialog(msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pause:
                if (isRunActive()) {
                    if (isRunning()) {
                        pauseRun(true);
                    } else {
                        resumeRun();
                    }
                }
                break;

            case R.id.btn_stop:

                showStopDialog();
                break;
        }
    }

    @Override
    public void showStopDialog() {
        String rDistance = distance.getText().toString();
        Float fDistance = Float.parseFloat(rDistance);
        if (mCauseData.getMinDistance() > (fDistance * 1000)) {
            showMinDistanceDialog();
        } else {
            showRunEndDialog();
        }
    }

    private void showRunEndDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Finish Run");
        alertDialog.setMessage("Are you sure you want to end the run?");
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                endRun(true);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void showMinDistanceDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(getString(R.string.dialog_title_min_distance));
        alertDialog.setMessage(getString(R.string.dialog_msg_min_distance, mCauseData.getMinDistance()));
        alertDialog.setPositiveButton(getString(R.string.dialog_positive_button_min_distance), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.setNegativeButton(getString(R.string.dialog_negative_button_min_distance), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                endRun(true);
            }
        });
        alertDialog.show();
    }

    /*  Rs per km*/
    public float getConversionFactor() {
        return mCauseData.getConversionRate();
    }

    private void loadThankYouImage() {
        if (mCauseData != null) {
            Picasso.with(getActivity()).load(mCauseData.getCauseThankYouImage()).fetch();
        }
    }

    private void showErrorDialog(String msg) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        View view = getLayoutInflater(null).inflate(R.layout.alert_dialog_title, null);
        view.setBackgroundColor(getResources().getColor(R.color.neon_red));
        TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText(getString(R.string.error));
        alertDialog.setCustomTitle(view);
        alertDialog.setMessage(msg);
        alertDialog.setPositiveButton(getString(R.string.resume), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                resumeRun();

            }
        });
        alertDialog.setNegativeButton(getString(R.string.stop), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                endRun(true);
            }
        });

        alertDialog.show();
    }

}
