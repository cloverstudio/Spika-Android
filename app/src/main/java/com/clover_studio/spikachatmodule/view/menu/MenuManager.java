package com.clover_studio.spikachatmodule.view.menu;

import android.app.Activity;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.clover_studio.spikachatmodule.R;
import com.clover_studio.spikachatmodule.utils.AnimUtils;
import com.clover_studio.spikachatmodule.utils.Const;
import com.clover_studio.spikachatmodule.view.circularview.animation.SupportAnimator;
import com.clover_studio.spikachatmodule.view.circularview.animation.ViewAnimationUtils;
import com.clover_studio.spikachatmodule.utils.Tools;

/**
 * Created by ubuntu_ivo on 24.07.15..
 */
public class MenuManager {

    private RelativeLayout rlMenuMain;
    private SupportAnimator menuAnimator;
    private LinearLayout location;
    private LinearLayout camera;
    private LinearLayout gallery;
    private LinearLayout audio;
    private LinearLayout video;
    private LinearLayout contact;

    private OnMenuManageListener listener;
    private OnMenuButtonsListener buttonsListener;

    public void setMenuLayout(Activity activity, int menuLayoutId, OnMenuManageListener listener, final OnMenuButtonsListener buttonsListener){

        rlMenuMain = (RelativeLayout) activity.findViewById(menuLayoutId);
        location = (LinearLayout) activity.findViewById(R.id.location);
        camera = (LinearLayout) activity.findViewById(R.id.camera);
        gallery = (LinearLayout) activity.findViewById(R.id.gallery);
        audio = (LinearLayout) activity.findViewById(R.id.audio);
        video = (LinearLayout) activity.findViewById(R.id.video);
        contact = (LinearLayout) activity.findViewById(R.id.file);

        if(!Tools.isBuildOver(20)){
            fixFloatingButtonInPreL(activity, (FloatingActionButton) location.getChildAt(0), (FloatingActionButton) camera.getChildAt(0), (FloatingActionButton) gallery.getChildAt(0),
                            (FloatingActionButton) audio.getChildAt(0), (FloatingActionButton) video.getChildAt(0), (FloatingActionButton) contact.getChildAt(0));
        }

        this.listener = listener;
        this.buttonsListener = buttonsListener;

        location.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsListener.onLocationClicked();
            }
        });

        camera.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsListener.onCameraClicked();
            }
        });

        gallery.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsListener.onGalleryClicked();
            }
        });

        audio.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsListener.onAudioClicked();
            }
        });

        video.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsListener.onVideoClicked();
            }
        });

        contact.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonsListener.onFileClicked();
            }
        });

    }

    public void openMenu(ImageButton btnSend){

        ((View)rlMenuMain.getParent().getParent()).setVisibility(View.VISIBLE);

        // get the center for the clipping circle
        int cx = btnSend.getLeft();
        int cy = rlMenuMain.getBottom();

        // get the final radius for the clipping circle
        int finalRadius = Math.max(rlMenuMain.getWidth(), rlMenuMain.getHeight());

        menuAnimator = ViewAnimationUtils.createCircularReveal(rlMenuMain, cx, cy, 0, finalRadius);
        menuAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        menuAnimator.setDuration(Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);
        menuAnimator.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
            }

            @Override
            public void onAnimationEnd() {
                listener.onMenuOpened();
            }

            @Override
            public void onAnimationCancel() {
            }

            @Override
            public void onAnimationRepeat() {
            }
        });
        menuAnimator.start();

        handleButtonsOnOpen();

    }

    protected void handleButtonsOnOpen(){

        singleButtonAnimationOn(audio, 100);
        singleButtonAnimationOn(location, 150);
        singleButtonAnimationOn(video, 200);
        singleButtonAnimationOn(gallery, 250);
        singleButtonAnimationOn(contact, 300);
        singleButtonAnimationOn(camera, 350);

    }

    protected void singleButtonAnimationOn(final View view, int offset){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
                AnimUtils.scale(view, 0.6f, 1, Const.AnimationDuration.MENU_BUTTON_ANIMATION_DURATION, null);
            }
        }, offset);
    }

    public void closeMenu(){

        if(menuAnimator == null) {
            return;
        }

        menuAnimator = menuAnimator.reverse();
        if(menuAnimator != null){
            menuAnimator.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {}

                @Override
                public void onAnimationEnd() {
                    listener.onMenuClosed();
                    ((View)rlMenuMain.getParent().getParent()).setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel() {}

                @Override
                public void onAnimationRepeat() {}
            });

            menuAnimator.start();
        }

        handleButtonsOnClose();

    }

    protected void handleButtonsOnClose(){

        singleButtonAnimationOff(audio, Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);
        singleButtonAnimationOff(location, Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);
        singleButtonAnimationOff(video, Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);
        singleButtonAnimationOff(gallery, Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);
        singleButtonAnimationOff(contact, Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);
        singleButtonAnimationOff(camera, Const.AnimationDuration.MENU_LAYOUT_ANIMATION_DURATION);

    }

    protected void singleButtonAnimationOff(final View view, int offset){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.INVISIBLE);
            }
        }, offset);
    }

    protected void fixFloatingButtonInPreL(Activity activity, FloatingActionButton... args){
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -16, activity.getResources().getDisplayMetrics());
        for(int i = 0; i < args.length; i++){
            FloatingActionButton mFab = args[i];
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mFab.getLayoutParams();
            p.setMargins(0, margin, 0, margin); // get rid of margins since shadow area is now the margin
            mFab.setLayoutParams(p);
        }
    }

}
