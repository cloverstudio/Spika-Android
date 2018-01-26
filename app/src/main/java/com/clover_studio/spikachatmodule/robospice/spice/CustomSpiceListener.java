package com.clover_studio.spikachatmodule.robospice.spice;

import android.content.Context;
import android.text.TextUtils;

import com.clover_studio.spikachatmodule.R;
import com.clover_studio.spikachatmodule.base.BaseActivity;
import com.clover_studio.spikachatmodule.dialogs.NotifyDialog;
import com.clover_studio.spikachatmodule.base.BaseModel;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;


public class CustomSpiceListener<T> implements RequestListener<T> {

    private Context ctx;

    public CustomSpiceListener(Context context) {
        this.ctx = context;
    }

    @Override
    public void onRequestFailure(SpiceException spiceException) {

        if (ctx != null) {

            if (ctx instanceof BaseActivity && !((BaseActivity) ctx).isFinishing()) {
                ((BaseActivity) ctx).handleProgress(false);
            }

            final NotifyDialog dialog = new NotifyDialog(ctx,
                   ctx.getString(R.string.e_network_error),
                    !TextUtils.isEmpty(spiceException.getMessage()) ? spiceException.getMessage() : ctx.getString(R.string.e_something_went_wrong),
                    NotifyDialog.Type.INFO);

            dialog.show();
        }
    }

    @Override
    public void onRequestSuccess(T t) {

        if (ctx instanceof BaseActivity && !((BaseActivity) ctx).isFinishing()) {
            ((BaseActivity) ctx).handleProgress(false);
        }

        if(t instanceof BaseModel){
            if(((BaseModel)t).success == 0){
                if(((BaseModel)t).error != null){
                    NotifyDialog dialog = NotifyDialog.startInfo(ctx, ctx.getString(R.string.error), ((BaseModel)t).error.message);
                }else{
                    NotifyDialog dialog = NotifyDialog.startInfo(ctx, ctx.getString(R.string.error), ctx.getString(R.string.e_something_went_wrong));
                }
            }
        }
    }
}
