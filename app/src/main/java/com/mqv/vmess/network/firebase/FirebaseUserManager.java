package com.mqv.vmess.network.firebase;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FirebaseUserManager {
    private final ListenerRegistry listenerRegistry;
    private static FirebaseUserManager instance;

    public static FirebaseUserManager getInstance(){
        if (instance == null){
            instance = new FirebaseUserManager();
        }
        return instance;
    }

    private FirebaseUserManager() {
        this.listenerRegistry = new ListenerRegistry();
    }

    public interface Listener {
        void onUserChanged();
    }

    static class ListenerRegistry {
        private final Set<Listener> listeners;

        public ListenerRegistry() {
            this.listeners = new CopyOnWriteArraySet<>();
        }

        public void add(Listener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        public void remove(Listener listener) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        public void emitListener(){
            for (var l : listeners){
                l.onUserChanged();
            }
        }
    }

    public void addListener(Listener listener) {
        this.listenerRegistry.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listenerRegistry.remove(listener);
    }

    public void emitListener(){
        this.listenerRegistry.emitListener();
    }
}
