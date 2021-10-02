package com.mqv.realtimechatapplication.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.mqv.realtimechatapplication.data.converter.GenderConverter;
import com.mqv.realtimechatapplication.data.converter.LocalDateTimeConverter;
import com.mqv.realtimechatapplication.data.converter.SocialLinksListConverter;
import com.mqv.realtimechatapplication.data.converter.SocialTypeConverter;
import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.PeopleDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.data.People;

@Database(entities = {User.class, HistoryLoggedInUser.class, People.class}, version = 4, exportSchema = false)
@TypeConverters({LocalDateTimeConverter.class, GenderConverter.class,
        SocialTypeConverter.class, SocialLinksListConverter.class})
public abstract class MyDatabase extends RoomDatabase {
    public abstract UserDao getUserDao();

    public abstract HistoryLoggedInUserDao getHistoryUserDao();

    public abstract PeopleDao getPeopleDao();
}
