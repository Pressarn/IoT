package com.simonschwieler.speech_to_text;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;



public class CalendarC {
    public static ArrayList<String> nameOfEvent = new ArrayList<String>();
    public static ArrayList<String> startDates = new ArrayList<String>();
    public static ArrayList<String> endDates = new ArrayList<String>();
    public static ArrayList<String> descriptions = new ArrayList<String>();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    static Date date = new Date();
    private static String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());


    public static ArrayList<String> readCalendarEvent(Context context) {

        Cursor cursor = context.getContentResolver()
                .query(
                        Uri.parse("content://com.android.calendar/events"),
                        new String[]{"calendar_id", "title", "description",
                                "dtstart", "dtend", "eventLocation"}, null,
                        null, null);
        cursor.moveToFirst();

        String CNames[] = new String[cursor.getCount()];


        nameOfEvent.clear();
        startDates.clear();
        endDates.clear();
        descriptions.clear();


        for (int i = 0; i < CNames.length; i++) {

            if(getDate(Long.parseLong(cursor.getString(3))).contains(timeStamp)) {

                nameOfEvent.add(cursor.getString(1));
                startDates.add(getDate(Long.parseLong(cursor.getString(3))));
                endDates.add(getDate(Long.parseLong(cursor.getString(4))));
                descriptions.add(cursor.getString(2));
                CNames[i] = cursor.getString(1);
                cursor.moveToNext();
            }
        }

        return nameOfEvent;
    }

    public static String getDate(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy hh:mm:ss a");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

}