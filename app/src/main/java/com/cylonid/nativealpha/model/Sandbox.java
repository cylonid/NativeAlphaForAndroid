package com.cylonid.nativealpha.model;

public class Sandbox {
    private int sID;

    public Sandbox(int sID) {
        this.sID = sID;
    }

    public int getSID() {
        return sID;
    }

    public boolean isUsed() {
        for (WebApp webapp : DataManager.getInstance().getActiveWebsites()) {
            if (webapp.getContainerId() == this.sID)
                return true;
        }
        return false;
    }

    public String getLabel() {
        return String.valueOf(sID);
    }

}
