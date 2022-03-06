package com.cylonid.nativealpha.model;

import android.util.Log;
import androidx.annotation.NonNull;

import com.cylonid.nativealpha.BuildConfig;

import java.util.stream.IntStream;

public class SandboxManager {

    private static SandboxManager instance = null;
    private static final int NUM_OF_SANDBOXES = 8;
    private final Sandbox[] sandboxes = new Sandbox[NUM_OF_SANDBOXES];
    private int next_container;

    private SandboxManager()
    {
        next_container = 0;
        IntStream.range(0, NUM_OF_SANDBOXES).forEach(x -> sandboxes[x] = new Sandbox(x));
    }

    public static SandboxManager getInstance() {
        if (BuildConfig.FLAVOR.equals("extended")) {
            instance = instance == null ? new SandboxManager() : instance;
            return instance;
        }
        return null;
    }

    public int calculateNextFreeContainerId() {
        if (next_container == NUM_OF_SANDBOXES) {
            next_container = 0;
        }
        return next_container++;
    }

    public void registerWebAppToSandbox(WebApp webapp) {
        int sId = webapp.getContainerId();
        sandboxes[sId].registerWebApp(webapp.getID(), webapp.getBaseUrl());
        Log.d("SBX ", "Sandbox "  + webapp.getContainerId() + " is occupied by " + webapp.getTitle());
    }

    public void unregisterWebAppFromSandbox(int sandboxId) {
        if(!sandboxes[sandboxId].isUnoccupied()) {
            WebApp currently_set_webapp = DataManager.getInstance().getWebApp(sandboxes[sandboxId].getCurrentlyRegisteredWebapp());
            sandboxes[sandboxId].unregisterWebApp();
            Log.d("SBX", "Sandbox " + sandboxId + " is left by " + currently_set_webapp.getTitle());
        }
    }

    public boolean isSandboxUsedByAnotherApp(@NonNull WebApp webapp) {
        int sId = webapp.getContainerId();
        return sandboxes[sId].isUsedByAnotherApp(webapp);
    }

    public int getNextContainer() {
        return next_container;
    }

    public void setNextContainer(int nextContainer) {
        this.next_container = nextContainer;
    }

}

