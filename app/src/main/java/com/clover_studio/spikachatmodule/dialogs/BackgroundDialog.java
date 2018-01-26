package com.clover_studio.spikachatmodule.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.clover_studio.spikachatmodule.R;

public class BackgroundDialog extends Dialog {

    private OnBackgroundSelected listener;

    public static BackgroundDialog startDialog(Context context, OnBackgroundSelected listener){
        BackgroundDialog dialog = new BackgroundDialog(context, listener);
        return dialog;
    }

    public BackgroundDialog(Context context, OnBackgroundSelected lis) {
        super(context, R.style.Theme_Dialog);

        setCancelable(true);
        setCanceledOnTouchOutside(true);

        listener = lis;

        show();

    }

    public void setListener(OnBackgroundSelected listener){
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_for_choose_background);

        setClickOption();

    }

    private void setClickOption() {

        findViewById(R.id.pattern1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) listener.onSelected(R.drawable.chat_back_1, BackgroundDialog.this);
            }
        });

        findViewById(R.id.pattern2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) listener.onSelected(R.drawable.chat_back_2, BackgroundDialog.this);
            }
        });

        findViewById(R.id.pattern3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) listener.onSelected(R.drawable.chat_back_3, BackgroundDialog.this);
            }
        });

        findViewById(R.id.pattern4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) listener.onSelected(R.drawable.chat_back_4, BackgroundDialog.this);
            }
        });

    }

    public interface OnBackgroundSelected{
        public void onSelected(int drawable, Dialog dialog);
    }

}