package com.sharesmile.share.core;

import android.content.Context;
import android.support.v4.app.Fragment;

/**
 * Created by ankitmaheshwari1 on 11/01/16.
 */
public class BaseFragment extends Fragment {

    private static final String TAG = "BaseFragment";

    private IFragmentController controller;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.controller = (IFragmentController) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.controller = null;
    }

    public boolean isAttachedToActivity(){
        return controller != null;
    }

    public IFragmentController getFragmentController(){
        return controller;
    }

    /**
     Override this method if you want your fragment to receive a back press callback.

     @return true if your fragment consumes the back press event,
     false if the back press should be handled by the activity. Returns false
     by default.
     */
    protected boolean handleBackPress() {
        return false;
    }

    public String getName(){
        return getClass().getCanonicalName();
    }

}
