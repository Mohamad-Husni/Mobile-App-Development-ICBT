package com.example.printxpress;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class PrintXpressApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Queue Realtime Database writes/reads locally and sync automatically
        // once connectivity returns, instead of failing outright when offline.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (IllegalStateException alreadyConfigured) {
            // setPersistenceEnabled() may only be called once per process.
        }
    }
}
