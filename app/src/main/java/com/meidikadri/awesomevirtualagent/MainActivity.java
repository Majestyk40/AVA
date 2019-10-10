package com.meidikadri.awesomevirtualagent;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
// Pour cet import, pensez à ajouter dans Gradle (app) la dépendance ci dessous:
// implementation 'org.apache.commons:commons-lang3:3.6'

public class MainActivity extends AppCompatActivity {

    //////// START OF VOICE RECOGNITION CODE
    private static final int RECORD_REQUEST_CODE = 101;

    private static final String TAG = MainActivity.class.getName();

    // WakeLock pour garder l'OS éveillé
    protected PowerManager.WakeLock mWakeLock;

    // RecVoc pour les mots clés
    private SpeechRecognizer mSpeechRecognizer;
    // recVoc pour les requetes
    private SpeechRecognizer requestSpeechRec;
    // RecVoc pour les dates
    private SpeechRecognizer dateSpeechRec;

    //handler to post changes to progress bar
    private Handler mHandler = new Handler();

    //intent for speech recogniztion
    Intent mSpeechIntent;
    // intent for article recognition
    Intent mSpeechArticleIntent;

    //this bool will record that it's time to kill P.A.L.
    boolean killCommanded = false;

    //legel commands
    public static final String[] VALID_COMMANDS = {
            "yo ava",
            "salut",
    };
    public static final int VALID_COMMANDS_SIZE = VALID_COMMANDS.length;

    TextView tvWakeUp;
    Switch modeEcoute;
    //////// END OF VOICE RECOGNITION CODE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("AVA > Your Awesome Virtual Agent");

        //////// START OF VOICE RECOGNITION CODE
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }

        tvWakeUp = findViewById(R.id.tv_ecoute);
        requestSpeechRec = SpeechRecognizer.createSpeechRecognizer(this);
        //////// END OF VOICE RECOGNITION CODE


    }

    //////// START OF VOICE RECOGNITION CODE
    private void speechRec(){
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        SpeechListener mRecognitionListener = new SpeechListener();
        mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
        mSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.meidikadri.awesomevirtualagent.MainActivity");

        // Given an hint to the recognizer about what the user is going to say
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Specify how many results you want to receive. The results will be sorted
        // where the first result is the one with higher confidence.
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);

        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        mSpeechRecognizer.startListening(mSpeechIntent);
    }

    @Override
    public void onStart() {
        super.onStart();
        modeEcoute = findViewById(R.id.swEcoute);
        modeEcoute.setChecked(false);
        modeEcoute.setTextOff("Off");
        modeEcoute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bChecked) {
                if (bChecked) {
                    speechRec();
                    modeEcoute.setTextOn("On");
                }else{
                    if (mSpeechRecognizer != null) {
                        mSpeechRecognizer.destroy();
                        mSpeechRecognizer = null;
                    }
                    modeEcoute.setTextOff("Off");
                }
            }
        });
        //aqcuire the wakelock to keep the screen on until user exits/closes app
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        this.mWakeLock.acquire();
    }

    @Override
    public void onPause() {
        //kill the voice recognizer
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
        //this.mWakeLock.release();

        super.onPause();
    }

    private void processCommand(ArrayList<String> matchStrings) {
        String response = "Désolé, je ne comprends pas";
        int maxStrings = matchStrings.size();
        boolean resultFound = false;
        for (int i = 0; i < VALID_COMMANDS_SIZE && !resultFound; i++) {
            for (int j = 0; j < maxStrings && !resultFound; j++) {
                if (StringUtils.getLevenshteinDistance(matchStrings.get(j), VALID_COMMANDS[i]) < (VALID_COMMANDS[i].length() / 3)) {
                    response = getResponse(i);
                }
            }
        }

        final String finalResponse = response;
        mHandler.post(new Runnable() {
            public void run() {
                //tvText.setText(finalResponse);
            }
        });
    }

    class SpeechListener implements RecognitionListener {
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "buffer recieved ");
        }

        public void onError(int error) {
            //if critical error then exit
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                Log.d(TAG, "client error");
            }
            //else ask to repeats
            else {
                Log.d(TAG, "other error");
                mSpeechRecognizer.startListening(mSpeechIntent);
            }
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent");
        }

        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "partial results");
        }

        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "on ready for speech");
        }

        public void onResults(Bundle results) {
            Log.d(TAG, "on results");
            ArrayList<String> matches = null;
            if (results != null) {
                matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    Log.d(TAG, "results are " + matches.toString());
                    //tvText.setText(matches.toString());
                    processCommand(matches);
                    if (!killCommanded)
                        mSpeechRecognizer.startListening(mSpeechIntent);
                    else
                        finish();
                }
            }
        }

        public void onRmsChanged(float rmsdB) {
            //			Log.d(TAG, "rms changed");
        }

        public void onBeginningOfSpeech() {
            Log.d(TAG, "speach begining");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "speach done");
        }
    }

    private String getResponse(int command) {
        String retString = "Je ne comprends pas";
        switch (command) {
            case 0:
                initRequestRecognizer();
                tvWakeUp.setText("Je vous écoute ?");
                tvWakeUp.setTypeface(null, Typeface.BOLD);
                mSpeechRecognizer.destroy();
                break;

            case 1:
                killCommanded = true;
                break;

            default:
                break;
        }
        return retString;
    }

    // MODE DE RECONNAISSANCE VOCALE SANS GOOGLE DIALOG
    private void initRequestRecognizer() {
        Intent intentA = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentA.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intentA.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intentA.putExtra(RecognizerIntent.EXTRA_PROMPT, "Je vous écoute");

        requestSpeechRec.startListening(intentA);

        if (SpeechRecognizer.isRecognitionAvailable(this)){
            requestSpeechRec.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    List<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    tvWakeUp.setText(result.get(0));
                    tvWakeUp.setTypeface(null, Typeface.NORMAL);
                    speechRec();
                    //processResult(result.get(0));
                }

                @Override
                public void onPartialResults(Bundle partialResults) {

                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }

    }
    // FIN MODE DE RECONNAISSANCE VOCALE SANS GOOGLE DIALOG

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }
    //////// END OF VOICE RECOGNITION CODE

}
