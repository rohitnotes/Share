package com.sharesmile.share.rfac.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sharesmile.share.R;
import com.sharesmile.share.LeaderBoard;
import com.sharesmile.share.views.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by piyush on 9/1/16.
 */
public class LeaderBoardAdapter extends RecyclerView.Adapter<LeaderBoardAdapter.LeaderBoardViewHolder>{

    private List<LeaderBoard> mData;
    private Context mContext;

    public LeaderBoardAdapter(Context context, List<LeaderBoard> leaderBoard) {
        this.mData = leaderBoard;
        this.mContext = context;
    }

    @Override
    public LeaderBoardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leaderboard_list_item, parent, false);
        return new LeaderBoardViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public void setData(List<LeaderBoard> data){
        this.mData = data;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(LeaderBoardViewHolder holder, int position) {
        holder.bindData(mData.get(position), position);
    }

    class LeaderBoardViewHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.id_leaderboard)
        TextView mleaderBoard;
        @BindView(R.id.img_profile)
        CircularImageView mProfileImage;

        @BindView(R.id.tv_profile_name)
        TextView mProfileName;

        @BindView(R.id.last_week_distance)
        TextView mlastWeekDistance;



        public LeaderBoardViewHolder(View itemView){
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        private void bindData(LeaderBoard leaderboard, int position){
            mleaderBoard.setText(String.valueOf(position + 1));

            Picasso.with(mContext).
                    load(leaderboard.getSocial_thumb()).
                    placeholder(R.drawable.placeholder_profile).
                    into(mProfileImage);

            String firstName = leaderboard.getFirst_name().substring(0,1).toUpperCase() + leaderboard.getFirst_name().substring(1);
            String lastName = leaderboard.getLast_name().substring(0,1).toUpperCase() + leaderboard.getLast_name().substring(1);

            String name = firstName + ' ' + lastName;
            mProfileName.setText(name);

            String last_Week_Distance =  String.format("%.2f", leaderboard.getLast_week_distance());
            mlastWeekDistance.setText(last_Week_Distance + " Km");


        }


    }
}