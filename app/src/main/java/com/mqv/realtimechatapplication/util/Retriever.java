package com.mqv.realtimechatapplication.util;

public final class Retriever {
    public static <T, E extends T> T getOrDefault(T expected, E defaultValue) {
        if (expected == null) {
            return defaultValue;
        }
        return expected;
    }
}
