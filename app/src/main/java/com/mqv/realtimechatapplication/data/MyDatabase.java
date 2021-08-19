package com.mqv.realtimechatapplication.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.model.LoggedInUser;

@Database(entities = {LoggedInUser.class}, version = 1)
public abstract class MyDatabase extends RoomDatabase {
    public abstract UserDao getUserDao();
}
