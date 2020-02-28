package com.liner.facerengineview.Engine.Util.Parser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.text.format.DateFormat;

import com.liner.facerengineview.R;

import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagParser {
    private static final String SPECIAL_REGEX = "(\\$(\\-?\\w+\\.?\\d*)(=|!=|>|<|<=|>=)(\\-?\\w+\\.?\\d*)(\\|\\||&&)?(\\-?\\w+\\.?\\d*)?(=|!=|>|<|<=|>=)?(\\-?\\w+\\.?\\d*)?(\\|\\||&&)?(\\-?\\w+\\.?\\d*)?(=|!=|>|<|<=|>=)?(\\-?\\w+\\.?\\d*)?\\?([^:\\r\\n]*):([^$\\r\\n]*)\\$)";
    private static final Pattern mPattern = Pattern.compile(SPECIAL_REGEX);

    //---------- BATTERY ----------
    public static String processBattery(Context context, String tag) {
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
    public static String[] getTagOnly(Context context,String tag){
        Pattern pattern = Pattern.compile("#\\w*#");
        Matcher matcher = pattern.matcher(tag);
        matcher.reset(tag);
        ArrayList<String> out = new ArrayList<>();
        while (matcher.find()) {
            out.add(matcher.group());
        }
        return out.toArray(new String[0]);
    }

    public static String processMath(String tag){
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
    public static String processPhone(Context context, String tag){
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
    public static String processTime(Context context, String tag){
        String[] hours = context.getResources().getStringArray(R.array.watchface_text_hours);
        String[] major = context.getResources().getStringArray(R.array.watchface_text_minutes_major);
        String clearedTAG = tag.replaceAll("#", "");
        switch (clearedTAG){
            case "DWE": //Time elapsed since last watch face view (in seconds with 0.01 parts)
                return "0";
            case "DNOW": //Current timestamp
            case "DSYNC": //Timestamp at which watch face was synced
                long timestamp = java.lang.System.currentTimeMillis()/1000;
                return Long.toString(timestamp);
            case "Dy": //Year
                return tag.replace(tag, getDateForFormat("y"));
            case "Dyy": //Short Year
                return tag.replace(tag, getDateForFormat("yy"));
            case "Dyyyy": //Long Year
                return tag.replace(tag, getDateForFormat("yyyy"));
            case "DM": //Month in Year (Numeric)
                return tag.replace(tag, getDateForFormat("M"));
            case "DMM": //Month in Year (Numeric) with leading 0
                return tag.replace(tag, getDateForFormat("MM"));
            case "DMMM": //Month in Year (Short String)
                return tag.replace(tag, getDateForFormat("MMM"));
            case "DMMMM": //Month in Year (String)
                return tag.replace(tag, getDateForFormat("MMMM"));
            case "DW": //Week in Month
                return tag.replace(tag, getDateForFormat("W"));
            case "Dw": //Week in Year
                return tag.replace(tag, getDateForFormat("w"));
            case "DD":
                return tag.replace(tag, getDateForFormat("DD"));
            case "Dd":
                return tag.replace(tag, getDateForFormat("d"));
            case "DdL":
                return tag.replace(tag, Integer.parseInt(getDateForFormat("d")) < 10 ? "0"+getDateForFormat("d") : getDateForFormat("d"));
            case "DIM":
                return tag.replace(tag, String.valueOf(Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)));
            case "DE":
                return tag.replace(tag, getDateForFormat("E"));
            case "DES":
                return tag.replace(tag, getDateForFormat("E").substring(0, 1));
            case "DOW":
                return tag.replace(tag, String.valueOf(Integer.parseInt(getDateForFormat("u"))-1));
            case "DOWB":
                return tag.replace(tag, getDateForFormat("u"));
            case "DEEEE":
                return tag.replace(tag, getDateForFormat("EEEE"));
            case "DF":
                return tag.replace(tag, getDateForFormat("F"));
            case "Da":
                return tag.replace(tag, getDateForFormat("a"));
            case "Db":
                if(DateFormat.is24HourFormat(context)){
                    if(Integer.valueOf(getDateForFormat("k")) < 10) {
                        return tag.replace(tag, "0" + getDateForFormat("k"));
                    } else {
                        return tag.replace(tag, getDateForFormat("k"));
                    }
                } else {
                    if(Integer.valueOf(getDateForFormat("h")) < 10) {
                        return tag.replace(tag, "0" + getDateForFormat("h"));
                    } else {
                        return tag.replace(tag, getDateForFormat("h"));
                    }
                }
            case "Dh":
                return tag.replace(tag, getDateForFormat("h"));
            case "Dk":
                return tag.replace(tag, getDateForFormat("k"));
            case "DH":
                return tag.replace(tag, getDateForFormat("H"));
            case "DK":
                return tag.replace(tag, getDateForFormat("K"));
            case "DHZ":
                if(Integer.valueOf(getDateForFormat("H")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("H"));
                } else {
                    return tag.replace(tag, getDateForFormat("H"));
                }
            case "DkZ":
                if(Integer.valueOf(getDateForFormat("k")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("k"));
                } else {
                    return tag.replace(tag, getDateForFormat("k"));
                }
            case "DkZA":
                if(Integer.valueOf(getDateForFormat("k")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("k").substring(0,1));
                } else {
                    return tag.replace(tag, getDateForFormat("k").substring(0,1));
                }
            case "DkZB":
                if(Integer.valueOf(getDateForFormat("k")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("k").substring(1,2));
                } else {
                    return tag.replace(tag, getDateForFormat("k").substring(1,2));
                }
            case "DKZ":
                if(Integer.valueOf(getDateForFormat("K")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("K"));
                } else {
                    return tag.replace(tag, getDateForFormat("K"));
                }
            case "DhZ":
                if(Integer.valueOf(getDateForFormat("h")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("h"));
                } else {
                    return tag.replace(tag, getDateForFormat("h"));
                }
            case "DhZA":
                if(Integer.valueOf(getDateForFormat("h")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("h").substring(0,1));
                } else {
                    return tag.replace(tag, getDateForFormat("h").substring(0,1));
                }
            case "DhZB":
                if(Integer.valueOf(getDateForFormat("h")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("h").substring(1,2));
                } else {
                    return tag.replace(tag, getDateForFormat("h").substring(1,2));
                }
            case "DhoT":
                return tag.replace(tag, String.valueOf((Integer.parseInt(getDateForFormat("H"))%12)*30));
            case "DhoTb":
                return tag.replace(tag, String.valueOf((Integer.parseInt(getDateForFormat("H"))*15)));
            case "DWFK":
                return tag.replace(tag, String.valueOf((Integer.parseInt(getDateForFormat("K"))%12)*30));
            case "DWFH":
                return tag.replace(tag, String.valueOf((Integer.parseInt(getDateForFormat("K"))*15)));
            case "DWFKS":
                return tag.replace(tag, String.valueOf(((double) ((Integer.parseInt(getDateForFormat("H")) % 12) * 30)) + (((double) Integer.parseInt(getDateForFormat("m"))) * 0.5d)));
            case "DWFHS":
                return tag.replace(tag, String.valueOf(((double) ((Integer.parseInt(getDateForFormat("H")) % 12) * 30)) + (((double) Integer.parseInt(getDateForFormat("m"))) * 0.25d)));
            case "DhT":
                return tag.replace(tag, (Integer.parseInt(getDateForFormat("K")) % 12 == 0 ? hours[11] : hours[((Integer.parseInt(getDateForFormat("K")) % 12)-1)]));
            case "DkT":
                return tag.replace(tag, (Integer.parseInt(getDateForFormat("H")) % 12 == 0 ? hours[11] : hours[((Integer.parseInt(getDateForFormat("H")) % 12)-1)]));
            case "Dm":
                return tag.replace(tag, getDateForFormat("m"));
            case "DmZ":
                if(Integer.valueOf(getDateForFormat("m")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("m"));
                } else {
                    return tag.replace(tag, getDateForFormat("m"));
                }
            case "DWFM":
                return tag.replace(tag, String.valueOf(Integer.parseInt(getDateForFormat("m")) * 6));
            case "DWFMS":
                return tag.replace(tag, String.valueOf(((double) (Integer.parseInt(getDateForFormat("m")) * 6)) + (((double) Integer.parseInt(getDateForFormat("s"))) * 0.1d)));
            case "DmT":
                String tempString = "";
                if(Integer.parseInt(getDateForFormat("m")) > 20 || Integer.parseInt(getDateForFormat("m")) == 0){
                    if(Integer.parseInt(getDateForFormat("m")) != 0){
                        switch (Integer.parseInt(getDateForFormat("m")) / 10){
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
                        return tag.replace(tag, tempString + " " + hours[(Integer.parseInt(getDateForFormat("m")) % 10)-1]);
                    }
                }
                return tag.replace(tag, tempString);
            case "DmMT":
                return tag.replace(tag, (Integer.parseInt(getDateForFormat("m")) / 10) >= 2 ? major[(Integer.parseInt(getDateForFormat("m")) / 10)-1] : "");
            case "DmST":
                if (Integer.parseInt(getDateForFormat("m")) % 10 != 0 && Integer.parseInt(getDateForFormat("m")) / 10 != 1) {
                    return tag.replace(tag, hours[Integer.parseInt(getDateForFormat("m")) % 10 - 1]);
                } else if (Integer.parseInt(getDateForFormat("m")) / 10 == 1) {
                    return tag.replace(tag, hours[((Integer.parseInt(getDateForFormat("m")) / 10 * 10) + Integer.parseInt(getDateForFormat("m")) % 10) - 1]);
                }
            case "Ds":
                return tag.replace(tag, getDateForFormat("s"));
            case "DsZ":
                if(Integer.valueOf(getDateForFormat("s")) < 10) {
                    return tag.replace(tag, "0" + getDateForFormat("s"));
                } else {
                    return tag.replace(tag, getDateForFormat("s"));
                }
            case "Dsm":
                return tag.replace(tag, String.format(Locale.getDefault(), "%.3f", ((double) ((float) Integer.parseInt(getDateForFormat("s")))) + (((double) java.lang.System.currentTimeMillis() % 1000) * 0.001d)));
            case "DWFS":
            case "DseT":
                return tag.replace(tag, String.valueOf(Integer.parseInt(getDateForFormat("s")) * 6));
            case "DWFSS":
                return tag.replace(tag, String.format(Locale.US, "%.3f", ((double) (Integer.parseInt(getDateForFormat("s")) * 6)) + (((double) java.lang.System.currentTimeMillis() % 1000) * 0.006d)));
            case "DSMOOTH":
                return Boolean.toString(true);
            case "Dz":
                return tag.replace(tag, getDateForFormat("z"));
            case "Dzzzz":
                return tag.replace(tag, getDateForFormat("zzzz"));
            case "DWR":
                return tag.replace(tag, String.valueOf((360.0f / ((float) Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_WEEK))) * ((float) (Integer.parseInt(getDateForFormat("s"))))));
            case "DMR":
                return tag.replace(tag, String.valueOf((360.0f / ((float) Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH))) * ((float) Integer.parseInt(getDateForFormat("d")))));
            case "DYR":
                return tag.replace(tag, String.valueOf((360.0f / ((float) Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_YEAR))) * ((float) (Integer.parseInt(getDateForFormat("D"))))));
            case "DMYR":
                return tag.replace(tag, String.valueOf((Integer.parseInt(getDateForFormat("M"))) * 30));
            case "DUh":
                return tag.replace(tag, getUTCDateForFormat("h"));
            case "DUk":
                return tag.replace(tag, getUTCDateForFormat("k"));
            case "DUH":
                return tag.replace(tag, getUTCDateForFormat("H"));
            case "DUK":
                return tag.replace(tag, getUTCDateForFormat("K"));
            case "DUb":
                if(DateFormat.is24HourFormat(context)){
                    if(Integer.valueOf(getUTCDateForFormat("k")) < 10) {
                        return tag.replace(tag, "0" + getUTCDateForFormat("k"));
                    } else {
                        return tag.replace(tag, getUTCDateForFormat("k"));
                    }
                } else {
                    if(Integer.valueOf(getUTCDateForFormat("h")) < 10) {
                        return tag.replace(tag, "0" + getUTCDateForFormat("h"));
                    } else {
                        return tag.replace(tag, getUTCDateForFormat("h"));
                    }
                }
                default:
                    return tag;
        }
    }
    @SuppressLint("WrongConstant")
    private static String getDateForFormat(String formatString) {
        if (formatString == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(java.lang.System.currentTimeMillis());
        format.setCalendar(calendar);
        return format.format(calendar.getTime());
    }
    private static String getUTCDateForFormat(String formatString) {
        if (formatString == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(java.lang.System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date(java.lang.System.currentTimeMillis());
        return format.format(date);
    }

    //-------------------------

    //---------- TEXT ----------
    public static String processText(Context context, String text){
        Pattern pattern = Pattern.compile("#\\w*#");
        Matcher matcher = pattern.matcher(text);
        matcher.reset(text);
        String tempString;
        char tempChar;
        while (matcher.find()){
            tempString = matcher.group();
            tempChar = tempString.charAt(1);
            switch (tempChar){
                case 'D':
                case 'd':
                    text = text.replace(tempString, processTime(context, tempString));
                case 'B':
                case 'b':
                    text =  text.replace(tempString, processBattery(context,tempString));
                case 'Z':
                case 'z':
                    text =  text.replace(tempString, processHealth(tempString));
                case 'P':
                case 'p':
                    text =  text.replace(tempString, processPhone(context,tempString));
                //case 'W':
                //case 'w':
                //    text =  "weather"; //WeatherParser
            }
        }
        if (text.contains("(") || text.contains(")") || text.contains("$")) {
            return processFinal(TagParser.processMath(text));
        }
        return text;
    }
    //-------------------------

    //---------- HEALTH ----------
    public static String processHealth(String tag){
        int stepCount = 1002;
        int avgHeartRate = 89;
        String clearTAG = tag.replaceAll("#", "");
        if(clearTAG.equals("ZSC")){
            return String.valueOf(stepCount);
        } else if (clearTAG.equals("ZHR")){
            return String.valueOf(avgHeartRate);
        } else {
            return tag;
        }
    }
    //-------------------------

    //---------- FINAL ------------
    public static String processFinal(String string) {
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
