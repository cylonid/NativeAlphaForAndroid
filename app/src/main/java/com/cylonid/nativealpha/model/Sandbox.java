package com.cylonid.nativealpha.model;

public class Sandbox {
    private int sIndex;
    private String baseUrl;
    private int currently_registered_webapp = -1;
    private static String SANDBOX_NO_BASE_URL_SET = "SANDBOX_NO_BASE_URL";


    public Sandbox(int sIndex) {
        this.sIndex = sIndex;
        this.baseUrl = SANDBOX_NO_BASE_URL_SET;
    }

    public int getCurrentlyRegisteredWebapp() {
        return currently_registered_webapp;
    }

    public boolean isUnoccupied() {
        return currently_registered_webapp == -1 && baseUrl.equals(SANDBOX_NO_BASE_URL_SET);
    }

    public boolean isUsedByAnotherApp(WebApp webapp) {
        if(this.isUnoccupied()) return false;

        return currently_registered_webapp != webapp.getID() || !baseUrl.equals(webapp.getBaseUrl());
    }

    public void registerWebApp(int currentlyRegisteredWebapp, String baseUrl) {
        this.currently_registered_webapp = currentlyRegisteredWebapp;
        this.baseUrl = baseUrl;
    }

    public void unregisterWebApp() {
        this.currently_registered_webapp = -1;
        this.baseUrl = SANDBOX_NO_BASE_URL_SET;
    }

}
