package com.simonschwieler.speech_to_text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements SensorEventListener {

    protected static final int RESULT_SPEECH = 1;
    private ImageButton btnSpeak;
    private TextView Text;
    private String com = null;
    private static final DateFormat sdf = new SimpleDateFormat("HH:mm");
    private TextToSpeech t1;
    private SensorManager mSensorManager;
    private Sensor mTemperature;
    private float ambient_temperature = 0;
    private Calendar cal = Calendar.getInstance();
    private Intent intent = new Intent(Intent.ACTION_EDIT);
    private boolean temperatureBoolean = false;
    private boolean timeBoolean = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Text = (TextView) findViewById(R.id.Text);
        btnSpeak = (ImageButton) findViewById(R.id.mic);
        com = run("tdtool -l");

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        }
        if (mTemperature == null) {
        }

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    Text.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "No support for STT",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        if (t1 != null) {
            t1.stop();
            //  t1.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ambient_temperature = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Accuracy changes
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String question = "Do you want to turn on the heater?";
        Date date = new Date();
        String toSpeak = sdf.format(date);
        //  int time = (int)sdf.format(date);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String alternative = text.get(0).toLowerCase();

                    switch (alternative) {


                        case "yes":
                            if (ambient_temperature < 25 && temperatureBoolean) {
                                run("tdtool --on 4");
                                temperatureBoolean = false;
                            }
                            try {
                                if (isTimeBetweenTwoTime("07:00:00", "11:00:00", currentTime()) && timeBoolean){
                                    run("tdtool --on 2");
                                    timeBoolean = false;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            break;

                        case "status":
                            com = run("tdtool -l");
                            statusMethod(com);
                            break;
                        case "lights off":
                            run("tdtool --off 4");
                            break;
                        case "lights":
                            run("tdtool --on 4");
                            break;
                        case "heater":
                            run("tdtool --on 2");
                            break;
                        case "heater off":
                            run("tdtool --off 2");
                            break;
                        case "time":

                            timeBoolean = true;
                            Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
                            t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

                            try {
                                TimeUnit.SECONDS.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent(
                                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());

                            try {
                                startActivityForResult(intent, RESULT_SPEECH);

                            } catch (ActivityNotFoundException a) {
                                Toast.makeText(getApplicationContext(),
                                        "No support for STT",
                                        Toast.LENGTH_SHORT).show();
                            }


                            break;
                        case "temperature":

                            temperatureBoolean = true;
                            ambient_temperature = (float) ((int) (ambient_temperature * 10f)) / 10f;
                            Log.d(TAG, " myFloat = " + ambient_temperature);
                            Toast.makeText(getApplicationContext(), ambient_temperature +
                                    getResources().getString(R.string.celsius), Toast.LENGTH_SHORT).show();
                            t1.speak(String.valueOf(ambient_temperature) +
                                    getResources().getString(R.string.celsius), TextToSpeech.QUEUE_FLUSH, null);

                            if (ambient_temperature < 25) {
                                t1.speak(question, TextToSpeech.QUEUE_ADD, null);

                                try {
                                    TimeUnit.SECONDS.sleep(5);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                intent = new Intent(
                                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());

                                try {
                                    startActivityForResult(intent, RESULT_SPEECH);

                                } catch (ActivityNotFoundException a) {
                                    Toast.makeText(getApplicationContext(),
                                            "No support for STT",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            break;

                        case "today":
                            ArrayList<String> nameOfEvent = CalendarC.readCalendarEvent(this);
                            Toast.makeText(getApplicationContext(), nameOfEvent.toString(), Toast.LENGTH_SHORT).show();
                            t1.speak(nameOfEvent.toString(), TextToSpeech.QUEUE_FLUSH, null);


                    }

                    Text.setText(text.get(0));

                }
                break;
            }

        }
    }

    public String currentTime() {


        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");


        return sdf.format(cal.getTime());
}


    public void statusMethod(String com){
        String[] lines = com.split("\\n");

        for(String s: lines){
            if(s.contains("4") && s.contains("Lightingnumber2")){
                if(s.contains("OFF")){
                    t1.speak("Light is off", TextToSpeech.QUEUE_ADD, null);
                    Toast.makeText(getApplicationContext(), "Light is off", Toast.LENGTH_SHORT).show();
                }else{
                    t1.speak("Light is on", TextToSpeech.QUEUE_ADD, null);
                    Toast.makeText(getApplicationContext(), "Light is on", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static boolean isTimeBetweenTwoTime(String initialTime, String finalTime, String currentTime) throws ParseException {
        String reg = "^([0-1][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
        if (initialTime.matches(reg) && finalTime.matches(reg) && currentTime.matches(reg)) {
            boolean valid = false;

            java.util.Date inTime = new SimpleDateFormat("HH:mm:ss").parse(initialTime);
            Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(inTime);


            java.util.Date checkTime = new SimpleDateFormat("HH:mm:ss").parse(currentTime);
            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime(checkTime);


            java.util.Date finTime = new SimpleDateFormat("HH:mm:ss").parse(finalTime);
            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime(finTime);

            if (finalTime.compareTo(initialTime) < 0) {
                calendar2.add(Calendar.DATE, 1);
                calendar3.add(Calendar.DATE, 1);
            }

            java.util.Date actualTime = calendar3.getTime();
            if ((actualTime.after(calendar1.getTime()) || actualTime.compareTo(calendar1.getTime()) == 0)
                    && actualTime.before(calendar2.getTime())) {
                valid = true;
            }
            return valid;
        } else {
            throw new IllegalArgumentException("Not a valid time, expecting HH:MM:SS format");
        }

    }

    public String run(String command) {
        String hostname = "192.168.0.8";
        String username = "pi";
        String password = "raspberry";
        StringBuilder total = new StringBuilder();

        try {

            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Connection conn = new Connection(hostname);
            conn.connect();
            Boolean isAuthenticated =
                    conn.authenticateWithPassword(username, password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new
                    InputStreamReader(stdout));
            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                total.append(line).append("\n");
            }

            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close();
            conn.close();


        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }

        return total.toString();

    }


}

