package com.cylonid.nativealpha.model;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class WebAppInstanceCreator implements InstanceCreator<WebApp>
{
    @Override
    public WebApp createInstance(Type type)
    {
        return new WebApp("", Integer.MAX_VALUE);
    }
}