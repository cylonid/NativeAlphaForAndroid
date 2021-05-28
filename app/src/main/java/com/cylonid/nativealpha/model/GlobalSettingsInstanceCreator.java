package com.cylonid.nativealpha.model;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class GlobalSettingsInstanceCreator implements InstanceCreator<GlobalSettings>
{
    @Override
    public GlobalSettings createInstance(Type type)
    {
        return new GlobalSettings();
    }
}