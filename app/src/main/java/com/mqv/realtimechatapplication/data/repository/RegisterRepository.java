package com.mqv.realtimechatapplication.data.repository;

import java.util.Map;

public interface RegisterRepository {
    void login(Map<String, Object> payload);
}
