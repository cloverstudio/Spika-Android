package com.clover_studio.spikachatmodule.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.clover_studio.spikachatmodule.base.BaseModel;

/**
 * Created by ubuntu_ivo on 22.07.15..
 */
public class UploadFileResult extends BaseModel implements Parcelable{

    public FileModel result;

    public UploadFileResult() {
    }

    @Override
    public String toString() {
        return "UploadFileResult{" +
                "result=" + result +
                '}';
    }

    protected UploadFileResult(Parcel in) {
        result = (FileModel) in.readValue(FileModel.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(result);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<UploadFileResult> CREATOR = new Parcelable.Creator<UploadFileResult>() {
        @Override
        public UploadFileResult createFromParcel(Parcel in) {
            return new UploadFileResult(in);
        }

        @Override
        public UploadFileResult[] newArray(int size) {
            return new UploadFileResult[size];
        }
    };

}
