package com.liner.facerengineview.Engine.Util.Parser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.core.math.MathUtils;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.liner.facerengineview.R;

import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagParser {
    private static final String SPECIAL_REGEX = "(\\$(\\-?\\w+\\.?\\d*)(=|!=|>|<|<=|>=)(\\-?\\w+\\.?\\d*)(\\|\\||&&)?(\\-?\\w+\\.?\\d*)?(=|!=|>|<|<=|>=)?(\\-?\\w+\\.?\\d*)?(\\|\\||&&)?(\\-?\\w+\\.?\\d*)?(=|!=|>|<|<=|>=)?(\\-?\\w+\\.?\\d*)?\\?([^:\\r\\n]*):([^$\\r\\n]*)\\$)";
    private static final Pattern mPattern = Pattern.compile(SPECIAL_REGEX);
    private static final String METHOD_ACCEL_ACCEL_X = "accelerometerRawX";
    private static final String METHOD_ACCEL_ACCEL_Y = "accelerometerRawY";
    private static final String METHOD_ACCEL_X = "accelerometerX";
    private static final String METHOD_ACCEL_Y = "accelerometerY";
    private static final String METHOD_BOXCAR = "boxcar";
    private static final String METHOD_CLAMP = "clamp";
    private static final String METHOD_DEG = "deg";
    private static final String METHOD_HEAVISIDE = "heaviside";
    private static final String METHOD_INTERP_ACCEL = "interpAccel";
    private static final String METHOD_INTERP_ACCELDECEL = "interpAccelDecel";
    private static final String METHOD_INTERP_ANTICIPATE = "interpAnticipate";
    private static final String METHOD_INTERP_ANTICIPATEOVERSHOOT = "interpAnticipateOvershoot";
    private static final String METHOD_INTERP_BOUNCE = "interpBounce";
    private static final String METHOD_INTERP_CYCLE = "interpCycle";
    private static final String METHOD_INTERP_DECEL = "interpDecel";
    private static final String METHOD_INTERP_FASTOUTLINEARIN = "interpFastOutLinearIn";
    private static final String METHOD_INTERP_FASTOUTSLOWIN = "interpFastOutSlowIn";
    private static final String METHOD_INTERP_LINEAR = "interpLinear";
    private static final String METHOD_INTERP_LINEAROUTSLOWIN = "interpLinearOutSlowIn";
    private static final String METHOD_INTERP_OVERSHOOT = "interpOvershoot";
    private static final String METHOD_ORBITX = "orbitX";
    private static final String METHOD_ORBITY = "orbitY";
    private static final String METHOD_RAD = "rad";
    private static final String METHOD_RAND = "rand";
    private static final String METHOD_RAND_STATE = "stRand";
    private static final String METHOD_RAND_WAKE = "wakeRand";
    private static final String METHOD_ROUND = "round";
    private static final String METHOD_SQUAREWAVE = "squareWave";
    private static final String[] OPERATORS = new String[]{"+", "-", "/", "*"};
    private static final String PARAM_SEPARATOR = ",";
    private static final String[] METHOD_LIST = new String[]{METHOD_ROUND, METHOD_RAND, METHOD_RAND_STATE, METHOD_RAND_WAKE, METHOD_RAD, METHOD_DEG, METHOD_CLAMP, METHOD_ORBITX, METHOD_ORBITY, METHOD_INTERP_LINEAR, METHOD_INTERP_ACCEL, METHOD_INTERP_DECEL, METHOD_INTERP_ACCELDECEL, METHOD_INTERP_ANTICIPATE, METHOD_INTERP_OVERSHOOT, METHOD_INTERP_ANTICIPATEOVERSHOOT, METHOD_INTERP_BOUNCE, METHOD_INTERP_CYCLE, METHOD_INTERP_FASTOUTLINEARIN, METHOD_INTERP_FASTOUTSLOWIN, METHOD_INTERP_LINEAROUTSLOWIN, METHOD_HEAVISIDE, METHOD_BOXCAR, METHOD_SQUAREWAVE, METHOD_ACCEL_X, METHOD_ACCEL_Y, METHOD_ACCEL_ACCEL_X, METHOD_ACCEL_ACCEL_Y, "abs", "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "ceil", "floor", "log", "log2", "log10", "sqrt", "cbrt", "exp", "expm1"};

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


    public static String processWeather(String tag){
        String clearedTAG = tag.replaceAll("#", "");
        switch (clearedTAG){
            case "WM":
                return tag.replace(tag, "C");
            case "WLC":
                return tag.replace(tag, "Los Angeles");
            case "WTH":
                return tag.replace(tag, "25");
            case "WTL":
                return tag.replace(tag, "20");
            case "WCT":
                return tag.replace(tag, "22");
            case "WCCI":
                return tag.replace(tag, "03");
            case "WCCT":
                return tag.replace(tag, "Cloudy");
            case "WCHN":
                return tag.replace(tag, "40");
            case "WCHP":
                return tag.replace(tag, "40%");
            case "WRh":
                return tag.replace(tag, "5");
            case "WRhZ":
                return tag.replace(tag, "05");
            case "WRH":
                return tag.replace(tag, "5");
            case "WRHZ":
                return tag.replace(tag, "05");
            case "WRm":
                return tag.replace(tag, "25");
            case "WRmZ":
                return tag.replace(tag, "25");
            case "WSh":
                return tag.replace(tag, "12");
            case "WShZ":
                return tag.replace(tag, "12");
            case "WSH":
                return tag.replace(tag, "18");
            case "WSHZ":
                return tag.replace(tag, "18");
            case "WSm":
                return tag.replace(tag, "26");
            case "WSmZ":
                return tag.replace(tag, "26");
            case "WSUNRISE":
                return tag.replace(tag, "5:50 AM");
            case "WSUNSET":
                return tag.replace(tag, "8:06 PM");
            case "WSUNRISE24":
                return tag.replace(tag, "05:50");
            case "WSUNSET24":
                return tag.replace(tag, "20:06");
            case "WSUNRISEH":
                return tag.replace(tag, "5");
            case "WSUNRISEM":
                return tag.replace(tag, "06");
            case "WSUNSETH":
                return tag.replace(tag, "9");
            case "WSUNSETM":
                return tag.replace(tag, "14");
            case "WSUNRISEH24":
                return tag.replace(tag, "05");
            case "WSUNSETH24":
                return tag.replace(tag, "20");
            case "WFAH":
                return tag.replace(tag, "26");
            case "WFAL":
                return tag.replace(tag, "25");
            case "WFACT":
                return tag.replace(tag, "Sunny");
            case "WFACI":
                return tag.replace(tag, "01");
            case "WND":
                return tag.replace(tag, "5.5");
            case "WNDD":
                return tag.replace(tag, "45.98889");
            case "WNDDS":
                return tag.replace(tag, "NNE");
            case "WNDDSS":
                return tag.replace(tag, "North East");
                default:
                    return tag;
        }
    }

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

    private static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

    private static Function boxcar = new Function(METHOD_BOXCAR, 3) {
        public double apply(double... args) {
            if (args == null || args.length < 3) {
                return 0d;
            }
            double current = args[0];
            return heaviside(current - args[1]) - heaviside(current - args[2]);
        }
    };
    private static Function clamp = new Function(METHOD_CLAMP, 3) {
        public double apply(double... args) {
            if (args == null || args.length < 3) {
                return 0d;
            }
            return MathUtils.clamp(args[0], args[1], args[2]);
        }
    };
    private static Function deg = new Function(METHOD_DEG) {
        public double apply(double... doubles) {
            return Math.toDegrees(doubles[0]);
        }
    };
    private static Function heaviside = new Function(METHOD_HEAVISIDE) {
        public double apply(double... args) {
            if (args == null || args.length < 1) {
                return 0d;
            }
            return heaviside(args[0]);
        }
    };
    private static Function interpAccel = new Function(METHOD_INTERP_ACCEL, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            return calculateInterpolation(new AccelerateInterpolator((float) args[3]), args);
        }
    };
    private static Function interpAccelDecel = new Function(METHOD_INTERP_ACCELDECEL, 3) {
        private final Interpolator interpolator = new AccelerateDecelerateInterpolator();

        public double apply(double... args) {
            return calculateInterpolation(this.interpolator, args);
        }
    };
    private static Function interpAnticipate = new Function(METHOD_INTERP_ANTICIPATE, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            return calculateInterpolation(new AnticipateInterpolator((float) args[3]), args);
        }
    };
    private static Function interpAnticipateOvershoot = new Function(METHOD_INTERP_ANTICIPATEOVERSHOOT, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            return calculateInterpolation(new AnticipateOvershootInterpolator((float) args[3]), args);
        }
    };
    private static Function interpBounce = new Function(METHOD_INTERP_BOUNCE, 3) {
        private final Interpolator interpolator = new BounceInterpolator();

        public double apply(double... args) {
            return calculateInterpolation(this.interpolator, args);
        }
    };
    private static Function interpCycle = new Function(METHOD_INTERP_CYCLE, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            return calculateInterpolation(new CycleInterpolator((float) args[3]), args);
        }
    };
    private static Function interpDecel = new Function(METHOD_INTERP_DECEL, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            return calculateInterpolation(new DecelerateInterpolator((float) args[3]), args);
        }
    };
    private static Function interpFastOutLinearIn = new Function(METHOD_INTERP_FASTOUTLINEARIN, 3) {
        private final Interpolator interpolator = new FastOutLinearInInterpolator();

        public double apply(double... args) {
            return calculateInterpolation(this.interpolator, args);
        }
    };
    private static Function interpFastOutSlowIn = new Function(METHOD_INTERP_FASTOUTSLOWIN, 3) {
        private final Interpolator interpolator = new FastOutSlowInInterpolator();

        public double apply(double... args) {
            return calculateInterpolation(this.interpolator, args);
        }
    };
    private static Function interpLinear = new Function(METHOD_INTERP_LINEAR, 3) {
        private final Interpolator interpolator = new LinearInterpolator();

        public double apply(double... args) {
            return calculateInterpolation(this.interpolator, args);
        }
    };
    private static Function interpLinearOutSlowIn = new Function(METHOD_INTERP_LINEAROUTSLOWIN, 4) {
        private final Interpolator interpolator = new LinearOutSlowInInterpolator();

        public double apply(double... args) {
            return calculateInterpolation(this.interpolator, args);
        }
    };
    private static Function interpOvershoot = new Function(METHOD_INTERP_OVERSHOOT, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            return calculateInterpolation(new OvershootInterpolator((float) args[3]), args);
        }
    };
    private static Function orbitX = new Function(METHOD_ORBITX, 2) {
        public double apply(double... args) {
            if (args == null || args.length < 2) {
                return 0d;
            }
            double theta = args[0];
            return Math.cos(theta) * args[1];
        }
    };
    private static Function orbitY = new Function(METHOD_ORBITY, 2) {
        public double apply(double... args) {
            if (args == null || args.length < 2) {
                return 0d;
            }
            double theta = args[0];
            return Math.sin(theta) * args[1];
        }
    };
    private static Function rad = new Function(METHOD_RAD) {
        public double apply(double... doubles) {
            return Math.toRadians(doubles[0]);
        }
    };
    private static Function rand = new Function(METHOD_RAND, 2) {
        public double apply(double... args) {
            if (args.length < 2) {
                return 0d;
            }
            double upper = args[1] >= args[0] ? args[1] : args[0];
            double lower = args[1] >= args[0] ? args[0] : args[1];
            return (double) Math.round((Math.random() * Math.abs(upper - lower)) + lower);
        }
    };
    private static Function round = new Function(METHOD_ROUND) {
        public double apply(double... args) {
            return (double) Math.round(args[0]);
        }
    };
    private static Function squareWave = new Function(METHOD_SQUAREWAVE, 4) {
        public double apply(double... args) {
            if (args == null || args.length < 4) {
                return 0d;
            }
            double current = args[0];
            return Math.signum(Math.sin((6.283185307179586d * (current - args[3])) / args[2])) * args[1];
        }
    };

    static {
        formatter.setMaximumFractionDigits(15);
        formatter.setMinimumIntegerDigits(1);
        formatter.setGroupingUsed(false);
    }

    public static String processMath(String string) {
        return parseEquations(parseMethods(string));
    }

    private static String parseMethods(String evaluatable) {
        if (evaluatable == null || evaluatable.trim().length() <= 0) {
            return null;
        }
        String output = evaluatable.trim();
        if (output.length() <= 0) {
            return output;
        }
        List<String> methodList = new ArrayList<>();
        for (String method : METHOD_LIST) {
            if (output.contains(method)) {
                methodList.add(method);
            }
        }
        int latestIndex;
        do {
            latestIndex = -1;
            for (String method2 : methodList) {
                int lastMethodIndex = output.lastIndexOf(method2);
                if (lastMethodIndex > latestIndex) {
                    latestIndex = lastMethodIndex;
                }
            }
            if (latestIndex >= 0) {
                String methodString = output.substring(latestIndex);
                int beginningParanIndex = -1;
                int endingParenIndex = -1;
                int depth = 0;
                int i = 0;
                while (i < methodString.length()) {
                    if (methodString.charAt(i) == '(' || methodString.charAt(i) == '[') {
                        if (depth == 0) {
                            beginningParanIndex = i;
                        }
                        depth++;
                    } else if (methodString.charAt(i) == ')' || methodString.charAt(i) == ']') {
                        depth--;
                        if (depth == 0) {
                            endingParenIndex = i;
                            break;
                        }
                    }
                    i++;
                }
                if (endingParenIndex < 0) {
                    return output;
                }
                StringBuilder compiledMethodBuilder = new StringBuilder();
                compiledMethodBuilder.append(methodString.substring(0, beginningParanIndex));
                compiledMethodBuilder.append("(");
                String[] params = methodString.substring(beginningParanIndex + 1, endingParenIndex).split(PARAM_SEPARATOR);
                for (i = 0; i < params.length; i++) {
                    if (i > 0) {
                        compiledMethodBuilder.append(PARAM_SEPARATOR);
                    }
                    String evaluatedParam = parseEquations(params[i]);
                    if (evaluatedParam != null) {
                        compiledMethodBuilder.append(evaluatedParam);
                    }
                }
                compiledMethodBuilder.append(")");
                output = output.substring(0, latestIndex) + eval(compiledMethodBuilder.toString()) + methodString.substring(endingParenIndex + 1);
            }
        } while (latestIndex >= 0);
        return output;
    }
    public static String parseEquations(String evaluatable) {
        if (evaluatable == null || evaluatable.trim().length() <= 0) {
            return null;
        }
        String output = evaluatable.trim();
        if (output.length() <= 0) {
            return output;
        }
        int openingBraceIndex;
        do {
            openingBraceIndex = Math.max(output.lastIndexOf("("), output.lastIndexOf("["));
            if (openingBraceIndex < 0 || openingBraceIndex >= output.length() - 1) {
                int firstParanthBracketIndex = output.indexOf(")");
                int firstSquareBracketIndex = output.indexOf("]");
                int endingBraceIndex = -1;
                if (firstParanthBracketIndex >= 0) {
                    endingBraceIndex = firstParanthBracketIndex;
                }
                if (firstSquareBracketIndex >= 0 && endingBraceIndex >= 0) {
                    endingBraceIndex = Math.min(endingBraceIndex, firstSquareBracketIndex);
                }
                if (endingBraceIndex >= 0) {
                    output = eval(output.substring(0, endingBraceIndex)) + (endingBraceIndex < output.length() ? output.substring(endingBraceIndex + 1) : "");
                }
            } else {
                output = output.substring(0, openingBraceIndex) + parseEquations(output.substring(openingBraceIndex + 1));
            }
        } while (openingBraceIndex >= 0);
        return output;
    }



    protected static double calculateInterpolation(Interpolator interpolator, double... args) {
        if (interpolator == null || args == null || args.length < 3) {
            return 0d;
        }
        double current = args[0];
        double min = Math.min(args[1], args[2]);
        float interpValue = 0.0f;
        double range = Math.max(args[1], args[2]) - min;
        if (Double.doubleToRawLongBits(range) != 0) {
            interpValue = (float) MathUtils.clamp((current - min) / range, (double) 0d, 1.0d);
        }
        return (double) interpolator.getInterpolation(interpValue);
    }

    protected static double heaviside(double input) {
        return 0.5d * (1.0d - Math.signum(input));
    }

    private static String eval(String text) {
        try {
            String result = formatter.format(new ExpressionBuilder(text).variables("pi", "e").functions(round, rand, rad, deg, clamp, orbitX, orbitY, interpLinear, interpAccel, interpDecel, interpAccelDecel, interpAnticipate, interpOvershoot, interpAnticipateOvershoot, interpBounce, interpCycle, interpFastOutLinearIn, interpFastOutSlowIn, interpLinearOutSlowIn, heaviside, boxcar, squareWave).build().setVariable("pi", 3.141592653589793d).setVariable("e", 2.718281828459045d).evaluate());
            return result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
        } catch (IllegalArgumentException e) {
            return "0";
        } catch (EmptyStackException e2) {
            e2.printStackTrace();
            return "0";
        }
    }

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
                return Double.toString(((double) java.lang.System.currentTimeMillis()) / 1000.0d);
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
                case 'W':
                case 'w':
                    text =  text.replace(tempString, processWeather(tempString)); //WeatherParser
            }
        }
        if (text.contains("(") || text.contains(")") || text.contains("$")) {
            return processFinal(processMath(text));
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
