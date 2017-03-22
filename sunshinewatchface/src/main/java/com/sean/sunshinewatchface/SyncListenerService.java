package com.sean.sunshinewatchface;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class SyncListenerService extends WearableListenerService {

    private static final String KEY_HI_LOW_TEMP = "SunshineSyncWearableHi";
    private static final String KEY_WEATHER_IMAGE = "SunshineSyncWearableWeatherImage";
    private static final String DATA_SYNC_MAP_PATH = "/sunshinewatchface";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent:dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (path.equals(DATA_SYNC_MAP_PATH)) {
                    String hiLowTemp = dataMap.getString(KEY_HI_LOW_TEMP);
                    String weatherIconName = dataMap.getString(KEY_WEATHER_IMAGE);

                    Log.d("SyncListener:", " Started");


                    PrefUtils.setSyncIsWaitingBoolean(getApplicationContext(), true);
                    PrefUtils.setSyncTemp(getApplicationContext(), hiLowTemp);
                    PrefUtils.setSyncIconName(getApplicationContext(), weatherIconName);
                }
            }
        }
    }
}
