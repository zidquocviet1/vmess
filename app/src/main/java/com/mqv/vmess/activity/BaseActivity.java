package com.mqv.vmess.activity;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.activity.listener.OnNetworkChangedListener;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.DarkMode;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.firebase.FirebaseUserManager;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.websocket.WebSocketConnectionState;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.util.MyActivityForResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public abstract class BaseActivity<V extends ViewModel, B extends ViewBinding>
        extends AppCompatActivity {

    public B mBinding;
    public V mViewModel;

    @Inject
    AppPreferences mPreferences;

    /*
     * Some callback to notify current user change
     * */
    private Consumer<FirebaseUser> firebaseUserConsumer;
    private Consumer<User> loggedInUserConsumer;
    private FirebaseUserManager firebaseUserManager;
    private LoggedInUserManager loggedInUserManager;

    /*
    * Network manager to check network status and notify
    * */
    private static ConnectivityManager sConnectivityManager;
    private OnNetworkChangedListener mOnNetworkChangedListener;

    /*
     * Attribute to check whether change dark mode theme
     * */
    private boolean themeChangePending;
    private boolean paused;
    private boolean hasNetwork;

    private final AppPreferences.Listener onPreferenceChanged = new AppPreferences.Listener() {
        @Override
        public void onDarkThemeModeChanged(DarkMode mode) {
            onThemeSettingsModeChange();
        }
    };

    private final FirebaseUserManager.Listener onFirebaseUserChanged = new FirebaseUserManager.Listener() {
        @Override
        public void onUserChanged() {
            if (firebaseUserConsumer != null) {
                firebaseUserConsumer.accept(getCurrentUser());
            }
        }
    };

    private final LoggedInUserManager.LoggedInUserListener onLoggedInUserChanged = user -> {
        if (loggedInUserConsumer != null)
            loggedInUserConsumer.accept(user);
    };

    private Disposable mConnectionStateDisposable;

    public MyActivityForResult<Intent, ActivityResult> activityResultLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.StartActivityForResult());

    public MyActivityForResult<String, Boolean> permissionLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.RequestPermission());

    public MyActivityForResult<Uri, Boolean> takePictureLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.TakePicture());

    public MyActivityForResult<String, Uri> getContentLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.GetContent());

    public MyActivityForResult<String, List<Uri>> mGetMultipleContentLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.GetMultipleContents());

    public MyActivityForResult<String[], List<Uri>> mGetMultipleDocument =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.OpenMultipleDocuments());

    public MyActivityForResult<String[], Map<String, Boolean>> mPermissionsLauncher =
            MyActivityForResult.registerActivityForResult(this, new ActivityResultContracts.RequestMultiplePermissions());

    public abstract void binding();

    public abstract Class<V> getViewModelClass();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding();

        if (getViewModelClass() != null) {
            mViewModel = new ViewModelProvider(this).get(getViewModelClass());
        }

        setContentView(mBinding.getRoot());

        setupObserver();

        firebaseUserManager = FirebaseUserManager.getInstance();
        loggedInUserManager = LoggedInUserManager.getInstance();
        sConnectivityManager = getSystemService(ConnectivityManager.class);

        registerNetworkEventManager();
        registerConnectionStateChanged();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        mPreferences.addListener(onPreferenceChanged);
        firebaseUserManager.addListener(onFirebaseUserChanged);
        loggedInUserManager.addListener(onLoggedInUserChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;

        if (themeChangePending) {
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBinding != null) mBinding = null;
//        mPreferences.removeListener(onPreferenceChanged);
        firebaseUserManager.removeListener(onFirebaseUserChanged);
        loggedInUserManager.removeListener(onLoggedInUserChanged);
        mConnectionStateDisposable.dispose();
    }

    public abstract void setupObserver();

    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }
    public AppPreferences getAppPreference() { return mPreferences; }

    /*
     * OnCurrentUser changed
     * */
    public void reloadFirebaseUser() {
        var executorService = Executors.newSingleThreadExecutor();

        getCurrentUser().reload().addOnCompleteListener(executorService, task -> {
            if (task.isSuccessful()) {
                Logging.show("Reload firebase user successfully");
                firebaseUserManager.emitListener();
            }
        });
    }

    public void updateLoggedInUser(User user) {
        loggedInUserManager.setLoggedInUser(user);
    }

    /*
     * Register event section
     * */
    public void registerFirebaseUserChange(Consumer<FirebaseUser> callback) {
        this.firebaseUserConsumer = callback;
    }

    public void registerLoggedInUserChanged(Consumer<User> callback) {
        this.loggedInUserConsumer = callback;
    }

    protected void registerNetworkEventCallback(OnNetworkChangedListener listener) {
        mOnNetworkChangedListener = listener;
    }

    private void registerNetworkEventManager() {
        sConnectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
//                Logging.show("The default network is now: " + network);
                hasNetwork = true;

                if (mOnNetworkChangedListener != null)
                    mOnNetworkChangedListener.onAvailable();
            }

            @Override
            public void onLost(@NonNull Network network) {
//                Logging.show("The application no longer has a default network. The last default network was " + network);
                hasNetwork = false;

                if (mOnNetworkChangedListener != null)
                    mOnNetworkChangedListener.onLost();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
//                Logging.show("The default network changed capabilities: " + networkCapabilities);
            }

            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
//                Logging.show("The default network changed link properties: " + linkProperties);
            }
        });
    }

    private void registerConnectionStateChanged() {
        BehaviorSubject<WebSocketConnectionState> subject = BehaviorSubject.create();

        AppDependencies.getWebSocket()
                       .getWebSocketState()
                       .distinctUntilChanged()
                       .compose(RxHelper.applyObservableSchedulers())
                       .subscribe(subject);

        mConnectionStateDisposable = subject.debounce(2, TimeUnit.SECONDS)
                                            .compose(RxHelper.applyObservableSchedulers())
                                            .subscribe(this::onConnectionStateChanged, t -> {});
    }

    private void onThemeSettingsModeChange() {
        if (paused) {
            themeChangePending = true;
        } else {
            recreate();
        }
    }

    /*
     * Protected method for derived class
     * */
    public boolean getNetworkStatus() {
        return hasNetwork;
    }

    public void onConnectionStateChanged(WebSocketConnectionState state) {
    }
}
