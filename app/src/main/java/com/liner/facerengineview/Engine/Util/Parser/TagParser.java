package com.liner.facerengineview.Engine.Util.Parser;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.text.format.Time;
import android.util.Log;

import com.liner.facerengineview.R;

import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EmptyStackException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagParser {
    private static final String SPECIAL_REGEX = "(\\$(\\-?\\w+\\.?\\d*)(=|!=|>|<|<=|>=)(\\-?\\w+\\.?\\d*)(\\|\\||&&)?(\\-?\\w+\\.?\\d*)?(=|!=|>|<|<=|>=)?(\\-?\\w+\\.?\\d*)?(\\|\\||&&)?(\\-?\\w+\\.?\\d*)?(=|!=|>|<|<=|>=)?(\\-?\\w+\\.?\\d*)?\\?([^:\\r\\n]*):([^$\\r\\n]*)\\$)";
    private static final Pattern mPattern = Pattern.compile(SPECIAL_REGEX);

    //---------- BATTERY ----------
    public static String parseBattery(Context context, String tag) {
        try {
            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            assert batteryStatus != null;
            int status = batteryStatus.getIntExtra("status", -1);
            boolean isCharging = status == 2 || status == 5;
            int batteryLevel = batteryStatus.getIntExtra("level", -1);
            int batteryScale = batteryStatus.getIntExtra("scale", -1);
            int batteryTemp = batteryStatus.getIntExtra("temperature", -1);
            int batteryVoltage = batteryStatus.getIntExtra("voltage", -1);
            float batteryLevelPercent = (float)batteryLevel/(float)batteryScale;
            String clearTAG = tag.replaceAll("#", "");
            switch (clearTAG){
                case "BLP":
                    return Math.round(100.0f * batteryLevelPercent) + "%";
                case "BLN":
                    return String.valueOf(Math.round(100.0f * batteryLevelPercent));
                case "BTC":
                    return (batteryTemp / 10) + "ºC";
                case "BTI":
                    return Math.round((((double) (batteryTemp / 10)) * 1.8d) + 32.0d) + "ºF";
                case "BS":
                    return isCharging ? context.getString(R.string.charging) : context.getString(R.string.not_charging);
                case "BTCN":
                    return String.valueOf((batteryTemp / 10));
                case "BTIN":
                    return String.valueOf(Math.round((((double) (batteryTemp / 10)) * 1.8d) + 32.0d));
                    default:
                        return tag;

            }
        } catch (Exception e){
            e.printStackTrace();
            return tag;
        }
    }
    //-------------------------

    //---------- MATH ----------
    public static String parseMath(String tag){
        int openBr = 0;
        int start = 0;
        int i = 0;
        while (i < tag.length()) {
            if (tag.charAt(i) == '(' || tag.charAt(i) == '[') {
                if (openBr == 0) {
                    start = i;
                }
                openBr++;
            } else if (tag.charAt(i) == ')' || tag.charAt(i) == ']') {
                openBr--;
                if (openBr == 0) {
                    tag = tag.replace(tag.substring(start, i + 1), eval(tag.substring(start, i + 1)));
                }
            }
            i++;
        }
        return tag;
    }
    private static String eval(String text) {
        try {
            String result = new ExpressionBuilder(text).variables("pi", "e").functions(round, rand, rad, deg).build().setVariable("pi", 3.141592653589793d).setVariable("e", 2.718281828459045d).evaluate() + "";
            return result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
        } catch (IllegalArgumentException e) {
            return "0";
        } catch (EmptyStackException e2) {
            e2.printStackTrace();
            return "0";
        }
    }
    private static Function deg = new Function("deg") {
        public double apply(double... doubles) {
            return Math.toDegrees(doubles[0]);
        }
    };
    private static Function rad = new Function("rad") {
        public double apply(double... doubles) {
            return Math.toRadians(doubles[0]);
        }
    };
    private static Function rand = new Function("rand", 2) {
        public double apply(double... args) {
            return (double) ((int) ((Math.random() * args[1]) + args[0]));
        }
    };
    private static Function round = new Function("round") {
        public double apply(double... args) {
            return (double) Math.round(args[0]);
        }
    };
    //-------------------------

    //---------- PHONE ----------
    //"PBP" return phone battery level percentage (redundant, cause cannot get phone status)
    //"PBN" return phone battery level (redundant, cause cannot get phone status)
    public static String parsePhoneData(Context context, String tag){
        try {
            if(tag.replaceAll("#", "").equals("PWL")){
                return String.valueOf(WifiManager.calculateSignalLevel(((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getRssi(), 4));
            } else {
                return tag;
            }
        }catch (Exception e){
            return  tag;
        }
    }
    //-------------------------

    //---------- TIME ----------
    public static String parseTimeTAG(Context context, String tag, boolean isSmooth){
        Log.d("TEXTPARSER", "Input "+tag);
        String[] hours = context.getResources().getStringArray(R.array.watchface_text_hours);
        String[] major = context.getResources().getStringArray(R.array.watchface_text_minutes_major);
        Time time = new Time();
        Calendar calendar = Calendar.getInstance();
        time.set(java.lang.System.currentTimeMillis());
        calendar.setTimeInMillis(java.lang.System.currentTimeMillis());
        switch (tag){
            case "#DhT#":
                return tag.replace(tag, time.hour % 12 == 0 ? hours[11] : hours[(time.hour % 12)-1]);
            case "#DkT#":
                return tag.replace(tag, time.hour == 0 ? hours[23] : hours[time.hour - 1]);
            case "#DmT#":
                String tempString = "";
                if(time.minute > 20 || time.minute == 0){
                    if(time.minute != 0){
                        switch (time.minute / 10){
                            case 2:
                                tempString = hours[19];
                                break;
                            case 3:
                                tempString = major[2];
                                break;
                            case 4:
                                tempString = major[3];
                                break;
                            case 5:
                                tempString = major[4];
                                break;
                        }
                        return tag.replace(tag, tempString + " " + hours[(time.minute % 10)-1]);
                    }
                }
                return tag.replace(tag, tempString);
            case "#DmMT#":
                return tag.replace(tag, (time.minute / 10) >= 2 ? major[(time.minute / 10)-1] : "");
            case "#DmST#":
                if (time.minute % 10 != 0 && time.minute / 10 != 1) {
                    return tag.replace(tag, hours[time.minute % 10 - 1]);
                } else if (time.minute / 10 == 1) {
                    return tag.replace(tag, hours[((time.minute / 10 * 10) + time.minute % 10) - 1]);
                }
            case "#DWFS#":
                return tag.replace(tag, String.valueOf(time.second * 6));
            case "#DWFK#":
            case "#DhoT#":
                return tag.replace(tag, String.valueOf((time.hour % 12) * 30));
            case "#DWFH#":
            case "#DhoTb#":
                return tag.replace(tag, String.valueOf(time.hour * 15));
            case "#DWFKS#":
                return tag.replace(tag, String.valueOf(((double) ((time.hour % 12) * 30)) + (((double) time.minute) * 0.5d)));
            case "#DWFHS#":
                return tag.replace(tag, String.valueOf(((double) (time.hour * 15)) + (((double) time.minute) * 0.25d)));
            case "#DWFM#":
            case "#DmoT#":
                return tag.replace(tag, String.valueOf(time.minute * 6));
            case "#DWFMS#":
                return tag.replace(tag, String.valueOf(((double) (time.minute * 6)) + (((double) time.second) * 0.1d)));
            case "#DseT#":
                return tag.replace(tag, String.valueOf(time.second * 6));
            case "#DHZ#":
            case "#DkZ#":
                return tag.replace(tag, time.hour < 10 ? "0"+time.hour : String.valueOf(time.hour));
            case "#DKZ#":
                return tag.replace(tag, time.hour < 10 ? "0"+time.hour % 12 : String.valueOf(time.hour % 12));
            case "#DhZ#":
                String tmpString;
                if (time.hour == 0 || time.hour == 12) {
                    tmpString = "12";
                } else {
                    tmpString = String.valueOf(time.hour % 12);
                }
                if (!(time.hour % 12 >= 10 || time.hour == 0 || time.hour == 12)) {
                    return tag.replace(tag, "0" + tmpString);
                } else {
                    return tag.replace(tag,  tmpString);
                }
            case "#DhZA#":
                return tag.replace(tag, (time.hour == 0 || time.hour == 12) ? "1" : String.valueOf((time.hour % 12) / 10));
            case "#DhZB#":
                return tag.replace(tag, (time.hour == 0 || time.hour == 12) ? "2" : String.valueOf((time.hour % 12) % 10));
            case "#DkZA#":
                return tag.replace(tag, String.valueOf(time.hour / 10));
            case "#DkZB#":
                return tag.replace(tag, String.valueOf(time.hour % 10));
            case "#DmZ#":
                return tag.replace(tag, time.minute < 10 ? "0"+time.minute : String.valueOf(time.minute));
            case "#DsZ#":
                return tag.replace(tag, time.second < 10 ? "0"+time.second : String.valueOf(time.second));
            case "#DWR#":
                return tag.replace(tag, String.valueOf((360.0f / ((float) calendar.getActualMaximum(Calendar.DAY_OF_WEEK))) * ((float) (time.weekDay + 1))));
            case "#DMR#":
                return tag.replace(tag, String.valueOf((360.0f / ((float) calendar.getActualMaximum(Calendar.DAY_OF_MONTH))) * ((float) time.monthDay)));
            case "#DYR#":
                return tag.replace(tag, String.valueOf((360.0f / ((float) calendar.getActualMaximum(Calendar.DAY_OF_YEAR))) * ((float) (time.yearDay + 1))));
            case "#DMYR#":
                return tag.replace(tag, String.valueOf((time.month + 1) * 30));
            case "#Dd#":
                return tag.replace(tag, String.valueOf(time.monthDay));
            case "#DdL#":
                return tag.replace(tag, time.monthDay < 10 ? "0"+time.monthDay : String.valueOf(time.monthDay));
            case "#DWFSS#":
                return tag.replace(tag, String.format(Locale.US, "%.3f", ((double) (time.second * 6)) + (((double) java.lang.System.currentTimeMillis() % 1000) * 0.006d)));
            case "#Dsm#":
                return tag.replace(tag, String.format(Locale.US, "%.3f", ((double) ((float) time.second)) + (((double) java.lang.System.currentTimeMillis() % 1000) * 0.001d)));
            case "#Dms#":
                return tag.replace(tag, String.format(Locale.US, "%.3f", ((double) java.lang.System.currentTimeMillis() % 1000) * 0.001d));
            case "#DOW#":
                return tag.replace(tag, String.valueOf(time.weekDay));
            case "#DOWB#":
                return tag.replace(tag, String.valueOf(time.weekDay + 1));
            case "#DIM#":
                return tag.replace(tag, String.valueOf(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)));
            case "#DISFACER#":
                return tag.replace(tag, "100");
            case "#DSMOOTH#":
                return tag.replace(tag, Boolean.toString(isSmooth));
            case "#DES#":
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E", Locale.getDefault());
                simpleDateFormat.setCalendar(calendar);
                return tag.replace(tag, simpleDateFormat.format(calendar.getTime()).substring(0, 1));
                default:
                    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(tag.substring(2), Locale.getDefault());
                    simpleDateFormat2.setCalendar(calendar);
                    return tag.replace(tag, simpleDateFormat2.format(calendar.getTime()));
        }
    }
    //-------------------------

    //---------- TEXT ----------
    public static String parseText(Context context, String text){
        Pattern pattern = Pattern.compile("#\\w+#");
        Matcher matcher = pattern.matcher("");
        matcher.reset(text);
        String tempString;
        char tempChar;
        while (matcher.find()){
            tempString = matcher.group();
            tempChar = tempString.charAt(1);
            switch (tempChar){
                case 'D':
                case 'd':
                    return tempString.replace(tempString, parseTimeTAG(context, tempString, true));
                case 'B':
                case 'b':
                    return tempString.replace(tempString, parseBattery(context,tempString));
                case 'Z':
                case 'z':
                    return tempString.replace(tempString, parseHealthTAG(tempString));
                case 'P':
                case 'p':
                    return tempString.replace(tempString, parsePhoneData(context,tempString));
                case 'W':
                case 'w':
                    return "weather"; //WeatherParser
            }
        }
        if (text.contains("(") || text.contains(")") || text.contains("$")) {
            return parseFinal(TagParser.parseMath(text));
        }
        return text;
    }
    public static float parseTextFloat(Context context, String text){
        Pattern pattern = Pattern.compile("#\\w+#");
        Matcher matcher = pattern.matcher("");
        matcher.reset(text);
        String tempString;
        char tempChar;
        while (matcher.find()){
            tempString = matcher.group();
            tempChar = tempString.charAt(1);
            switch (tempChar){
                case 'D':
                case 'd':
                    return Float.parseFloat(tempString.replace(tempString, parseTimeTAG(context, tempString, true)));
                case 'B':
                case 'b':
                    return Float.parseFloat(tempString.replace(tempString, parseBattery(context,tempString)));
                case 'Z':
                case 'z':
                    return 0f; //WearParser
                case 'W':
                case 'w':
                    return 0f; //WeatherParser
            }
        }
        if (text.contains("(") || text.contains(")") || text.contains("$")) {
            return Float.parseFloat(parseFinal(TagParser.parseMath(text)));
        }
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e2) {
            return 0.0f;
        }
    }
    //-------------------------


    //---------- HEALTH ----------
    public static String parseHealthTAG(String tag){
        int stepCount = 1002;
        int avgHeartRate = 89;
        String clearTAG = tag.replaceAll("#", "");
        switch (clearTAG){
            case "ZSC":
                return String.valueOf(stepCount);
            case "ZHR":
                return String.valueOf(avgHeartRate);
            default:
                return tag;
        }
    }
    //-------------------------


    //---------- FINAL ------------
    public static String parseFinal(String string) {
        Matcher mMatcher = mPattern.matcher(string);
        while (mMatcher.find()) {
            String group1;
            String group2;
            boolean mMatch1 = false;
            boolean mMatch2 = false;
            boolean mMatch3 = false;
            if (mMatcher.group(3).matches("=")) {
                group1 = mMatcher.group(2);
                group2 = mMatcher.group(4);
                if (group1.endsWith(".0")) {
                    group1 = group1.substring(0, group1.length() - 2);
                }
                if (group2.endsWith(".0")) {
                    group2 = group2.substring(0, group2.length() - 2);
                }
                mMatch1 = group1.matches(group2);
            } else if (mMatcher.group(3).matches("!=")) {
                mMatch1 = !mMatcher.group(2).matches(mMatcher.group(4));
            } else if (mMatcher.group(3).matches("<")) {
                mMatch1 = Float.parseFloat(mMatcher.group(2)) < Float.parseFloat(mMatcher.group(4));
            } else if (mMatcher.group(3).matches(">")) {
                mMatch1 = Float.parseFloat(mMatcher.group(2)) > Float.parseFloat(mMatcher.group(4));
            } else if (mMatcher.group(3).matches(">=")) {
                mMatch1 = Float.parseFloat(mMatcher.group(2)) >= Float.parseFloat(mMatcher.group(4));
            } else if (mMatcher.group(3).matches("<=")) {
                mMatch1 = Float.parseFloat(mMatcher.group(2)) <= Float.parseFloat(mMatcher.group(4));
            }
            if (!(mMatcher.group(5) == null || mMatcher.group(6) == null || mMatcher.group(7) == null || mMatcher.group(8) == null)) {
                if (mMatcher.group(7).matches("=")) {
                    group1 = mMatcher.group(6);
                    group2 = mMatcher.group(8);
                    if (group1.endsWith(".0")) {
                        group1 = group1.substring(0, group1.length() - 2);
                    }
                    if (group2.endsWith(".0")) {
                        group2 = group2.substring(0, group2.length() - 2);
                    }
                    mMatch2 = group1.matches(group2);
                } else if (mMatcher.group(7).matches("!=")) {
                    mMatch2 = !mMatcher.group(6).matches(mMatcher.group(8));
                } else if (mMatcher.group(7).matches("<")) {
                    mMatch2 = Float.parseFloat(mMatcher.group(6)) < Float.parseFloat(mMatcher.group(8));
                } else if (mMatcher.group(7).matches(">")) {
                    mMatch2 = Float.parseFloat(mMatcher.group(6)) > Float.parseFloat(mMatcher.group(8));
                } else if (mMatcher.group(7).matches(">=")) {
                    mMatch2 = Float.parseFloat(mMatcher.group(6)) >= Float.parseFloat(mMatcher.group(8));
                } else if (mMatcher.group(7).matches("<=")) {
                    mMatch2 = Float.parseFloat(mMatcher.group(6)) <= Float.parseFloat(mMatcher.group(8));
                }
            }
            if (!(mMatcher.group(9) == null || mMatcher.group(10) == null || mMatcher.group(11) == null || mMatcher.group(12) == null)) {
                if (mMatcher.group(11).matches("=")) {
                    group1 = mMatcher.group(10);
                    group2 = mMatcher.group(12);
                    if (group1.endsWith(".0")) {
                        group1 = group1.substring(0, group1.length() - 2);
                    }
                    if (group2.endsWith(".0")) {
                        group2 = group2.substring(0, group2.length() - 2);
                    }
                    mMatch3 = group1.matches(group2);
                } else if (mMatcher.group(11).matches("!=")) {
                    mMatch3 = !mMatcher.group(10).matches(mMatcher.group(12));
                } else if (mMatcher.group(11).matches("<")) {
                    mMatch3 = Float.parseFloat(mMatcher.group(10)) < Float.parseFloat(mMatcher.group(12));
                } else if (mMatcher.group(11).matches(">")) {
                    mMatch3 = Float.parseFloat(mMatcher.group(10)) > Float.parseFloat(mMatcher.group(12));
                } else if (mMatcher.group(11).matches(">=")) {
                    mMatch3 = Float.parseFloat(mMatcher.group(10)) >= Float.parseFloat(mMatcher.group(12));
                } else if (mMatcher.group(11).matches("<=")) {
                    mMatch3 = Float.parseFloat(mMatcher.group(10)) <= Float.parseFloat(mMatcher.group(12));
                }
            }
            boolean mMatchA = false;
            if (mMatcher.group(5) != null) {
                if (mMatcher.group(5).matches("\\|\\|")) {
                    mMatchA = mMatch1 || mMatch2;
                } else if (mMatcher.group(5).matches("&&")) {
                    mMatchA = mMatch1 && mMatch2;
                }
            }
            if (mMatcher.group(9) != null) {
                if (mMatcher.group(9).matches("\\|\\|")) {
                    if (mMatchA || mMatch3) {
                        string = string.replace(mMatcher.group(), mMatcher.group(13));
                    } else {
                        string = string.replace(mMatcher.group(), mMatcher.group(14));
                    }
                } else if (mMatcher.group(9).matches("&&")) {
                    if (mMatchA && mMatch3) {
                        string = string.replace(mMatcher.group(), mMatcher.group(13));
                    } else {
                        string = string.replace(mMatcher.group(), mMatcher.group(14));
                    }
                }
            } else if (mMatcher.group(5) != null) {
                if (mMatchA) {
                    string = string.replace(mMatcher.group(), mMatcher.group(13));
                } else {
                    string = string.replace(mMatcher.group(), mMatcher.group(14));
                }
            } else if (mMatch1) {
                string = string.replace(mMatcher.group(), mMatcher.group(13));
            } else {
                string = string.replace(mMatcher.group(), mMatcher.group(14));
            }
        }
        return string;
    }

}
