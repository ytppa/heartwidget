package com.aga.nothingheart;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

public class HeartWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_HEART_BEAT = "com.aga.nothingheart.action.HEART_BEAT";
    private static final int[] HEARTBEAT_FRAME_RES_IDS = {
            R.drawable.heart_00,
            R.drawable.heart_01,
            R.drawable.heart_02,
            R.drawable.heart_03,
            R.drawable.heart_04,
            R.drawable.heart_05,
            R.drawable.heart_06,
            R.drawable.heart_07,
            R.drawable.heart_08,
            R.drawable.heart_09
    };
    private static final int[] HEARTBEAT_DELAY_MS = {0, 30, 35, 40, 50, 45, 45, 50, 55, 70};
    private static final int DEFAULT_HEART_FRAME_RES_ID = R.drawable.heart_00;
    private static final int MAX_QUEUED_PULSES = 2;
    private static final Object ANIMATION_LOCK = new Object();
    private static boolean isAnimating = false;
    private static int queuedPulseCount = 0;
    private static int[] animationWidgetIds = new int[0];

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, DEFAULT_HEART_FRAME_RES_ID);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_HEART_BEAT.equals(intent.getAction())) {
            HeartRepositories.get(context).sendBeatToPartner();

            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int[] appWidgetIds = {appWidgetId};
                updateWidgetCounterOnly(context, appWidgetIds);
                enqueueHeartbeat(context.getApplicationContext(), appWidgetIds);
            } else {
                int[] appWidgetIds = getAllWidgetIds(context);
                updateWidgetCounterOnly(context, appWidgetIds);
                enqueueHeartbeat(context.getApplicationContext(), appWidgetIds);
            }
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int heartFrameResId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_heart);

        Intent intent = new Intent(context, HeartWidgetProvider.class);
        intent.setAction(ACTION_HEART_BEAT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse("heart-widget://beat/" + appWidgetId));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setImageViewResource(R.id.heart_image, heartFrameResId);
        applyBeatCount(context, views);
        views.setOnClickPendingIntent(R.id.heart_image, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = getAllWidgetIds(context);

        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, DEFAULT_HEART_FRAME_RES_ID);
        }
    }

    private static void applyBeatCount(Context context, RemoteViews views) {
        int count = HeartRepositories.get(context).getReceivedBeatCount();
        String formattedCount = HeartStateStore.formatBeatCount(count);

        views.setTextViewText(R.id.beat_count_badge, formattedCount);
        views.setViewVisibility(R.id.beat_count_badge, count > 0 ? View.VISIBLE : View.GONE);
    }

    private static void updateWidgetCounterOnly(Context context, int[] appWidgetIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_heart);
        applyBeatCount(context, views);
        AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetIds, views);
    }

    private static void enqueueHeartbeat(Context context, int[] appWidgetIds) {
        synchronized (ANIMATION_LOCK) {
            animationWidgetIds = appWidgetIds.clone();

            if (isAnimating) {
                queuedPulseCount = Math.min(MAX_QUEUED_PULSES, queuedPulseCount + 1);
                return;
            }

            isAnimating = true;
            queuedPulseCount = 0;
        }

        new Thread(() -> {
            try {
                while (true) {
                    playOneHeartbeat(context);

                    synchronized (ANIMATION_LOCK) {
                        if (queuedPulseCount <= 0) {
                            isAnimating = false;
                            return;
                        }
                        queuedPulseCount--;
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                synchronized (ANIMATION_LOCK) {
                    isAnimating = false;
                    queuedPulseCount = 0;
                }
            } catch (RuntimeException exception) {
                synchronized (ANIMATION_LOCK) {
                    isAnimating = false;
                    queuedPulseCount = 0;
                }
                throw exception;
            }
        }, "heart-widget-beat").start();
    }

    private static void playOneHeartbeat(Context context) throws InterruptedException {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        for (int i = 0; i < HEARTBEAT_FRAME_RES_IDS.length; i++) {
            int delayMs = HEARTBEAT_DELAY_MS[i];
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }

            int[] appWidgetIds;
            synchronized (ANIMATION_LOCK) {
                appWidgetIds = animationWidgetIds.clone();
            }

            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, HEARTBEAT_FRAME_RES_IDS[i]);
            }
        }
    }

    private static int[] getAllWidgetIds(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, HeartWidgetProvider.class);
        return appWidgetManager.getAppWidgetIds(widget);
    }
}
