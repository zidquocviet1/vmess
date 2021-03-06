package com.mqv.vmess.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.mqv.vmess.data.converter.ChatFileConverter;
import com.mqv.vmess.data.converter.ChatPhotoConverter;
import com.mqv.vmess.data.converter.ChatShareConverter;
import com.mqv.vmess.data.converter.ChatVideoConverter;
import com.mqv.vmess.data.converter.ConversationChatConverter;
import com.mqv.vmess.data.converter.ConversationGroupConverter;
import com.mqv.vmess.data.converter.ConversationParticipantsConverter;
import com.mqv.vmess.data.converter.ConversationStatusConverter;
import com.mqv.vmess.data.converter.ConversationTypeConverter;
import com.mqv.vmess.data.converter.GenderConverter;
import com.mqv.vmess.data.converter.IdentityKeyPairConverter;
import com.mqv.vmess.data.converter.LocalDateTimeConverter;
import com.mqv.vmess.data.converter.MessageSeenByConverter;
import com.mqv.vmess.data.converter.MessageStatusConverter;
import com.mqv.vmess.data.converter.MessageTypeConverter;
import com.mqv.vmess.data.converter.SocialLinksListConverter;
import com.mqv.vmess.data.converter.SocialTypeConverter;
import com.mqv.vmess.data.dao.AccountDao;
import com.mqv.vmess.data.dao.ChatDao;
import com.mqv.vmess.data.dao.ConversationColorDao;
import com.mqv.vmess.data.dao.ConversationDao;
import com.mqv.vmess.data.dao.ConversationOptionDao;
import com.mqv.vmess.data.dao.FriendNotificationDao;
import com.mqv.vmess.data.dao.HistoryLoggedInUserDao;
import com.mqv.vmess.data.dao.IdentityKeyDao;
import com.mqv.vmess.data.dao.LinkMetadataDao;
import com.mqv.vmess.data.dao.LocalPlaintextContentDao;
import com.mqv.vmess.data.dao.PendingMessageDao;
import com.mqv.vmess.data.dao.PeopleDao;
import com.mqv.vmess.data.dao.PreKeyDao;
import com.mqv.vmess.data.dao.RecentSearchDao;
import com.mqv.vmess.data.dao.SeenMessageDao;
import com.mqv.vmess.data.dao.SenderKeyDao;
import com.mqv.vmess.data.dao.SessionDao;
import com.mqv.vmess.data.dao.SignedPreKeyDao;
import com.mqv.vmess.data.dao.UserDao;
import com.mqv.vmess.data.model.AccountModel;
import com.mqv.vmess.data.model.ConversationColor;
import com.mqv.vmess.data.model.ConversationIgnoreOption;
import com.mqv.vmess.data.model.ConversationNotificationOption;
import com.mqv.vmess.data.model.FriendNotification;
import com.mqv.vmess.data.model.HistoryLoggedInUser;
import com.mqv.vmess.data.model.IdentityKeyModel;
import com.mqv.vmess.data.model.LinkMetadata;
import com.mqv.vmess.data.model.LocalPlaintextContentModel;
import com.mqv.vmess.data.model.PendingMessage;
import com.mqv.vmess.data.model.PreKeyModel;
import com.mqv.vmess.data.model.RecentSearchPeople;
import com.mqv.vmess.data.model.SeenMessage;
import com.mqv.vmess.data.model.SenderKeyModel;
import com.mqv.vmess.data.model.SessionModel;
import com.mqv.vmess.data.model.SignedPreKeyModel;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.ui.data.People;

@Database(entities = {User.class,
                      HistoryLoggedInUser.class,
                      People.class,
                      Conversation.class,
                      Chat.class,
                      PendingMessage.class,
                      SeenMessage.class,
                      FriendNotification.class,
                      ConversationNotificationOption.class,
                      ConversationIgnoreOption.class,
                      RecentSearchPeople.class,
                      ConversationColor.class,
                      LinkMetadata.class,
                      AccountModel.class,
                      IdentityKeyModel.class,
                      PreKeyModel.class,
                      SenderKeyModel.class,
                      SignedPreKeyModel.class,
                      SessionModel.class,
                      LocalPlaintextContentModel.class},
        version = 35,
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
                         ConversationGroupConverter.class,
                         ChatPhotoConverter.class,
                         ChatShareConverter.class,
                         ChatVideoConverter.class,
                         ChatFileConverter.class,
                         IdentityKeyPairConverter.class})
public abstract class MyDatabase extends RoomDatabase {
    public abstract UserDao getUserDao();

    public abstract HistoryLoggedInUserDao getHistoryUserDao();

    public abstract PeopleDao getPeopleDao();

    public abstract FriendNotificationDao getFriendNotificationDao();

    public abstract ConversationDao getConversationDao();

    public abstract ChatDao getChatDao();

    public abstract PendingMessageDao getPendingMessageDao();

    public abstract SeenMessageDao getSeenMessageDao();

    public abstract ConversationOptionDao getConversationOptionDao();

    public abstract RecentSearchDao getRecentSearchDao();

    public abstract ConversationColorDao getConversationColorDao();

    public abstract LinkMetadataDao getLinkMetadataDao();

    public abstract PreKeyDao getPreKeyDao();

    public abstract SignedPreKeyDao getSignedPreKeyDao();

    public abstract SenderKeyDao getSenderKeyDao();

    public abstract IdentityKeyDao getIdentityKeyDao();

    public abstract AccountDao getAccountDao();

    public abstract SessionDao getSessionDao();

    public abstract LocalPlaintextContentDao getPlaintextContentDao();
}
