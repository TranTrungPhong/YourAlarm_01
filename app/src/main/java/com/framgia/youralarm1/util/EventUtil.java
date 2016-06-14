package com.framgia.youralarm1.util;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.activity.MainActivity;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.models.ItemAlarm;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by phongtran on 13/06/2016.
 */
public class EventUtil {
    private Context mContext;
    private GoogleAccountCredential mCredential;

    public EventUtil(Context mContext, GoogleAccountCredential credential) {
        this.mContext = mContext;
        this.mCredential = credential;
    }


    public String getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            return Const.NO_ACCOUNT;
        } else if (!isDeviceOnline()) {
            return Const.NO_INTERNET;
        }
        return Const.ACCOUNT_SUCCESS;
    }

    public boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(mContext);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    public void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(mContext);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    public void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                (MainActivity) mContext.getApplicationContext(),
                connectionStatusCode,
                Const.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public void insertEvent(ItemAlarm itemAlarm) {
        //TODO add 1 Event vao google api calendar
    }

    public void deleteEvent(ItemAlarm itemAlarm) {
        //TODO xoa 1 event tren calendar
    }

    public List<ItemAlarm> loadEvent() {
        List<ItemAlarm> alarmList = new ArrayList<>();
        //TODO load event tu calendar
        return alarmList;
    }

    public void updateEvent(ItemAlarm itemAlarm) {
        //TODO update ...
    }
}
