package com.mqv.realtimechatapplication.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.mqv.realtimechatapplication.data.converter.ConversationChatConverter;
import com.mqv.realtimechatapplication.data.converter.ConversationGroupConverter;
import com.mqv.realtimechatapplication.data.converter.ConversationParticipantsConverter;
import com.mqv.realtimechatapplication.data.converter.ConversationStatusConverter;
import com.mqv.realtimechatapplication.data.converter.ConversationTypeConverter;
import com.mqv.realtimechatapplication.data.converter.GenderConverter;
import com.mqv.realtimechatapplication.data.converter.LocalDateTimeConverter;
import com.mqv.realtimechatapplication.data.converter.MessageSeenByConverter;
import com.mqv.realtimechatapplication.data.converter.MessageStatusConverter;
import com.mqv.realtimechatapplication.data.converter.MessageTypeConverter;
import com.mqv.realtimechatapplication.data.converter.SocialLinksListConverter;
import com.mqv.realtimechatapplication.data.converter.SocialTypeConverter;
import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.NotificationDao;
import com.mqv.realtimechatapplication.data.dao.PendingMessageDao;
import com.mqv.realtimechatapplication.data.dao.PeopleDao;
import com.mqv.realtimechatapplication.data.dao.SeenMessageDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.data.model.PendingMessage;
import com.mqv.realtimechatapplication.data.model.SeenMessage;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.data.People;

@Database(entities = {User.class,
                      HistoryLoggedInUser.class,
                      People.class,
                      Notification.class,
                      Conversation.class,
                      Chat.class,
                      PendingMessage.class,
                      SeenMessage.class},
        version = 17,
        exportSchema = false)
@TypeConverters(value = {LocalDateTimeConverter.class,
                         GenderConverter.class,
                         SocialTypeConverter.class,
                         SocialLinksListConverter.class,
                         ConversationParticipantsConverter.class,
                         ConversationChatConverter.class,
                         ConversationTypeConverter.class,
                         ConversationStatusConverter.class,
                         MessageTypeConverter.class,
                         MessageStatusConverter.class,
                         MessageSeenByConverter.class,
                         ConversationGroupConverter.class})
public abstract class MyDatabase extends RoomDatabase {
    public abstract UserDao getUserDao();

    public abstract HistoryLoggedInUserDao getHistoryUserDao();

    public abstract PeopleDao getPeopleDao();

    public abstract NotificationDao getNotificationDao();

    public abstract ConversationDao getConversationDao();

    public abstract ChatDao getChatDao();

    public abstract PendingMessageDao getPendingMessageDao();

    public abstract SeenMessageDao getSeenMessageDao();
}
