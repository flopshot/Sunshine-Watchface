package com.example.android.sunshine.sync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class SunshineSyncWearable extends Service implements
      GoogleApiClient.ConnectionCallbacks,
      GoogleApiClient.OnConnectionFailedListener {

    private static final String EXTRA_WEARABLE_DATA = "SunshineSyncIntentServiceWearableDataExtra";
    private static final String KEY_HI_LOW_TEMP = "SunshineSyncWearableHi";
    private static final String KEY_WEATHER_IMAGE = "SunshineSyncWearableWeatherImage";
    private static final String DATA_SYNC_MAP_PATH = "/sunshinewatchface";

    GoogleApiClient mGoogleClientApi;
    String[] mWearableDataStringArray;

    public SunshineSyncWearable() {
    }

    @Override
    public void onCreate() {
        mGoogleClientApi = new GoogleApiClient.Builder(this)
              .addApi(Wearable.API)
              .addConnectionCallbacks(this)
              .addOnConnectionFailedListener(this)
              .build();
        mGoogleClientApi.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWearableDataStringArray = intent.getExtras().getStringArray(EXTRA_WEARABLE_DATA);
        return START_NOT_STICKY;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        new DataTask (getApplicationContext()).execute(mWearableDataStringArray);
    }

    private class DataTask  extends AsyncTask<String[], Void, Void> {
        Context c;

        DataTask (Context c) {
            this.c = c;
        }

        @Override
        protected Void doInBackground(String[]... params) {
            String[] wearableDataStringArray = params[0] ;

            double wearableDataHi = Double.valueOf(wearableDataStringArray[0]);
            double wearableDataLow = Double.valueOf(wearableDataStringArray[1]);
            int wearableDataWeatherId = Integer.valueOf(wearableDataStringArray[2]);

            String wearableHiLow = SunshineWeatherUtils.formatHighLows(getApplicationContext(), wearableDataHi, wearableDataLow);

            int wearableImageId = SunshineWeatherUtils
                        .getSmallArtResourceIdForWeatherCondition(wearableDataWeatherId);

            String weatherIconName
                  = getApplicationContext().getResources().getResourceEntryName(wearableImageId);

            PutDataMapRequest dataMap = PutDataMapRequest.create(DATA_SYNC_MAP_PATH);
            dataMap.getDataMap().putString(KEY_HI_LOW_TEMP, wearableHiLow);
            dataMap.getDataMap().putString(KEY_WEATHER_IMAGE, weatherIconName);

            PutDataRequest request = dataMap.asPutDataRequest();

            Wearable.DataApi.putDataItem(mGoogleClientApi, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess()) {
                                Log.d(this.getClass().getSimpleName(), "Successfully Sent Data to Wear");
                            } else {
                                Log.e(this.getClass().getSimpleName(), "Failed to Send Data to Wear");
                            }
                        }
                    });

            return null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
