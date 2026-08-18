package com.orihistoria.dictado;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_PLAY = "com.orihistoria.dictado.action.PLAY";
    public static final String ACTION_PAUSE = "com.orihistoria.dictado.action.PAUSE";
    public static final String ACTION_RESUME = "com.orihistoria.dictado.action.RESUME";
    public static final String ACTION_STOP = "com.orihistoria.dictado.action.STOP";
    public static final String ACTION_NEXT = "com.orihistoria.dictado.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.orihistoria.dictado.action.PREVIOUS";
    public static final String ACTION_STATE = "com.orihistoria.dictado.action.STATE";

    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_SPEED = "extra_speed";
    public static final String EXTRA_READS = "extra_reads";
    public static final String EXTRA_REPEAT_PAUSE_MS = "extra_repeat_pause_ms";
    public static final String EXTRA_NEXT_PAUSE_MS = "extra_next_pause_ms";
    public static final String EXTRA_STATE = "extra_state";
    public static final String EXTRA_CURRENT = "extra_current";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_READING = "extra_reading";
    public static final String EXTRA_FRAGMENT = "extra_fragment";
    public static final String EXTRA_MESSAGE = "extra_message";

    public static final String STATE_PLAYING = "playing";
    public static final String STATE_PAUSED = "paused";
    public static final String STATE_FINISHED = "finished";
    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_ERROR = "error";

    private static final String CHANNEL_ID = "dictado_playback";
    private static final int NOTIFICATION_ID = 1407;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<String> segments = new ArrayList<>();

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean running = false;
    private boolean paused = false;
    private boolean pausedByAudioFocus = false;
    private int segmentIndex = 0;
    private int completedReadsForSegment = 0;
    private int totalReads = 2;
    private float speechRate = 0.8f;
    private long repeatPauseMs = 800L;
    private long nextPauseMs = 2500L;
    private long utteranceCounter = 0L;
    private String activeUtteranceId;

    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            handler.post(() -> {
                if (running && !paused) {
                    pausedByAudioFocus = true;
                    pauseInternal();
                }
            });
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            handler.post(() -> {
                if (pausedByAudioFocus && running) {
                    pausedByAudioFocus = false;
                    resumeInternal();
                }
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    getPackageName() + ":dictado");
            wakeLock.setReferenceCounted(false);
        }
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        tts = new TextToSpeech(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY:
                beginNewSession(intent);
                break;
            case ACTION_PAUSE:
                pauseInternal();
                break;
            case ACTION_RESUME:
                resumeInternal();
                break;
            case ACTION_NEXT:
                moveToSegment(1);
                break;
            case ACTION_PREVIOUS:
                moveToSegment(-1);
                break;
            case ACTION_STOP:
                stopSession(false);
                break;
            default:
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false;
            broadcastError("No se pudo iniciar el motor de voz del teléfono.");
            stopSession(false);
            return;
        }

        int languageResult = tts.setLanguage(new Locale("es", "AR"));
        if (languageResult == TextToSpeech.LANG_MISSING_DATA
                || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            languageResult = tts.setLanguage(new Locale("es", "ES"));
        }
        if (languageResult == TextToSpeech.LANG_MISSING_DATA
                || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            broadcastError("El motor de voz instalado no tiene una voz en español.");
        }

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                handler.post(() -> {
                    if (utteranceId.equals(activeUtteranceId)) {
                        updateNotification();
                        broadcastPlaying();
                    }
                });
            }

            @Override
            public void onDone(String utteranceId) {
                handler.post(() -> completeUtterance(utteranceId));
            }

            @Override
            public void onError(String utteranceId) {
                handler.post(() -> completeUtterance(utteranceId));
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                handler.post(() -> completeUtterance(utteranceId));
            }
        });

        ttsReady = true;
        if (running && !paused) {
            speakCurrent();
        }
    }

    private void beginNewSession(Intent intent) {
        String text = intent.getStringExtra(EXTRA_TEXT);
        ArrayList<String> split = Segmenter.split(text);
        if (split.isEmpty()) {
            broadcastError("El texto no contiene fragmentos para leer.");
            stopSelf();
            return;
        }

        invalidateCurrentUtterance();
        if (tts != null) {
            tts.stop();
        }
        handler.removeCallbacksAndMessages(null);

        segments.clear();
        segments.addAll(split);
        segmentIndex = 0;
        completedReadsForSegment = 0;
        totalReads = Math.max(1, Math.min(5, intent.getIntExtra(EXTRA_READS, 2)));
        speechRate = Math.max(0.3f, Math.min(2.0f, intent.getFloatExtra(EXTRA_SPEED, 0.8f)));
        repeatPauseMs = Math.max(0L, intent.getLongExtra(EXTRA_REPEAT_PAUSE_MS, 800L));
        nextPauseMs = Math.max(0L, intent.getLongExtra(EXTRA_NEXT_PAUSE_MS, 2500L));
        running = true;
        paused = false;
        pausedByAudioFocus = false;

        startAsForeground();
        acquireWakeLock();
        requestAudioFocus();

        if (ttsReady) {
            speakCurrent();
        }
    }

    private void speakCurrent() {
        if (!running || paused || !ttsReady || tts == null || segments.isEmpty()) {
            return;
        }
        if (segmentIndex < 0 || segmentIndex >= segments.size()) {
            finishSession();
            return;
        }

        handler.removeCallbacksAndMessages(null);
        acquireWakeLock();
        tts.setSpeechRate(speechRate);

        activeUtteranceId = "dictado_" + (++utteranceCounter);
        Bundle parameters = new Bundle();
        parameters.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
        int result = tts.speak(
                segments.get(segmentIndex),
                TextToSpeech.QUEUE_FLUSH,
                parameters,
                activeUtteranceId);

        if (result == TextToSpeech.ERROR) {
            String failedId = activeUtteranceId;
            handler.postDelayed(() -> completeUtterance(failedId), 250L);
        } else {
            updateNotification();
            broadcastPlaying();
        }
    }

    private void completeUtterance(String utteranceId) {
        if (!running || paused || utteranceId == null || !utteranceId.equals(activeUtteranceId)) {
            return;
        }
        activeUtteranceId = null;
        completedReadsForSegment++;

        if (completedReadsForSegment < totalReads) {
            handler.postDelayed(this::speakCurrent, repeatPauseMs);
            return;
        }

        completedReadsForSegment = 0;
        segmentIndex++;
        if (segmentIndex >= segments.size()) {
            finishSession();
        } else {
            handler.postDelayed(this::speakCurrent, nextPauseMs);
        }
    }

    private void pauseInternal() {
        if (!running || paused) {
            return;
        }
        paused = true;
        invalidateCurrentUtterance();
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
        }
        releaseWakeLock();
        abandonAudioFocus();
        updateNotification();
        broadcastState(STATE_PAUSED, null);
    }

    private void resumeInternal() {
        if (!running || !paused) {
            return;
        }
        paused = false;
        acquireWakeLock();
        requestAudioFocus();
        updateNotification();
        speakCurrent();
    }

    private void moveToSegment(int offset) {
        if (!running || segments.isEmpty()) {
            return;
        }
        invalidateCurrentUtterance();
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
        }
        segmentIndex = Math.max(0, Math.min(segments.size() - 1, segmentIndex + offset));
        completedReadsForSegment = 0;
        paused = false;
        pausedByAudioFocus = false;
        acquireWakeLock();
        requestAudioFocus();
        speakCurrent();
    }

    private void finishSession() {
        if (!running) {
            return;
        }
        running = false;
        paused = false;
        invalidateCurrentUtterance();
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
        }
        releaseWakeLock();
        abandonAudioFocus();
        broadcastState(STATE_FINISHED, null);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void stopSession(boolean fromDestroy) {
        boolean wasRunning = running;
        running = false;
        paused = false;
        pausedByAudioFocus = false;
        invalidateCurrentUtterance();
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
        }
        releaseWakeLock();
        abandonAudioFocus();
        if (wasRunning && !fromDestroy) {
            broadcastState(STATE_STOPPED, null);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        if (!fromDestroy) {
            stopSelf();
        }
    }

    private void invalidateCurrentUtterance() {
        activeUtteranceId = null;
        utteranceCounter++;
    }

    private void startAsForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = paused ? "Dictado pausado" : "Dictado por comas";
        String content = currentNotificationText();

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(contentIntent)
                .setOngoing(running)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(
                        android.R.drawable.ic_media_previous,
                        "Anterior",
                        servicePendingIntent(ACTION_PREVIOUS, 201))
                .addAction(
                        paused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause,
                        paused ? "Reanudar" : "Pausar",
                        servicePendingIntent(paused ? ACTION_RESUME : ACTION_PAUSE, 202))
                .addAction(
                        android.R.drawable.ic_media_next,
                        "Siguiente",
                        servicePendingIntent(ACTION_NEXT, 203))
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Detener",
                        servicePendingIntent(ACTION_STOP, 204));

        return builder.build();
    }

    private void updateNotification() {
        if (!running) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private String currentNotificationText() {
        if (segments.isEmpty() || segmentIndex < 0 || segmentIndex >= segments.size()) {
            return "Preparando lectura…";
        }
        String fragment = segments.get(segmentIndex);
        String prefix = String.format(
                Locale.getDefault(),
                "%d/%d · vez %d/%d — ",
                segmentIndex + 1,
                segments.size(),
                Math.min(totalReads, completedReadsForSegment + 1),
                totalReads);
        if (fragment.length() > 120) {
            fragment = fragment.substring(0, 117) + "…";
        }
        return prefix + fragment;
    }

    private PendingIntent servicePendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, SpeechService.class).setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Lectura de dictado",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Controles para la lectura con la pantalla apagada");
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    private void broadcastPlaying() {
        broadcastState(STATE_PLAYING, null);
    }

    private void broadcastError(String message) {
        broadcastState(STATE_ERROR, message);
    }

    private void broadcastState(String state, String message) {
        Intent intent = new Intent(ACTION_STATE)
                .setPackage(getPackageName())
                .putExtra(EXTRA_STATE, state)
                .putExtra(EXTRA_MESSAGE, message)
                .putExtra(EXTRA_CURRENT, segments.isEmpty() ? 0 : segmentIndex + 1)
                .putExtra(EXTRA_TOTAL, segments.size())
                .putExtra(EXTRA_READING, Math.min(totalReads, completedReadsForSegment + 1))
                .putExtra(EXTRA_READS, totalReads)
                .putExtra(EXTRA_FRAGMENT, currentFragment());
        sendBroadcast(intent);
    }

    private String currentFragment() {
        if (segments.isEmpty() || segmentIndex < 0 || segmentIndex >= segments.size()) {
            return "";
        }
        return segments.get(segmentIndex);
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(6L * 60L * 60L * 1000L);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void requestAudioFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build();
            }
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopSession(true);
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}
