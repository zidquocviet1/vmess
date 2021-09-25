package com.mqv.realtimechatapplication.network.model.type;

import android.util.SparseArray;

import com.google.gson.annotations.SerializedName;

public enum Gender {
    @SerializedName("male")
    MALE("Male", 1),
    @SerializedName("female")
    FEMALE("Female", 2),
    @SerializedName("nonBinary")
    NON_BINARY("Non-binary", 3),
    @SerializedName("transgender")
    TRANSGENDER("Transgender", 4),
    @SerializedName("intersex")
    INTERSEX("Intersex", 5),
    @SerializedName("preferNotToSay")
    PREFER_NOT_TO_SAY("Prefer not to say", 6);

    private final String value;
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

    Gender(String value, int key) {
        this.value = value;
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public int getKey(){
        return key;
    }

    public static Gender getGenderByKey(int key){
        return array.get(key);
    }
}
