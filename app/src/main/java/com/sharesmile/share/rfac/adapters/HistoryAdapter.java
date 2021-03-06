package com.sharesmile.share.rfac.adapters;

import android.graphics.Paint;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sharesmile.share.R;
import com.sharesmile.share.Workout;
import com.sharesmile.share.utils.DateUtil;
import com.sharesmile.share.utils.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Shine on 13/05/16.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final AdapterInterface mInterface;
    private List<Workout> mData;

    public HistoryAdapter(AdapterInterface adapterInterface) {
        mInterface = adapterInterface;
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_list_item, parent, false);

        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {

        holder.bindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public void setData(List<Workout> data) {
        this.mData = data;
        notifyDataSetChanged();
    }


    class HistoryViewHolder extends RecyclerView.ViewHolder {


        @BindView(R.id.date)
        TextView mDate;
        @BindView(R.id.cause_name)
        TextView mCause;
        @BindView(R.id.distance)
        TextView mDistance;

        @BindView(R.id.impact)
        TextView mImpact;

        @BindView(R.id.duration)
        TextView mDuration;

        @BindView(R.id.error_indicator)
        ImageView mIndicator;

        @BindView(R.id.content_view)
        CardView mCard;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bindData(Workout workout) {
            if (workout.getDate() != null) {
                mDate.setText(DateUtil.getUserFormattedDate(workout.getDate()));
            }
            mCause.setText(workout.getCauseBrief());
            String distanceCovered = Utils.formatWithOneDecimal(workout.getDistance());
            mDistance.setText(distanceCovered + " km");
            mImpact.setText(mImpact.getContext().getString(R.string.rs_symbol) + " " + (int) Math.ceil(workout.getRunAmount()));

            long timeInSec = Utils.stringToSec(workout.getElapsedTime());
            if (timeInSec >= 60) {
                int timeInMin = (int) (Utils.stringToSec(workout.getElapsedTime()) / 60);
                mDuration.setText(mImpact.getResources().getQuantityString(R.plurals.time_in_min, timeInMin, timeInMin));
            } else {
                mDuration.setText(mImpact.getResources().getQuantityString(R.plurals.time_in_sec, (int) timeInSec, (int) timeInSec));
            }

            if (workout.getIsValidRun()) {
                mIndicator.setVisibility(View.GONE);
                mCard.setCardBackgroundColor(itemView.getResources().getColor(R.color.white));
                mCard.setOnClickListener(null);
                mImpact.setPaintFlags(mImpact.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                mIndicator.setVisibility(View.VISIBLE);
                mCard.setCardBackgroundColor(itemView.getResources().getColor(R.color.very_light_grey));
                mImpact.setPaintFlags(mImpact.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);


                mCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mInterface.showInvalidRunDialog();
                    }
                });
            }


        }
    }

    public interface AdapterInterface {
        public void showInvalidRunDialog();
    }
}
