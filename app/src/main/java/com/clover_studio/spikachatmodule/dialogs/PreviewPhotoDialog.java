package com.clover_studio.spikachatmodule.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clover_studio.spikachatmodule.R;
import com.clover_studio.spikachatmodule.lazy.ImageLoaderSpice;

public class PreviewPhotoDialog extends Dialog {

    String imageUrl;

    public static PreviewPhotoDialog startDialog(Context context, String imageUrl){
        PreviewPhotoDialog dialog = new PreviewPhotoDialog(context, imageUrl);
        return dialog;
    }

    public PreviewPhotoDialog(Context context, String imageUrl) {
        super(context, R.style.Theme_Dialog);

        setOwnerActivity((Activity) context);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        this.imageUrl = imageUrl;

        show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_preview_photo);

        ImageView photo = (ImageView) findViewById(R.id.photoIv);
        ImageLoaderSpice.getInstance(getOwnerActivity()).displayImage(photo, imageUrl, 0, new ImageLoaderSpice.OnImageDisplayFinishListener() {
            @Override
            public void onFinish() {
                findViewById(R.id.progressBarLoading).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.closeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

}