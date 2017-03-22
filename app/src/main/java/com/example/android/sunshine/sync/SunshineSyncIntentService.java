/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.sync;

import android.app.IntentService;
import android.content.Intent;

import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SunshineSyncIntentService extends IntentService {

    private static final String EXTRA_WEARABLE_DATA = "SunshineSyncIntentServiceWearableDataExtra";

    public SunshineSyncIntentService() {
        super("SunshineSyncIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        List<String> wearableDataList =  SunshineSyncTask.syncWeather(getApplicationContext());
        String[] wearableDataStringArray = wearableDataList.toArray(new String[0]);

        Intent syncWearableIntent = new Intent();
        syncWearableIntent.setClass(getApplicationContext(), SunshineSyncWearable.class);
        syncWearableIntent.putExtra(EXTRA_WEARABLE_DATA, wearableDataStringArray);
        startService(syncWearableIntent);
    }
}