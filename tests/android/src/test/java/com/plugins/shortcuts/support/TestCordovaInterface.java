package com.plugins.shortcuts.support;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

public class TestCordovaInterface implements CordovaInterface {

    private final AppCompatActivity activity;
    private final ExecutorService threadPool = new SameThreadExecutorService();

    public TestCordovaInterface(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public AppCompatActivity getActivity() {
        return activity;
    }

    @Override
    public Context getContext() {
        return activity;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    @Override
    public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
    }

    @Override
    public void setActivityResultCallback(CordovaPlugin plugin) {
    }

    @Override
    public Object onMessage(String id, Object data) {
        return null;
    }

    @Override
    public void requestPermission(CordovaPlugin plugin, int requestCode, String permission) {
    }

    @Override
    public void requestPermissions(CordovaPlugin plugin, int requestCode, String[] permissions) {
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    private static class SameThreadExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown = false;

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }
    }
}
