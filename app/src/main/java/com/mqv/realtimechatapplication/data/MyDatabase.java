package com.mqv.realtimechatapplication.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.mqv.realtimechatapplication.data.converter.GenderConverter;
import com.mqv.realtimechatapplication.data.converter.LocalDateTimeConverter;
import com.mqv.realtimechatapplication.data.converter.SocialLinksListConverter;
import com.mqv.realtimechatapplication.data.converter.SocialTypeConverter;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.network.model.User;

@Database(entities = {User.class}, version = 1)
@TypeConverters({LocalDateTimeConverter.class, GenderConverter.class,
        SocialTypeConverter.class, SocialLinksListConverter.class})
public abstract class MyDatabase extends RoomDatabase {
    public abstract UserDao getUserDao();
}
