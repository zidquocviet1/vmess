package com.mqv.vmess.network.model.type;

import android.content.Context;
import android.util.SparseArray;

import com.google.gson.annotations.SerializedName;
import com.mqv.vmess.R;

public enum Gender {
    @SerializedName("male")
    MALE(R.string.title_male, 1),
    @SerializedName("female")
    FEMALE(R.string.title_female, 2),
    @SerializedName("nonBinary")
    NON_BINARY(R.string.title_non_binary, 3),
    @SerializedName("transgender")
    TRANSGENDER(R.string.title_transgender, 4),
    @SerializedName("intersex")
    INTERSEX(R.string.title_intersex, 5),
    @SerializedName("preferNotToSay")
    PREFER_NOT_TO_SAY(R.string.title_prefer_not_to_say, 6);

    private final int res;
    private final int key;
    private static final SparseArray<Gender> array = new SparseArray<>();
    static {
        array.put(1, MALE);
        array.put(2, FEMALE);
        array.put(3, NON_BINARY);
        array.put(4, TRANSGENDER);
        array.put(5, INTERSEX);
        array.put(6, PREFER_NOT_TO_SAY);
    }

    Gender(int res, int key) {
        this.res = res;
        this.key = key;
    }

    public String getValue(Context context) {
        return context.getString(res);
    }

    public int getKey(){
        return key;
    }

    public static Gender getGenderByKey(int key){
        return array.get(key);
    }
}
