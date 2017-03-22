/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final String TAG = SunshineWatchFace.class.getSimpleName();

    private static final Typeface NORMAL_TYPEFACE =
          Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private static final long UPDATE_DELAY = 60000;

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (obtainMessage().what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "Updateing Time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_DELAY);
                        }
                }
            }
        }; //new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTimeTextPaint;
        Paint mDateTextPaint;
        Paint mTempTextPaint;
        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffset;
        float mYOffset;
        float mYDateOffset;
        float mYTempOffset;
        boolean mIsRound;

        String mTempText;
        Bitmap mWeatherIcon;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Resources resources = SunshineWatchFace.this.getResources();
            setSyncObjects(resources);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                  .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                  .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                  .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                  .setShowSystemUiTime(false)
                  .setAcceptsTapEvents(true)
                  .build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.colorPrimary));

            mTimeTextPaint = new Paint();
            mTimeTextPaint = createTextPaint(resources.getColor(R.color.white));

            mDateTextPaint = new Paint();
            mDateTextPaint = createTextPaint(resources.getColor(R.color.activated));

            mTempTextPaint = new Paint();
            mTempTextPaint = createTextPaint(resources.getColor(R.color.white));

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void setSyncObjects(Resources resources) {
            mTempText = PrefUtils.getSyncTemp(getApplicationContext());
            String iconName = PrefUtils.getSyncIconName(getApplicationContext());
            final int resourceId = resources.getIdentifier(iconName, "drawable",
                  getApplicationContext().getPackageName());
            mWeatherIcon = BitmapFactory
                  .decodeResource(getApplicationContext().getResources(),resourceId);
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mIsRound = insets.isRound();

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mYDateOffset = resources.getDimension(R.dimen.date_y_offset);
            mYTempOffset = resources.getDimension(R.dimen.temp_y_offset);
//            mXOffset = resources.getDimension(isRound
//                  ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float timeTextSize = resources.getDimension(isRound
                  ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            float dateTextSize = resources.getDimension(isRound
                  ? R.dimen.date_text_size_round : R.dimen.date_text_size);

            float tempTextSize = resources.getDimension(isRound
                  ? R.dimen.temp_text_size_round : R.dimen.temp_text_size);

            mTimeTextPaint.setTextSize(timeTextSize);
            mTempTextPaint.setTextSize(tempTextSize);
            mDateTextPaint.setTextSize(dateTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            boolean syncIsWaiting = PrefUtils.getSyncIsWaitingBoolean(getApplicationContext());

            //Log.d("onTimeTick ", "syncIsWaiting: " + String.valueOf(syncIsWaiting));
            if (syncIsWaiting) {
                setSyncObjects(SunshineWatchFace.this.getResources());
                PrefUtils.setSyncIsWaitingBoolean(getApplicationContext(), false);
            }

            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTimeTextPaint.setAntiAlias(!inAmbientMode);
                    mDateTextPaint.setAntiAlias(!inAmbientMode);
                    mTempTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                          .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            float xFactor;
            if (mIsRound) {
                xFactor = 8.5f;
            } else {
                xFactor = 5f;
            }

            mXOffset = bounds.width()/xFactor;
            float xTimeOffsetFactor;
            float xTempOffsetFactor;
            mYOffset = bounds.height()/3f;

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Get system preference of hour format and apply
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            //Need to cast to Integer for logical comparison with .equals
            Integer intPm = Calendar.PM;
            Integer curHour = mCalendar.get(Calendar.HOUR);
            boolean is24h = DateFormat.is24HourFormat(getApplicationContext());
            boolean isPm = intPm.equals(mCalendar.get(Calendar.AM_PM));
            boolean hourIs10_11_12 = curHour.equals(10) | curHour.equals(11) | curHour.equals(0);

            String hourFormat = is24h?"H":"h";

            if ((is24h & isPm) | (hourIs10_11_12)) {
                xTimeOffsetFactor = 3.3f;
            } else {
                xTimeOffsetFactor = 2.8f;
            }
            float timeXOffset = bounds.width()/xTimeOffsetFactor;

            String timeText = new SimpleDateFormat(hourFormat + ":mm", Locale.getDefault())
                    .format(mCalendar.getTime());



            float weatherIconX = (bounds.width() - mWeatherIcon.getWidth())/2f;
            float weatherIconY = bounds.width()*.65f;


            int tempTextLength = mTempText.length();

            xTempOffsetFactor = getXOffsetFactor(mIsRound, tempTextLength);



            float tempXOffset = bounds.width()/xTempOffsetFactor;

            String dateText = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(mCalendar.getTime());
            canvas.drawText(timeText, timeXOffset, mYOffset, mTimeTextPaint);
            canvas.drawText(dateText, mXOffset, mYOffset + mYDateOffset, mDateTextPaint);
            canvas.drawText(mTempText, tempXOffset, mYOffset + mYTempOffset, mTempTextPaint);
            canvas.drawBitmap(mWeatherIcon, weatherIconX, weatherIconY, null);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                      - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private float getXOffsetFactor(boolean isRound, int textLength) {
            if (!isRound) {
                switch (textLength) {
                    case 11:
                        return 4.06f;
                    case 10:
                        return 3.86f;
                    case 9:
                        return 3.53f;
                    case 8:
                        return 3.26f;
                    case 7:
                        return 2.95f;
                    default:
                        return 3.53f;
                }
            } else {
                switch (textLength) {
                    case 11:
                        return 3.7f;
                    case 10:
                        return 3.4f;
                    case 9:
                        return 3.1f;
                    case 8:
                        return 2.8f;
                    case 7:
                        return 2.5f;
                    default:
                        return 3.53f;
                }
            }
        }
    }
}
