package com.orihistoria.dictado;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "dictado_prefs";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 901;

    private EditText textInput;
    private TextView speedLabel;
    private TextView repeatPauseLabel;
    private TextView nextPauseLabel;
    private TextView fragmentCountLabel;
    private TextView statusLabel;
    private Spinner repetitionsSpinner;
    private SeekBar speedSeekBar;
    private SeekBar repeatPauseSeekBar;
    private SeekBar nextPauseSeekBar;
    private Button pauseResumeButton;

    private boolean servicePaused = false;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshFragmentCount = new Runnable() {
        @Override
        public void run() {
            ArrayList<String> fragments = Segmenter.split(textInput.getText().toString());
            fragmentCountLabel.setText(String.format(
                    Locale.getDefault(),
                    "%d fragmentos: corta automáticamente en comas, punto y coma, dos puntos y finales de oración.",
                    fragments.size()));
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(SpeechService.EXTRA_STATE);
            int current = intent.getIntExtra(SpeechService.EXTRA_CURRENT, 0);
            int total = intent.getIntExtra(SpeechService.EXTRA_TOTAL, 0);
            int reading = intent.getIntExtra(SpeechService.EXTRA_READING, 0);
            int reads = intent.getIntExtra(SpeechService.EXTRA_READS, 0);
            String fragment = intent.getStringExtra(SpeechService.EXTRA_FRAGMENT);
            String message = intent.getStringExtra(SpeechService.EXTRA_MESSAGE);

            if (SpeechService.STATE_PLAYING.equals(state)) {
                servicePaused = false;
                pauseResumeButton.setText("Pausar");
                statusLabel.setText(String.format(
                        Locale.getDefault(),
                        "Leyendo fragmento %d de %d · vez %d de %d\n%s",
                        current,
                        total,
                        reading,
                        reads,
                        fragment == null ? "" : fragment));
            } else if (SpeechService.STATE_PAUSED.equals(state)) {
                servicePaused = true;
                pauseResumeButton.setText("Reanudar");
                statusLabel.setText("Pausado. Al reanudar, vuelve a empezar el fragmento actual.");
            } else if (SpeechService.STATE_FINISHED.equals(state)) {
                servicePaused = false;
                pauseResumeButton.setText("Pausar");
                statusLabel.setText("Texto terminado.");
            } else if (SpeechService.STATE_STOPPED.equals(state)) {
                servicePaused = false;
                pauseResumeButton.setText("Pausar");
                statusLabel.setText("Lectura detenida.");
            } else if (SpeechService.STATE_ERROR.equals(state)) {
                statusLabel.setText(message == null ? "Ocurrió un error de lectura." : message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildInterface();
        restorePreferences();
        handleIncomingText(getIntent());
        registerStateReceiver();
        requestNotificationPermission();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingText(intent);
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException ignored) {
            // El receptor ya no estaba registrado.
        }
        super.onDestroy();
    }

    private void buildInterface() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Dictado por comas");
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Pegá un texto de ChatGPT. La app lo divide sola, repite cada fragmento y sigue hablando con la pantalla apagada.");
        subtitle.setTextSize(16f);
        subtitle.setPadding(0, dp(8), 0, dp(16));
        root.addView(subtitle, matchWrap());

        textInput = new EditText(this);
        textInput.setHint("Pegá acá el texto completo…");
        textInput.setMinLines(10);
        textInput.setGravity(Gravity.TOP | Gravity.START);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(textInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)));

        LinearLayout clipboardRow = horizontalRow();
        Button pasteButton = button("Pegar portapapeles");
        pasteButton.setOnClickListener(v -> pasteClipboard());
        clipboardRow.addView(pasteButton, weighted());
        Button clearButton = button("Limpiar");
        clearButton.setOnClickListener(v -> textInput.setText(""));
        clipboardRow.addView(clearButton, weighted());
        root.addView(clipboardRow, matchWrapWithTop(10));

        fragmentCountLabel = new TextView(this);
        fragmentCountLabel.setPadding(0, dp(8), 0, dp(12));
        root.addView(fragmentCountLabel, matchWrap());

        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                uiHandler.removeCallbacks(refreshFragmentCount);
                uiHandler.postDelayed(refreshFragmentCount, 250);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        addSectionTitle(root, "Velocidad");
        speedLabel = new TextView(this);
        root.addView(speedLabel, matchWrap());
        speedSeekBar = new SeekBar(this);
        speedSeekBar.setMax(15); // 0,5x a 2,0x.
        speedSeekBar.setOnSeekBarChangeListener(simpleSeekListener(progress -> updateSpeedLabel()));
        root.addView(speedSeekBar, matchWrap());

        addSectionTitle(root, "Repeticiones");
        TextView repetitionsHelp = new TextView(this);
        repetitionsHelp.setText("Veces totales que se leerá cada fragmento (no repeticiones adicionales):");
        root.addView(repetitionsHelp, matchWrap());
        repetitionsSpinner = new Spinner(this);
        String[] repetitionOptions = {"1 vez", "2 veces", "3 veces", "4 veces", "5 veces"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                repetitionOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repetitionsSpinner.setAdapter(adapter);
        root.addView(repetitionsSpinner, matchWrap());

        addSectionTitle(root, "Pausas");
        repeatPauseLabel = new TextView(this);
        root.addView(repeatPauseLabel, matchWrap());
        repeatPauseSeekBar = new SeekBar(this);
        repeatPauseSeekBar.setMax(50); // 0 a 5 segundos, pasos de 100 ms.
        repeatPauseSeekBar.setOnSeekBarChangeListener(simpleSeekListener(progress -> updatePauseLabels()));
        root.addView(repeatPauseSeekBar, matchWrap());

        nextPauseLabel = new TextView(this);
        nextPauseLabel.setPadding(0, dp(8), 0, 0);
        root.addView(nextPauseLabel, matchWrap());
        nextPauseSeekBar = new SeekBar(this);
        nextPauseSeekBar.setMax(100); // 0 a 10 segundos.
        nextPauseSeekBar.setOnSeekBarChangeListener(simpleSeekListener(progress -> updatePauseLabels()));
        root.addView(nextPauseSeekBar, matchWrap());

        LinearLayout mainControls = horizontalRow();
        Button playButton = button("Leer desde el inicio");
        playButton.setOnClickListener(v -> startReading());
        mainControls.addView(playButton, weighted());
        pauseResumeButton = button("Pausar");
        pauseResumeButton.setOnClickListener(v -> togglePause());
        mainControls.addView(pauseResumeButton, weighted());
        root.addView(mainControls, matchWrapWithTop(18));

        LinearLayout navigationControls = horizontalRow();
        Button previousButton = button("Anterior");
        previousButton.setOnClickListener(v -> sendSimpleAction(SpeechService.ACTION_PREVIOUS));
        navigationControls.addView(previousButton, weighted());
        Button nextButton = button("Siguiente");
        nextButton.setOnClickListener(v -> sendSimpleAction(SpeechService.ACTION_NEXT));
        navigationControls.addView(nextButton, weighted());
        Button stopButton = button("Detener");
        stopButton.setOnClickListener(v -> sendSimpleAction(SpeechService.ACTION_STOP));
        navigationControls.addView(stopButton, weighted());
        root.addView(navigationControls, matchWrapWithTop(8));

        statusLabel = new TextView(this);
        statusLabel.setText("Listo para pegar un texto.");
        statusLabel.setTextSize(16f);
        statusLabel.setPadding(dp(12), dp(16), dp(12), dp(16));
        root.addView(statusLabel, matchWrapWithTop(12));

        TextView backgroundHelp = new TextView(this);
        backgroundHelp.setText("La lectura usa una notificación permanente para continuar con la pantalla apagada. En algunos celulares conviene permitir uso de batería “Sin restricciones”.");
        backgroundHelp.setPadding(0, dp(10), 0, dp(8));
        root.addView(backgroundHelp, matchWrap());

        Button batteryButton = button("Abrir ajustes de esta app");
        batteryButton.setOnClickListener(v -> openAppSettings());
        root.addView(batteryButton, matchWrap());

        setContentView(scrollView);
    }

    private void restorePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        textInput.setText(prefs.getString("text", ""));
        speedSeekBar.setProgress(prefs.getInt("speed_progress", 3)); // 0,8x.
        repetitionsSpinner.setSelection(Math.max(0, Math.min(4, prefs.getInt("reads", 2) - 1)));
        repeatPauseSeekBar.setProgress(prefs.getInt("repeat_pause", 8)); // 0,8 s.
        nextPauseSeekBar.setProgress(prefs.getInt("next_pause", 25)); // 2,5 s.
        updateSpeedLabel();
        updatePauseLabels();
        refreshFragmentCount.run();
    }

    private void savePreferences() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString("text", textInput.getText().toString())
                .putInt("speed_progress", speedSeekBar.getProgress())
                .putInt("reads", repetitionsSpinner.getSelectedItemPosition() + 1)
                .putInt("repeat_pause", repeatPauseSeekBar.getProgress())
                .putInt("next_pause", nextPauseSeekBar.getProgress())
                .apply();
    }

    private void startReading() {
        String text = textInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Primero pegá un texto.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> fragments = Segmenter.split(text);
        if (fragments.isEmpty()) {
            Toast.makeText(this, "No se encontraron fragmentos para leer.", Toast.LENGTH_SHORT).show();
            return;
        }

        savePreferences();
        Intent intent = new Intent(this, SpeechService.class)
                .setAction(SpeechService.ACTION_PLAY)
                .putExtra(SpeechService.EXTRA_TEXT, text)
                .putExtra(SpeechService.EXTRA_SPEED, getSpeechRate())
                .putExtra(SpeechService.EXTRA_READS, repetitionsSpinner.getSelectedItemPosition() + 1)
                .putExtra(SpeechService.EXTRA_REPEAT_PAUSE_MS, repeatPauseSeekBar.getProgress() * 100L)
                .putExtra(SpeechService.EXTRA_NEXT_PAUSE_MS, nextPauseSeekBar.getProgress() * 100L);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        statusLabel.setText(String.format(
                Locale.getDefault(),
                "Preparando %d fragmentos…",
                fragments.size()));
    }

    private void togglePause() {
        sendSimpleAction(servicePaused ? SpeechService.ACTION_RESUME : SpeechService.ACTION_PAUSE);
    }

    private void sendSimpleAction(String action) {
        Intent intent = new Intent(this, SpeechService.class).setAction(action);
        startService(intent);
    }

    private void pasteClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, "El portapapeles está vacío.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, "No hay texto para pegar.", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence value = clip.getItemAt(0).coerceToText(this);
        if (value == null || value.toString().trim().isEmpty()) {
            Toast.makeText(this, "El contenido copiado no es texto.", Toast.LENGTH_SHORT).show();
            return;
        }
        textInput.setText(value.toString());
        textInput.setSelection(textInput.length());
    }

    private void handleIncomingText(Intent intent) {
        if (intent == null) {
            return;
        }
        CharSequence incoming = null;
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            incoming = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        } else if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
            incoming = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        }

        if (incoming != null && !incoming.toString().trim().isEmpty()) {
            textInput.setText(incoming.toString());
            textInput.setSelection(textInput.length());
            Toast.makeText(this, "Texto recibido.", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerStateReceiver() {
        IntentFilter filter = new IntentFilter(SpeechService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private float getSpeechRate() {
        return 0.5f + (speedSeekBar.getProgress() * 0.1f);
    }

    private void updateSpeedLabel() {
        speedLabel.setText(String.format(
                Locale.getDefault(),
                "Velocidad: %.1fx",
                getSpeechRate()));
    }

    private void updatePauseLabels() {
        repeatPauseLabel.setText(String.format(
                Locale.getDefault(),
                "Pausa antes de repetir el mismo fragmento: %.1f s",
                repeatPauseSeekBar.getProgress() / 10f));
        nextPauseLabel.setText(String.format(
                Locale.getDefault(),
                "Pausa antes de pasar al fragmento siguiente: %.1f s",
                nextPauseSeekBar.getProgress() / 10f));
    }

    private void addSectionTitle(LinearLayout root, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(20f);
        title.setPadding(0, dp(18), 0, dp(6));
        root.addView(title, matchWrap());
    }

    private SeekBar.OnSeekBarChangeListener simpleSeekListener(ProgressCallback callback) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                callback.onProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapWithTop(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface ProgressCallback {
        void onProgress(int progress);
    }
}
