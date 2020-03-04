package com.liner.facerengineview.Engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Xml;
import android.view.View;

import androidx.annotation.Nullable;

import com.liner.facerengineview.FileUtils;
import com.liner.facerengineview.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class RenderView extends View implements Runnable {
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_LINE = 3;
    public static final int SHAPE_POLYGON = 2;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_TRIANGLE = 4;
    private Context context;
    private ArrayList<HashMap<String, String>> watchFaceLayers;
    private HashMap<String, BitmapDrawable> drawableHashMap;
    private HashMap<String, Typeface> typefaceHashMap;
    private ParserUtils parserUtils;
    private Thread mThread;
    private boolean isRunning = false;
    private int UPDATE_FREQ = 50;
    private boolean isProtected;
    private boolean canvasInited;
    private boolean isRoundWatch = true;
    private boolean isLowPower = false;
    private float renderScale = 2f;
    private Paint strokePaint = new Paint();
    private Paint canvasPaint = new Paint();
    private Path canvasPath = new Path();



    private static ClockSkinLayer clockSkinLayer;
    private static ArrayList<ClockSkinLayer> clockSkinLayers = null;
    private static long startAnimationTime = 0;
    private long animationTimeCount = 0;
    private static Drawable mDrawBattery;
    private static Drawable mDrawBatteryGray;
    private int milliSecond = 0;
    private Movie backgroundRender;
    private long renderStart = 0;
    private int renderCurrentTime = 0;
    private float renderLeft = 0f;
    private float renderTop = 0f;
    private boolean isAmbient = false;
    private boolean isHWAccelerated = true;

    private static int frame = 0;
    private static long lastTime = 0;



    private int WATCHFACE_TYPE; //0-Facer, 1-ClockSkin;

    public RenderView(Context context) {
        super(context);
        this.context = context;
        mDrawBattery = context.getResources().getDrawable(R.drawable.clock_battery_panel);
        mDrawBatteryGray = context.getResources().getDrawable(R.drawable.clock_battery_panel_gray);
    }

    public RenderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        mDrawBattery = context.getResources().getDrawable(R.drawable.clock_battery_panel);
        mDrawBatteryGray = context.getResources().getDrawable(R.drawable.clock_battery_panel_gray);
    }

    public void init(File file){
        WATCHFACE_TYPE = 0;
        setLayerType(LAYER_TYPE_HARDWARE, canvasPaint);
        watchFaceLayers = new ArrayList<>();
        drawableHashMap = new HashMap<>();
        typefaceHashMap = new HashMap<>();
        if (file.exists()) {
            String manifest = FileUtils.read(FileUtils.getFileFromZip(file, "watchface.json"));
            String description = FileUtils.read(FileUtils.getFileFromZip(file, "description.json"));
            JSONArray watchFace;
            try {
                isProtected = isProtected(new JSONObject(description));
                if (isProtected) {
                    if(manifest.contains("\"id\":")){
                        watchFace = new JSONArray(manifest);
                    } else {
                        watchFace = new JSONArray(new String(Base64.decode(manifest, 0), StandardCharsets.UTF_8));
                    }
                } else {
                    watchFace = new JSONArray(manifest);
                }
                parserUtils = new ParserUtils(context, watchFace);
                watchFaceLayers.clear();
                drawableHashMap.clear();
                typefaceHashMap.clear();
                for(int i = 0; i<watchFace.length(); i++){
                    try {
                        JSONObject layerJson = watchFace.getJSONObject(i);
                        HashMap<String, String> layer = new HashMap<>();
                        Iterator<String> iterator = layerJson.keys();
                        while (iterator.hasNext()){
                            String layerKey = iterator.next();
                            layer.put(layerKey, String.valueOf(layerJson.get(layerKey)));
                        }
                        insertImageHash(layer, file, "hash");
                        insertImageHash(layer, file, "hash_round");
                        insertImageHash(layer, file, "hash_round_ambient");
                        insertImageHash(layer, file, "hash_square");
                        insertImageHash(layer, file, "hash_square_ambient");
                        if (layer.containsKey("new_font_name")) {
                            try {
                                typefaceHashMap.put(layer.get("new_font_name"), Typeface.createFromFile(FileUtils.getFileFromZip(file, "fonts/"+layer.get("new_font_name"))));
                            } catch (Exception e3) {
                                e3.printStackTrace();
                            }
                        }
                        watchFaceLayers.add(layer);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public void initClockSkin(File file){
        WATCHFACE_TYPE = 1;
        clockSkinLayers = null;
        parserUtils = new ParserUtils(context);
        stopDraw();
        XmlPullParser xmlPullParser;
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(FileUtils.getFileFromZip(file, "clock_skin.xml"));
            xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");
            int eventType = xmlPullParser.getEventType();
            boolean isDone = false;
            String localName;
            while (!isDone) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        stopDraw();
                        if (clockSkinLayers == null) {
                            clockSkinLayers = new ArrayList<>();
                        } else {
                            clockSkinLayers = null;
                            clockSkinLayers = new ArrayList<>();
                        }
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        isDone = true;
                        isHWAccelerated = clockSkinLayers.get(0).getBackgroundStream() != null;
                        if(isHWAccelerated) {
                            backgroundRender = Movie.decodeStream(clockSkinLayers.get(0).getBackgroundStream());
                            if (backgroundRender != null) {
                                int movieWidth = backgroundRender.width();
                                int movieHeight = backgroundRender.height();
                                renderScale = (float) getWidth() / (float) (Math.min(movieHeight, movieWidth));
                                renderLeft = (((((float) getWidth() - (movieWidth * renderScale))) / 2f)) / renderScale;
                                if(getHeight() < getWidth()){
                                    renderTop = (((((float) getWidth() - (movieHeight * renderScale))) / 2f)) / renderScale;
                                } else {
                                    renderTop = (((((float) getHeight() - (movieHeight * renderScale))) / 2f)) / renderScale;
                                }
                            }
                        }
                        startDraw();
                        break;

                    case XmlPullParser.START_TAG: {
                        localName = xmlPullParser.getName();
                        if (ClockEngineConstants.TAG_DRAWABLE.equals(localName)) {
                            clockSkinLayer = new ClockSkinLayer();
                        } else if (clockSkinLayer != null) {
                            switch (localName){
                                case ClockEngineConstants.TAG_NAME:
                                    localName = xmlPullParser.nextText();
                                    int index = localName.lastIndexOf(".");
                                    if(localName.contains(".gif")){
                                        InputStream io;
                                        io = new FileInputStream(Objects.requireNonNull(FileUtils.getFileFromZip(file, localName)));
                                        try {
                                            clockSkinLayer.setBackgroundStream(io);
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        if (index > 0) {
                                            if (ClockEngineConstants.TAG_DRAWABLE_FILE_TYPE.equalsIgnoreCase(localName.substring(index + 1))) {
                                                parseDrawableArray(clockSkinLayer, localName, file);
                                            } else if (ClockEngineConstants.TAG_DRAWABLE_TYPE.equalsIgnoreCase(localName.substring(index + 1))) {
                                                Bitmap bmp;
                                                clockSkinLayer.setBackgroundStream(null);
                                                bmp = BitmapFactory.decodeStream(new FileInputStream(Objects.requireNonNull(FileUtils.getFileFromZip(file, localName))));
                                                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                                                try {
                                                    bmp.setDensity(dm.densityDpi);
                                                } catch (RuntimeException e) {
                                                    e.printStackTrace();
                                                }
                                                //bmp = BitmapShaderUtil.replaceColor(bmp, Color.BLACK, Color.CYAN);
                                                BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bmp);
                                                try {
                                                    //bitmapDrawable = BitmapShaderUtil.setSaturation(bitmapDrawable, 0f);
                                                    clockSkinLayer.setBackgroundDrawable(bitmapDrawable);
                                                } catch (NullPointerException e) {
                                                    stopDraw();
                                                    e.printStackTrace();
                                                    isDone = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case ClockEngineConstants.TAG_CENTERX:
                                    clockSkinLayer.setPositionX(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_CENTERY:
                                    clockSkinLayer.setPositionY(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_ROTATE:
                                    clockSkinLayer.setRotate(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_MUL_ROTATE:
                                    clockSkinLayer.setMulRotate(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_OFFSET_ANGLE:
                                    clockSkinLayer.setAngle(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_ARRAY_TYPE:
                                    clockSkinLayer.setArrayType(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_COLOR:
                                    clockSkinLayer.setColor(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_START_ANGLE:
                                    clockSkinLayer.setStartAngle(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_DIRECTION:
                                    clockSkinLayer.setDirection(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_TEXT_SIZE:
                                    clockSkinLayer.setTextsize(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_COLOR_ARRAY:
                                    clockSkinLayer.setColorArray(xmlPullParser.nextText());
                                    break;
                                case ClockEngineConstants.TAG_COUNT:
                                    int count = Integer.valueOf(xmlPullParser.nextText());
                                    ArrayList<ClockSkinLayer.DrawableInfo> infoArrayList = new ArrayList<>();
                                    for (int i = 0; i < count; i++) {
                                        float scale = new Random().nextFloat() < 0.1f ? 0.1f:new Random().nextFloat()*renderScale;
                                        ClockSkinLayer.DrawableInfo layer = new ClockSkinLayer.DrawableInfo();
                                        layer.setDrawable(scaleDrawable(clockSkinLayer.getBackgroundDrawable(), scale));
                                        layer.setDrawableScale(scale);
                                        layer.setRotateSpeed(new Random().nextFloat()/2);
                                        layer.setDrawableX(new Random().nextInt(getWidth()));
                                        layer.setDrawableY(new Random().nextInt(getHeight()));
                                        infoArrayList.add(layer);
                                    }
                                    clockSkinLayer.setClockEngineInfos(infoArrayList);
                                    break;
                                case ClockEngineConstants.TAG_DURATION:
                                    int i = Integer.valueOf(xmlPullParser.nextText());
                                    clockSkinLayer.setDuration(i);
                                    break;
                                case ClockEngineConstants.TAG_FRAMERATE:
                                    clockSkinLayer.setFramerateClockSkin((float) xmlPullParser.next());
                                    break;
                                case ClockEngineConstants.TAG_VALUE_TYPE:
                                    clockSkinLayer.setValusType(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_PROGRESS_DILIVER_COUNT:
                                    clockSkinLayer.setProfressDiliverCount(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_PROGRESS_DILIVER_ARC:
                                    clockSkinLayer.setProgressDiliverArc(Float.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_PROGRESS_RADIUS:
                                    clockSkinLayer.setCircleRadius(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_PROGRESS_STROKEN:
                                    clockSkinLayer.setCircleStroken(Integer.valueOf(xmlPullParser.nextText()));
                                    break;
                                case ClockEngineConstants.TAG_PICTURE_SHADOW:
                                    Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(Objects.requireNonNull(FileUtils.getFileFromZip(file, xmlPullParser.nextText()))));
                                    DisplayMetrics dm = context.getResources().getDisplayMetrics();
                                    try {
                                        bmp.setDensity(dm.densityDpi);
                                    }catch (RuntimeException e){
                                        e.printStackTrace();
                                    }
                                    BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bmp);
                                    clockSkinLayer.setShadowDrawable(bitmapDrawable);
                                    break;
                                case ClockEngineConstants.TAG_PACKAGE_NAME:
                                    clockSkinLayer.setPackageName(xmlPullParser.nextText());
                                    break;
                                case ClockEngineConstants.TAG_CLASS_NAME:
                                    clockSkinLayer.setClassName(xmlPullParser.nextText());
                                    break;
                                case ClockEngineConstants.TAG_RANGE:
                                    clockSkinLayer.setRange(Integer.parseInt(xmlPullParser.nextText()));
                                    break;
                            }
                        }
                    }
                    break;
                    case XmlPullParser.END_TAG: {
                        localName = xmlPullParser.getName();
                        if (ClockEngineConstants.TAG_DRAWABLE.equals(localName)) {
                            if (clockSkinLayer != null) {
                                if (clockSkinLayer.getArrayType() == ClockEngineConstants.ARRAY_ROTATE_ANIMATION) {
                                    clockSkinLayer.setCurrentAngle(clockSkinLayer.getStartAngle());
                                    if (clockSkinLayer.getDuration() != 0) {
                                        clockSkinLayer.setRotateSpeed(clockSkinLayer.getAngle() * 1.0f / clockSkinLayer.getDuration());
                                    }
                                    clockSkinLayer.setDirection(1);
                                }
                                try {
                                    clockSkinLayers.add(clockSkinLayer);
                                } catch (NullPointerException e){
                                    e.printStackTrace();
                                }
                                clockSkinLayer = null;
                            }
                        }

                    }
                    break;
                }
                eventType = xmlPullParser.next();
            }
            inputStream.close();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }
    private void parseDrawableArray(ClockSkinLayer paramDrawableInfo, String arrayName, File file) {
        int eventType;
        String itemName;
        ArrayList<Drawable> drawables = null;
        ArrayList<Integer> durations = null;
        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            InputStream inputStream = new FileInputStream(FileUtils.getFileFromZip(file, arrayName));
            xmlPullParser.setInput(inputStream, "UTF-8");
            eventType = xmlPullParser.getEventType();
            boolean isDone = false;
            while (!isDone) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        if (drawables == null) {
                            drawables = new ArrayList<>();
                        }
                        if (durations == null) {
                            durations = new ArrayList<>();
                        }
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        isDone = true;
                        paramDrawableInfo.setDrawableArrays(drawables);
                        paramDrawableInfo.setDurationArrays(durations);
                        break;
                    case XmlPullParser.START_TAG:
                        itemName = xmlPullParser.getName();
                        if (ClockEngineConstants.TAG_IMAGE.equals(itemName)) {
                            File drawableFile = FileUtils.getFileFromZip(file, xmlPullParser.nextText());
                            if(drawableFile != null){
                                Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(drawableFile));
                                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                                bmp.setDensity(dm.densityDpi);
                                BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), bmp);
                                if (drawables != null) {
                                    drawables.add(bitmapDrawable);
                                }
                            }
                        } else if (ClockEngineConstants.TAG_DURATION.equals(itemName)) {
                            int i = Integer.valueOf(xmlPullParser.nextText());
                            animationTimeCount +=i;
                            if (durations != null) {
                                durations.add(i);
                            }
                        }else if(ClockEngineConstants.TAG_CHILD_FOLDER.equals(itemName)){
                            paramDrawableInfo.setChildrenFolderName(xmlPullParser.nextText());
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }
            inputStream.close();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (WATCHFACE_TYPE){
            case 0:
                renderScale = (float)getWidth() / 320f;
                if(parserUtils != null && watchFaceLayers != null){
                    if (isRoundWatch) {
                        canvasPath.reset();
                        canvasPath.addCircle((float) (getWidth() / 2), (float) (getWidth() / 2), (float) (getWidth() / 2), Path.Direction.CCW);
                        canvasPath.close();
                        canvas.clipPath(canvasPath);
                    }
                    strokePaint.setStrokeWidth(6.0f);
                    strokePaint.setStyle(Paint.Style.STROKE);
                    strokePaint.setColor(Color.parseColor("#009957"));
                    if (!canvasInited) {
                        canvasInited = true;
                    }
                    canvas.drawColor(Color.parseColor("#ff000000"));
                    canvasPaint.reset();
                    if (watchFaceLayers.size() != 0) {
                        for (HashMap<String, String> layerData : watchFaceLayers) {
                            if (layerData.containsKey("type")) {
                                switch (Objects.requireNonNull(layerData.get("type"))) {
                                    case "text":
                                        drawText(canvas, layerData);
                                        break;
                                    case "image":
                                        drawImage(canvas, layerData);
                                        break;
                                    case "dynamic_image":
                                        drawDynamicImage(canvas, layerData);
                                        break;
                                    case "shape":
                                        drawShape(canvas, layerData);
                                        break;
                                }
                            }
                        }
                    }
                }
                break;

            case 1:
                if (clockSkinLayers != null && clockSkinLayers.size() > 0) {
                    renderScale = (float)getWidth() / 400f;
                    canvas.translate(getWidth()/2f, getHeight()/2f);
                    try {
                        for (ClockSkinLayer layer : clockSkinLayers) {
                            layer.setRenderScale(renderScale);
                            switch (layer.getRotate()) {
                                case ClockEngineConstants.ROTATE_NONE: {
                                    if(!isAmbient) {
                                        if (layer.getArrayType() > 0) {
                                            drawClockArray(canvas, layer);
                                        } else {
                                            if (layer.getBackgroundStream() != null) {
                                                if (backgroundRender != null)
                                                    drawBackgroundFrame(canvas, renderScale, renderLeft, renderTop);
                                            } else {
                                                if (layer.getArrayType() == 0 && layer.getDrawableArrays() != null && layer.getDrawableArrays().size() > 0) {
                                                    drawClockQuietPicture(canvas, layer.getDrawableArrays().get(frame), layer.getPositionX(), layer.getPositionY());
                                                    long cTime = System.currentTimeMillis();
                                                    if (cTime - lastTime > ((long) (1000 / layer.getFramerate()))) {
                                                        lastTime = cTime;
                                                        frame++;
                                                        if (frame >= layer.getDrawableArrays().size()) {
                                                            frame = 0;
                                                        }
                                                    }
                                                } else if (layer.getBackgroundDrawable() != null) {
                                                    drawClockQuietPicture(canvas, layer.getBackgroundDrawable(),
                                                            layer.getPositionX(), layer.getPositionY());
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                                case ClockEngineConstants.ROTATE_HOUR: {
                                    float hourAngle = hour / 12.0f * 360.0f;
                                    hourAngle += (float) (minute * 30 / 60);
                                    if (layer.getDirection() == ClockEngineConstants.ANTI_ROTATE_CLOCKWISE) {
                                        hourAngle = -hourAngle;
                                    }
                                    drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                            layer.getPositionY(), hourAngle + layer.getAngle());
                                    if (layer.getShadowDrawable() != null) {
                                        drawClockRotatePictureNew(canvas, layer.getShadowDrawable(), layer.getPositionX(),
                                                layer.getPositionY(), hourAngle + layer.getAngle());
                                    }
                                }
                                break;
                                case ClockEngineConstants.ROTATE_MINUTE: {
                                    float minuteAngle = minute / 60.0f * 360.0f;
                                    if (layer.getDirection() == ClockEngineConstants.ANTI_ROTATE_CLOCKWISE) {
                                        minuteAngle = -minuteAngle;
                                    }
                                    drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                            layer.getPositionY(), minuteAngle + layer.getAngle());
                                    if (layer.getShadowDrawable() != null) {
                                        drawClockRotatePictureNew(canvas, layer.getShadowDrawable(), layer.getPositionX(),
                                                layer.getPositionY(), minuteAngle + layer.getAngle());
                                    }
                                }
                                break;
                                case ClockEngineConstants.ROTATE_SECOND:
                                    if(!isAmbient) {
                                        float secondAngle = (second * 1000 + milliSecond) * 6.0f / 1000.0f;
                                        if (layer.getMulRotate() > 0) {
                                            secondAngle *= layer.getMulRotate();
                                        } else if (layer.getMulRotate() < 0) {
                                            secondAngle = secondAngle / (Math.abs(layer.getMulRotate()));
                                        }
                                        drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                layer.getPositionY(), secondAngle + layer.getAngle());
                                        if (layer.getShadowDrawable() != null) {
                                            drawClockRotatePictureNew(canvas, layer.getShadowDrawable(), layer.getPositionX(),
                                                    layer.getPositionY(), secondAngle + layer.getAngle());
                                        }
                                    }
                                    break;
                                case ClockEngineConstants.ROTATE_MONTH:
                                    if(!isAmbient)
                                        if (layer.getBackgroundDrawable() != null) {
                                            float angle = (month + 1) * 30.0F + day * 30.0F / Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                                            drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                    layer.getPositionY(), angle + layer.getAngle());
                                        }
                                    break;
                                case ClockEngineConstants.ROTATE_WEEK:
                                    if(!isAmbient)
                                        if (layer.getBackgroundDrawable() != null) {
                                            float angle = (dayOfWeek + hour * 1.0F / 24) * 360.0F / 7;
                                            drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                    layer.getPositionY(), angle + layer.getAngle());
                                        }
                                    break;
                                case ClockEngineConstants.ROTATE_BATTERY:
                                    if(!isAmbient)
                                        if (layer.getBackgroundDrawable() != null) {
                                            int direction = layer.getDirection();
                                            final float startoffset = layer.getProgressDiliverArc();
                                            if (startoffset == 0) {
                                                float angle = Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")) * 180.0F / 100;
                                                drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                        layer.getPositionY(), angle * ((direction == 1) ? 1 : -1) + layer.getAngle());
                                            } else {
                                                float angle1 = Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")) * (180.0F - startoffset * 2) / 100;
                                                drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                        layer.getPositionY(), angle1 * ((direction == 1) ? 1 : -1) + layer.getAngle() + startoffset * ((direction == 1) ? 1 : -1));
                                            }
                                        }
                                    break;
                                case ClockEngineConstants.ROTATE_DAY_NIGHT:
                                    if(!isAmbient)
                                        if (layer.getBackgroundDrawable() != null) {
                                            float angle = hour * 15.0F + minute / 60.0F * 15.0F;
                                            drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                    layer.getPositionY(), angle + layer.getAngle());
                                        }
                                    break;
                                case ClockEngineConstants.ROTATE_HOUR_BG:
                                    if(!isAmbient) {
                                        float hourAngle = hour / 24.0f * 360.0f;
                                        hourAngle += (float) (minute * 30 / 60);
                                        drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                layer.getPositionY(), hourAngle + layer.getAngle());
                                    }
                                    break;
                                case ClockEngineConstants.ROTATE_BATTERY_CIRCLE:
                                    if(!isAmbient){
                                        if (layer.getBackgroundDrawable() != null) {
                                            int direction = layer.getDirection();
                                            final float startoffset = layer.getProgressDiliverArc();
                                            if (startoffset == 0) {
                                                float angle = Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")) * 360.0F / 100;
                                                drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                        layer.getPositionY(), angle * ((direction == 1) ? 1 : -1) + layer.getAngle());
                                            } else {
                                                float angle1 = Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")) * (360.0F - startoffset * 2) / 100;
                                                drawClockRotatePictureNew(canvas, layer.getBackgroundDrawable(), layer.getPositionX(),
                                                        layer.getPositionY(), angle1 * ((direction == 1) ? 1 : -1) + layer.getAngle() + startoffset * ((direction == 1) ? 1 : -1));
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                        e.printStackTrace();
                        clockSkinLayers = null;
                    }
                }
                break;

        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                switch (WATCHFACE_TYPE){
                    case 0:
                        parserUtils.updateTagValues();
                        break;
                    case 1:
                        tick();
                        break;
                }
                postInvalidate();
                Thread.sleep(UPDATE_FREQ);
            } catch (InterruptedException localInterruptedException) {
                localInterruptedException.printStackTrace();
            }
        }
    }

    public void makeDrawCall(){
        invalidate();
    }

    public void startDraw(){
        isRunning = true;
        if(mThread != null && mThread.isAlive()){
            return;
        }
        mThread = new Thread(this);
        mThread.start();
    }
    public void startDraw(int updateFreq){
        this.UPDATE_FREQ = updateFreq;
        this.
        isRunning = true;
        if(mThread != null && mThread.isAlive()){
            return;
        }
        mThread = new Thread(this);
        mThread.start();
    }
    public void stopDraw(){
        isRunning = false;
    }

    //Control type
    public boolean isLowPower() {
        return isLowPower;
    }
    public boolean isRoundWatch() {
        return isRoundWatch;
    }
    public void setLowPower(boolean lowPower) {
        this.isLowPower = lowPower;
    }
    public void setRoundWatch(boolean roundWatch) {
        this.isRoundWatch = roundWatch;
    }

    //Draw methods
    private void drawText(Canvas canvas, HashMap<String, String> layerData){
        float alpha;
        String text;
        canvasPaint.reset();
        try {
            alpha = Math.round(2.55d * parserUtils.getTAGFloatValue(layerData.get("opacity")));
        } catch (NumberFormatException e){
            alpha = 255f;
        }
        if(alpha != 0f){
            if(layerData.containsKey("text")){
                text = parserUtils.getTAGValue(layerData.get("text"));
            } else {
                text = "_ERR_";
            }
            if(layerData.containsKey("transform")) {
                if (Objects.requireNonNull(layerData.get("transform")).matches("1")) {
                    text = text.replace(text, text.toUpperCase());
                } else if (Objects.requireNonNull(layerData.get("transform")).matches("2")) {
                    text = text.replace(text, text.toLowerCase());
                }
            }
            if(isLowPower){
                if(layerData.containsKey("low_power_color")){
                    canvasPaint.setColor(Integer.parseInt(Objects.requireNonNull(layerData.get("low_power_color"))));
                } else {
                    canvasPaint.setColor(-1);
                }
            } else {
                if (layerData.containsKey("color")) {
                    canvasPaint.setColor(Integer.parseInt(Objects.requireNonNull(layerData.get("color"))));
                } else {
                    canvasPaint.setColor(-1);
                }
            }
            int strokeColor;
            boolean shouldStroke;
            if(layerData.containsKey("text_effect")){
                shouldStroke = false;
                strokeColor = Color.WHITE;
                canvasPaint.clearShadowLayer();
                switch (Integer.valueOf(Objects.requireNonNull(layerData.get("text_effect")))){
                    case 1:
                        if(layerData.containsKey("stroke_size")){
                            canvasPaint.setStrokeWidth(Float.parseFloat(Objects.requireNonNull(layerData.get("stroke_size"))));
                        }
                        if (layerData.containsKey("stroke_color")) {
                            strokeColor = Integer.parseInt(Objects.requireNonNull(layerData.get("stroke_color")));
                        }
                        shouldStroke = true;
                        break;
                    case 2:
                        if(!layerData.containsKey("glow_size") || !layerData.containsKey("glow_color")){
                            break;
                        }
                        canvasPaint.setShadowLayer((float) Integer.valueOf(Objects.requireNonNull(layerData.get("glow_size"))), 0.0f, 0.0f, Integer.valueOf(Objects.requireNonNull(layerData.get("glow_color"))));
                        shouldStroke = true;
                        break;
                    default:
                        break;
                }
            } else {
                shouldStroke = false;
                strokeColor = Color.WHITE;
                canvasPaint.clearShadowLayer();
            }
            if(layerData.containsKey("size")){
                try {
                    canvasPaint.setTextSize(renderScale*parserUtils.getTAGFloatValue(layerData.get("size")));
                } catch (NumberFormatException e){
                    canvasPaint.setTextSize(1);
                }
            }
            if(alpha > 255f || alpha < 0f){
                canvasPaint.setAlpha(255);
            } else {
                canvasPaint.setAlpha(Math.round(alpha));
            }
            if (layerData.containsKey("alignment")) {
                if (Objects.requireNonNull(layerData.get("alignment")).matches("0")) {
                    canvasPaint.setTextAlign(Paint.Align.LEFT);
                } else {
                    if (Objects.requireNonNull(layerData.get("alignment")).matches("1")) {
                        canvasPaint.setTextAlign(Paint.Align.CENTER);
                    } else {
                        if (Objects.requireNonNull(layerData.get("alignment")).matches("2")) {
                            canvasPaint.setTextAlign(Paint.Align.RIGHT);
                        } else {
                            canvasPaint.setTextAlign(Paint.Align.LEFT);
                        }
                    }
                }
            }
            if (layerData.containsKey("new_font_name")) {
                if (typefaceHashMap.containsKey(layerData.get("new_font_name"))) {
                    canvasPaint.setTypeface(typefaceHashMap.get(layerData.get("new_font_name")));
                }
            }
            if(layerData.containsKey("r")){
                canvas.save();
                canvas.rotate(parserUtils.getTAGFloatValue(layerData.get("r")), parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale, parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale);
            }
            canvasPaint.setAntiAlias(true);
            if(shouldStroke){
                canvasPaint.setStyle(Paint.Style.STROKE);
                canvasPaint.setColor(strokeColor);
                if(isLowPower){
                    if(layerData.containsKey("low_power")){
                        if (Boolean.valueOf(layerData.get("low_power"))) {
                            canvas.drawText(text, parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale, parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale, canvasPaint);
                            canvasPaint.setStyle(Paint.Style.FILL);
                            canvasPaint.setColor(Color.BLACK);
                        }
                    }
                } else {
                    canvas.drawText(text, parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale, parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale, canvasPaint);
                }
            } else {
                if (isLowPower) {
                    if (layerData.containsKey("low_power")) {
                        if (Boolean.valueOf(layerData.get("low_power"))) {
                            canvas.drawText(text, parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale, parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale, canvasPaint);
                        }
                    }
                }
                if (!isLowPower) {
                    canvas.drawText(text, parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale, parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale, canvasPaint);
                }
            }
            canvas.restore();
            canvasPaint.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void drawImage(Canvas canvas, HashMap<String, String> layerData){
        BitmapDrawable mBitmap;
        float alpha;
        canvasPaint.reset();
        canvasPaint.setAntiAlias(true);
        try {
            alpha = parserUtils.getTAGFloatValue(layerData.get("opacity"));
        } catch (NumberFormatException e6) {
            alpha = 100.0f;
        }
        alpha = (float) Math.round(((double) alpha) * 2.55d);
        if (alpha != 0.0f) {
            if (layerData.containsKey("hash")) {
                mBitmap = drawableHashMap.get(layerData.get("hash"));
                float tempX = parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale;
                float tempY = parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale;
                float tempWidth = parserUtils.getTAGFloatValue(layerData.get("width")) * renderScale;
                float tempHeight = parserUtils.getTAGFloatValue(layerData.get("height")) * renderScale;
                float tempR = parserUtils.getTAGFloatValue(layerData.get("r"));
                int tmpOffset;
                try {
                    tmpOffset = Integer.valueOf(Objects.requireNonNull(layerData.get("alignment")));
                } catch (NumberFormatException e8) {
                    e8.printStackTrace();
                    tmpOffset = 0;
                }
                if (layerData.containsKey("is_tinted")) {
                    if (layerData.containsKey("tint_color") && mBitmap != null) {
                        if (Boolean.valueOf(layerData.get("is_tinted"))) {
                            mBitmap.setColorFilter(new PorterDuffColorFilter(Integer.valueOf(Objects.requireNonNull(layerData.get("tint_color"))), PorterDuff.Mode.MULTIPLY));
                        } else {
                            mBitmap.setColorFilter(null);
                        }
                    }
                }
                if (Boolean.valueOf(layerData.get("low_power")) || !isLowPower) {
                    canvas.save();
                    canvas.rotate(tempR, tempX, tempY);
                    if (mBitmap != null) {
                        mBitmap.setAlpha(Math.round(alpha));
                        calculateAlignmentOffset(tempWidth, tempHeight, tempX, tempY, tmpOffset, mBitmap);
                        mBitmap.draw(canvas);
                    }
                    canvas.restore();
                }
            }
        }
    }
    private void drawDynamicImage(Canvas canvas, HashMap<String, String> layerData){
        BitmapDrawable mBitmap;
        float alpha;
        canvasPaint.reset();
        canvasPaint.setAntiAlias(true);
        try {
            alpha = parserUtils.getTAGFloatValue(layerData.get("opacity"));
        } catch (NumberFormatException e6) {
            alpha = 100.0f;
        }
        alpha = (float) Math.round(((double) alpha) * 2.55d);
        if (alpha != 0.0f) {
            if (layerData.containsKey("hash_round")) {
                String hash = null;
                if(isRoundWatch) {
                    if (isLowPower){
                        if (layerData.containsKey("hash_round_ambient")) {
                            if (Objects.requireNonNull(layerData.get("hash_round_ambient")).length() != 0) {
                                hash = layerData.get("hash_round_ambient");
                            }
                        }
                    } else {
                        if (layerData.containsKey("hash_round")) {
                            if (Objects.requireNonNull(layerData.get("hash_round")).length() != 0) {
                                hash = layerData.get("hash_round");
                            }
                        }
                    }
                } else {
                    if (isLowPower){
                        if (layerData.containsKey("hash_square_ambient")) {
                            if (Objects.requireNonNull(layerData.get("hash_square_ambient")).length() != 0) {
                                hash = layerData.get("hash_square_ambient");
                            }
                        }
                    } else {
                        if (layerData.containsKey("hash_square")) {
                            if (Objects.requireNonNull(layerData.get("hash_square")).length() != 0) {
                                hash = layerData.get("hash_square");
                            }
                        }
                    }
                }
                mBitmap = drawableHashMap.get(hash);
                float tempX = parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale;
                float tempY = parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale;
                float tempWidth = parserUtils.getTAGFloatValue(layerData.get("width")) * renderScale;
                float tempHeight = parserUtils.getTAGFloatValue(layerData.get("height")) * renderScale;
                float tempR;
                try {
                    tempR = parserUtils.getTAGFloatValue(layerData.get("r"));
                } catch (NumberFormatException e){
                    tempR = 0f;
                }
                int tmpOffset;
                try {
                    tmpOffset = Integer.valueOf(Objects.requireNonNull(layerData.get("alignment")));
                } catch (NumberFormatException e8) {
                    e8.printStackTrace();
                    tmpOffset = 0;
                }
                if (layerData.containsKey("is_tinted")) {
                    if (layerData.containsKey("tint_color") && mBitmap != null) {
                        if (Boolean.valueOf(layerData.get("is_tinted"))) {
                            mBitmap.setColorFilter(new PorterDuffColorFilter(Integer.valueOf(Objects.requireNonNull(layerData.get("tint_color"))), PorterDuff.Mode.MULTIPLY));
                        } else {
                            mBitmap.setColorFilter(null);
                        }
                    }
                }
                if (Boolean.valueOf(layerData.get("low_power")) || !isLowPower) {
                    canvas.save();
                    canvas.rotate(tempR, tempX, tempY);
                    if (mBitmap != null) {
                        mBitmap.setAlpha(Math.round(alpha));
                        calculateAlignmentOffset(tempWidth, tempHeight, tempX, tempY, tmpOffset, mBitmap);
                        mBitmap.draw(canvas);
                    }
                    canvas.restore();
                }
            }
        }
    }
    private void drawShape(Canvas canvas, HashMap<String, String> layerData){
        float alpha;
        if(layerData.containsKey("shape_type")){
            if(Boolean.valueOf(layerData.get("low_power")) || !isLowPower){
                canvasPaint.reset();
                canvasPaint.setAntiAlias(true);
                int width = 90;
                int height = 90;
                int radius = 0;
                boolean shouldClip = false;
                int tempX = Math.round(parserUtils.getTAGFloatValue(layerData.get("x")) * renderScale);
                int tempY = Math.round(parserUtils.getTAGFloatValue(layerData.get("y")) * renderScale);
                float tempR = parserUtils.getTAGFloatValue(layerData.get("r"));
                int sides = Integer.parseInt(Objects.requireNonNull(layerData.get("sides")));
                if(layerData.containsKey("color")){
                    canvasPaint.setColor(Integer.parseInt(Objects.requireNonNull(layerData.get("color"))));
                } else {
                    canvasPaint.setColor(-1);
                }
                switch (Integer.valueOf(Objects.requireNonNull(layerData.get("shape_opt")))){
                    case 0:
                        canvasPaint.setStyle(Paint.Style.FILL);
                        break;
                    case 1:
                        canvasPaint.setStyle(Paint.Style.STROKE);
                        if(layerData.containsKey("stroke_size")){
                            canvasPaint.setStrokeWidth(parserUtils.getTAGFloatValue(layerData.get("stroke_size"))+renderScale);
                        }
                        break;
                    case 2:
                        shouldClip = true;
                        break;

                }
                if(layerData.containsKey("opacity")){
                    try {
                        alpha = parserUtils.getTAGFloatValue(layerData.get("opacity"));
                    } catch (NumberFormatException e6) {
                        alpha = 100.0f;
                    }
                    alpha = (float) Math.round(((double) alpha) * 2.55d);
                    if(alpha > 255f || alpha < 0f){
                        canvasPaint.setAlpha(255);
                    } else {
                        canvasPaint.setAlpha(Math.round(alpha));
                    }
                }
                if(layerData.containsKey("radius")){
                    radius = Math.round(parserUtils.getTAGFloatValue(layerData.get("radius"))*renderScale);
                }
                if(layerData.containsKey("width") && layerData.containsKey("height")){
                    width = Math.round(parserUtils.getTAGFloatValue(layerData.get("width")) * renderScale);
                    height = Math.round(parserUtils.getTAGFloatValue(layerData.get("height")) * renderScale);
                }
                canvas.save();
                canvas.rotate(tempR, (float)tempX, (float)tempY);
                if(!shouldClip){
                    switch (Integer.parseInt(Objects.requireNonNull(layerData.get("shape_type")))) {
                        case SHAPE_CIRCLE:
                            canvas.drawCircle((float) tempX, (float) tempY, (float) radius, canvasPaint);
                            break;
                        case SHAPE_SQUARE:
                        case SHAPE_LINE:
                            canvas.drawRect((float) tempX, (float) tempY, ((float) width) + ((float) tempX), ((float) height) + ((float) tempY), canvasPaint);
                            break;
                        case SHAPE_POLYGON:

                            canvas.drawPath(calculatePolygonPoints(renderScale, sides, radius, tempX, tempY, renderScale), canvasPaint);
                            break;
                        case SHAPE_TRIANGLE:
                            canvas.drawPath(calculatePolygonPoints(renderScale, 3, radius, tempX, tempY, renderScale), canvasPaint);
                            break;
                    }
                }
                Path tempPath = new Path();
                switch (Integer.valueOf(Objects.requireNonNull(layerData.get("shape_type")))) {
                    case SHAPE_CIRCLE:
                        tempPath.addCircle((float) tempX, (float) tempY, tempR, Path.Direction.CW);
                        tempPath.close();
                        canvas.clipPath(tempPath);
                        break;
                    case SHAPE_SQUARE:
                    case SHAPE_LINE:
                        canvas.save();
                        canvas.rotate(tempR, (float) tempX, (float) tempY);
                        tempPath.addRect((float) tempX, (float) tempY, ((float) tempX) + ((float) width), ((float) tempY) + ((float) height), Path.Direction.CW);
                        tempPath.close();
                        canvas.clipPath(tempPath);
                        canvas.restore();
                        break;
                    case SHAPE_POLYGON:
                        canvas.clipPath(calculatePolygonPoints(renderScale, sides, radius, tempX, tempY, renderScale));
                        break;
                    case SHAPE_TRIANGLE:
                        canvas.clipPath(calculatePolygonPoints(renderScale, 3, radius, tempX, tempY, renderScale));
                        break;
                }
                canvas.restore();
            }
        }
    }
    private void drawClockArray(Canvas canvas, ClockSkinLayer localDrawableInfo) {
        switch (localDrawableInfo.getArrayType()) {
            case ClockEngineConstants.ARRAY_YEAR_MONTH_DAY:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawDigitalYearMonthDay(canvas, localDrawableInfo.getDrawableArrays(),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_MONTH_DAY:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawDigitalMonthAndDay(canvas, localDrawableInfo.getDrawableArrays().get(month / 10),
                            localDrawableInfo.getDrawableArrays().get(month % 10), localDrawableInfo.getDrawableArrays().get(10),
                            localDrawableInfo.getDrawableArrays().get(day / 10),
                            localDrawableInfo.getDrawableArrays().get(day % 10), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;

            case ClockEngineConstants.ARRAY_MONTH:
                if ((localDrawableInfo.getDrawableArrays() != null) && localDrawableInfo.getDrawableArrays().size() > (month - 1)) {
                    drawDigitalOnePicture(canvas, localDrawableInfo.getDrawableArrays().get(month - 1),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_MONTH_NEW:
                if ((localDrawableInfo.getDrawableArrays() != null) && localDrawableInfo.getDrawableArrays().size() > 0) {
                    drawDigitalTwoPicture(canvas, localDrawableInfo.getDrawableArrays().get((month + 1) / 10),
                            localDrawableInfo.getDrawableArrays().get((month ) % 10),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_DAY:
                if ((localDrawableInfo.getDrawableArrays() != null) && localDrawableInfo.getDrawableArrays().size() > 0) {
                    drawDigitalTwoPicture(canvas, localDrawableInfo.getDrawableArrays().get(day / 10),
                            localDrawableInfo.getDrawableArrays().get(day % 10),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_DAY_NEW:
                if ((localDrawableInfo.getDrawableArrays() != null) && localDrawableInfo.getDrawableArrays().size() > 0) {
                    drawDigitalOnePicture(canvas, localDrawableInfo.getDrawableArrays().get(day - 1),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;

            case ClockEngineConstants.ARRAY_WEEKDAY:
                if ((localDrawableInfo.getDrawableArrays() != null) && localDrawableInfo.getDrawableArrays().size() >= dayOfWeek - 1) {
                    switch (dayOfWeek){
                        case 1:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(0),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                        case 2:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(1),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                        case 3:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(2),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                        case 4:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(3),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                        case 5:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(4),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                        case 6:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(5),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                        case 7:
                            drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(6),
                                    localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                            break;
                    }
                }
                break;
            case ClockEngineConstants.ARRAY_HOUR_MINUTE:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    Drawable amPm = null;
                    if (!DateFormat.is24HourFormat(context) && localDrawableInfo.getDrawableArrays().size() == 13) {
                        amPm = localDrawableInfo.getDrawableArrays().get(hour >= 12 ? 12 : 11);
                        hour = hour % 12;
                        if (hour == 0) {
                            hour = 12;
                        }
                    }
                    Drawable hour1 = localDrawableInfo.getDrawableArrays().get(hour / 10);
                    Drawable hour2 = localDrawableInfo.getDrawableArrays().get(hour % 10);
                    Drawable colon = localDrawableInfo.getDrawableArrays().get(10);
                    Drawable minute1 = localDrawableInfo.getDrawableArrays().get(minute / 10);
                    Drawable minute2 = localDrawableInfo.getDrawableArrays().get(minute % 10);
                    drawDigitalHourAndMinute(canvas, hour1, hour2, colon, minute1, minute2, amPm,
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), second);
                }
                break;
            case ClockEngineConstants.ARRAY_HOUR:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawDigitalTwoPicture(canvas, localDrawableInfo.getDrawableArrays().get(hour / 10),
                            localDrawableInfo.getDrawableArrays().get(hour % 10), localDrawableInfo.getPositionX(),
                            localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_MINUTE: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawDigitalTwoPicture(canvas, localDrawableInfo.getDrawableArrays().get(minute / 10),
                            localDrawableInfo.getDrawableArrays().get(minute % 10), localDrawableInfo.getPositionX(),
                            localDrawableInfo.getPositionY());
                }
            }
            break;
            case ClockEngineConstants.ARRAY_SECOND: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawDigitalTwoPicture(canvas, localDrawableInfo.getDrawableArrays().get(second / 10),
                            localDrawableInfo.getDrawableArrays().get(second % 10), localDrawableInfo.getPositionX(),
                            localDrawableInfo.getPositionY());
                }
            }
            break;
            case ClockEngineConstants.ARRAY_SECOND_NEW: {
                drawSecondCircle(canvas,  localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(),
                        second);

            }
            break;

            case ClockEngineConstants.ARRAY_WEATHER: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    try {
                        drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(weatherIcon), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                    } catch (IndexOutOfBoundsException e){
                        e.printStackTrace();
                    }
                }
            }
            break;
            case ClockEngineConstants.ARRAY_TEMPERATURE: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    Drawable minus = localDrawableInfo.getDrawableArrays().get(10);
                    Drawable temp1 = localDrawableInfo.getDrawableArrays().get(Math.abs(weatherTemp / 10));
                    Drawable temp2 = localDrawableInfo.getDrawableArrays().get(Math.abs(weatherTemp % 10));
                    Drawable tempUnit = localDrawableInfo.getDrawableArrays().get(11);
                    drawTemperature(canvas, minus, temp1, temp2, tempUnit, localDrawableInfo.getPositionX(),
                            localDrawableInfo.getPositionY(), weatherTemp < 0);
                }
            }
            break;
            case ClockEngineConstants.ARRAY_STEPS: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawStepsPicture(canvas, localDrawableInfo.getDrawableArrays(), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processHealth("#ZSC#")));
                }
            }
            break;
            case ClockEngineConstants.ARRAY_STEPS_NEW: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawStepsPictureNew(canvas, localDrawableInfo.getDrawableArrays(), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processHealth("#ZSC#")));
                }
            }
            break;
            case ClockEngineConstants.ARRAY_KCAL_NEW: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawKalPictureNew(canvas, localDrawableInfo.getDrawableArrays(), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processHealth("#ZCAL#")));
                }
            }
            break;

            case ClockEngineConstants.ARRAY_HEART_RATE: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawHeartRatePicture(canvas, localDrawableInfo.getDrawableArrays(), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processHealth("#ZHR#")));
                }
            }
            break;
            case ClockEngineConstants.ARRAY_BATTERY: {
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawBatteryPicture(canvas, localDrawableInfo.getDrawableArrays(),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")));
                }
            }
            break;
            case ClockEngineConstants.ARRAY_SPECIAL_SECOND:
                if (localDrawableInfo.getColorArray() != null) {
                    drawSpecialSecond(canvas, localDrawableInfo.getColorArray(), minute, second);
                }
                break;
            case ClockEngineConstants.ARRAY_YEAR:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawDigitalYear(canvas, localDrawableInfo.getDrawableArrays(), year, localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_BATTERY_WITH_CIRCLE:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)
                        && localDrawableInfo.getColorArray() != null) {
                    drawBatteryPictureWithCircleNew(canvas, localDrawableInfo.getDrawableArrays(),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")),
                            localDrawableInfo.getColorArray(), localDrawableInfo.getCircleRadius());
                }
                break;
            case ClockEngineConstants.ARRAY_BATTERY_WITH_CIRCLE_PIC:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    drawBatteryPictureWithCirclePic(canvas, localDrawableInfo.getDrawableArrays(),
                            localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")));
                }
                break;
            case ClockEngineConstants.ARRAY_STEPS_WITH_CIRCLE:
                if ((localDrawableInfo.getDrawableArrays() != null) &&
                        (localDrawableInfo.getDrawableArrays().size() > 0) && (localDrawableInfo.getColorArray() != null)) {
                    drawStepsPictureWithCircle(canvas, localDrawableInfo.getDrawableArrays(), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(),
                            stepCount, localDrawableInfo.getColorArray());
                }
                break;
            case ClockEngineConstants.ARRAY_STEPS_CIRCLE_NEW:
            {
                drawStepsCircle(canvas, localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), stepCount);
            }
            break;
            case ClockEngineConstants.ARRAY_BATTERY_CIRCLE_NEW:
            {
                if ((localDrawableInfo.getDrawableArrays() != null) &&
                        (localDrawableInfo.getDrawableArrays().size() > 0) ) {
                    drawBatteryCircle(canvas, localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(),
                            Integer.parseInt(parserUtils.processBattery(context, "#BLVL#")), localDrawableInfo.getDrawableArrays());
                }
            }
            break;

            case ClockEngineConstants.ARRAY_MOON_PHASE:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    int phase = moonPhase;
                    if(phase > 7){
                        phase = 7;
                    } else if (phase < 0){
                        phase = 0;
                    }
                    drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(phase), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                }
                break;
            case ClockEngineConstants.ARRAY_AM_PM:
                if ((localDrawableInfo.getDrawableArrays() != null) && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    if (!DateFormat.is24HourFormat(context)) {
                        int index = (hour >= 12) ? 1 : 0;
                        drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(index), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                    }
                }
                break;
            case ClockEngineConstants.ARRAY_FRAME_ANIMATION:
                Calendar calendar = Calendar.getInstance();
                if (startAnimationTime <= 0) {
                    startAnimationTime = calendar.getTimeInMillis();
                }
                if (localDrawableInfo.getDrawableArrays() != null && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    if (localDrawableInfo.getDurationArrays() != null && (localDrawableInfo.getDurationArrays().size() > 0)) {
                        if (animationTimeCount > 0) {
                            long diff = calendar.getTimeInMillis() - startAnimationTime;
                            diff = diff % animationTimeCount;
                            for (int i = 0; i < localDrawableInfo.getDurationArrays().size(); i++) {
                                if (diff < localDrawableInfo.getDurationArrays().get(i)) {
                                    drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(i), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                                    break;
                                }
                                diff -= localDrawableInfo.getDurationArrays().get(i);
                            }
                        }
                    }
                }
                break;
            case ClockEngineConstants.ARRAY_ANIMATED_ARRAY:
                if (startAnimationTime <= 0) {
                    startAnimationTime = Calendar.getInstance().getTimeInMillis();
                }
                if (localDrawableInfo.getDrawableArrays() != null && (localDrawableInfo.getDrawableArrays().size() > 0)) {
                    if (localDrawableInfo.getDurationArrays() != null && (localDrawableInfo.getDurationArrays().size() > 0)) {
                        if (localDrawableInfo.getFramerateClockSkin() > 0) {
                            long diff = Calendar.getInstance().getTimeInMillis() - startAnimationTime;
                            diff = (long) (diff % localDrawableInfo.getFramerateClockSkin());
                            for (int i = 0; i < localDrawableInfo.getDrawableArrays().size(); i++) {
                                if (diff < localDrawableInfo.getDurationArrays().get(i)) {
                                    drawClockQuietPicture(canvas, localDrawableInfo.getDrawableArrays().get(i), localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                                    break;
                                }
                                diff -= localDrawableInfo.getDurationArrays().get(i);
                            }
                        }
                    }
                }
                break;
            case ClockEngineConstants.ARRAY_ROTATE_ANIMATION:
                if (startAnimationTime <= 0) {
                    Calendar calendar2 = Calendar.getInstance();
                    startAnimationTime = calendar2.getTimeInMillis();
                }
                if (localDrawableInfo.getDirection() == ClockEngineConstants.ROTATE_CLOCKWISE) {
                    localDrawableInfo.setCurrentAngle(localDrawableInfo.getCurrentAngle() + localDrawableInfo.getRotateSpeed());
                } else {
                    localDrawableInfo.setCurrentAngle(localDrawableInfo.getCurrentAngle() - localDrawableInfo.getRotateSpeed());
                }
                if ((localDrawableInfo.getCurrentAngle() >= localDrawableInfo.getStartAngle() + localDrawableInfo.getAngle()) ||
                        localDrawableInfo.getCurrentAngle() <= localDrawableInfo.getStartAngle()) {
                    if (localDrawableInfo.getDirection() == ClockEngineConstants.ROTATE_CLOCKWISE) {
                        localDrawableInfo.setDirection(ClockEngineConstants.ANTI_ROTATE_CLOCKWISE);
                    } else {
                        localDrawableInfo.setDirection(ClockEngineConstants.ROTATE_CLOCKWISE);
                    }
                }
                if (localDrawableInfo.getBackgroundDrawable() != null) {
                    drawClockRotatePictureNew(canvas, localDrawableInfo.getBackgroundDrawable(), localDrawableInfo.getPositionX(),
                            localDrawableInfo.getPositionY(), localDrawableInfo.getCurrentAngle());

                }
                break;
            case ClockEngineConstants.ARRAY_SNOW_ANIMATION:
                if (localDrawableInfo.getBackgroundDrawable() != null && localDrawableInfo.getClockEngineInfos() != null &&
                        localDrawableInfo.getClockEngineInfos().size() > 0) {
                    if (startAnimationTime <= 0) {
                        Calendar calendar3 = Calendar.getInstance();
                        startAnimationTime = calendar3.getTimeInMillis();
                    }
                    for (ClockSkinLayer.DrawableInfo snowInfo : localDrawableInfo.getClockEngineInfos()) {
                        float y = snowInfo.getDrawableY() + snowInfo.getRotateSpeed();
                        if (y > getHeight()) {
                            Random random = new Random();
                            y = random.nextInt(getHeight() / 2);
                        }
                        snowInfo.setDrawableY(y);
                        int diffX = Math.abs((int) snowInfo.getDrawableX() - getWidth() / 2);
                        int diffY = Math.abs((int) snowInfo.getDrawableY() - getWidth() / 2);
                        if (diffX * diffX + diffY * diffY <= (getHeight() * getWidth() / 4)) {
                            drawClockQuietPicture(canvas, snowInfo.getDrawable(),
                                    (int) snowInfo.getDrawableX(), (int) snowInfo.getDrawableY());
                        }
                    }
                }
                break;
            case ClockEngineConstants.ARRAY_CHARGING: {
                if (Boolean.valueOf(parserUtils.processBattery(context, "#BSB#"))) {
                    int textColor = Color.WHITE;
                    Drawable chargingDrawable = mDrawBattery;
                    if (localDrawableInfo.getColor() == 1) {
                        chargingDrawable = mDrawBatteryGray;
                        textColor = Color.BLACK;
                    }
                    drawChargingInfo(canvas, chargingDrawable, localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY(), textColor);
                }
            }
            break;
            case ClockEngineConstants.ARRAY_PICTURE_HOUR:{
                final int index = hour*5 + minute%12;
                final Drawable  currDrawable = localDrawableInfo.getDrawableArrays().get(index);

                drawClockRotatePictureNew(canvas, currDrawable, localDrawableInfo.getPositionX(),
                        localDrawableInfo.getPositionY(), 0);
                break;
            }
            case ClockEngineConstants.ARRAY_PICTURE_MINUTER:{
                final Drawable currDrawable = localDrawableInfo.getDrawableArrays().get(minute);
                drawClockRotatePictureNew(canvas, currDrawable, localDrawableInfo.getPositionX(),
                        localDrawableInfo.getPositionY(), 0);
                break;
            }
            case ClockEngineConstants.ARRAY_PICTURE_SECOND:{
                final Drawable currDrawable = localDrawableInfo.getDrawableArrays().get(second);
                drawClockRotatePictureNew(canvas, currDrawable, localDrawableInfo.getPositionX(),
                        localDrawableInfo.getPositionY(), 0);
                break;
            }
            case ClockEngineConstants.ARRAY_PICTURE_HOUR_DIGITE:{
                final Drawable currDrawable = localDrawableInfo.getDrawableArrays().get(hour);
                drawClockQuietPicture(canvas, currDrawable,
                        localDrawableInfo.getPositionX(), localDrawableInfo.getPositionY());
                break;
            }
            case ClockEngineConstants.ARRAY_VALUE_WITH_PROGRESS:{
                drawValueWithProgress(canvas, localDrawableInfo.getDrawableArrays(),
                        localDrawableInfo.getValusType(), localDrawableInfo.getColorArray(),
                        localDrawableInfo.getProgressDiliverArc(), localDrawableInfo.getProfressDiliverCount(),
                        localDrawableInfo.getCenterXNew(), localDrawableInfo.getCenterYNew(),
                        localDrawableInfo.getCircleRadius(), localDrawableInfo.getCircleStroken(),
                        localDrawableInfo.getTextsize());
                break;
            }
            case ClockEngineConstants.ARRAY_VALUE_STRING:{
                drawValueString(canvas, localDrawableInfo.getValusType(),
                        localDrawableInfo.getColorArray(),
                        localDrawableInfo.getCenterXNew(), localDrawableInfo.getCenterYNew(),
                        localDrawableInfo.getTextsize());
                break;
            }
            case ClockEngineConstants.ARRAY_VALUE_WITH_CLIP_PICTURE:{
                drawValueWithClipPicture(canvas,localDrawableInfo.getValusType(),
                        localDrawableInfo.getDrawableArrays(),localDrawableInfo.getCenterXNew(), localDrawableInfo.getCenterYNew());
                break;
            }
        }
    }
    private void drawBatteryPicture(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int batteryLevel) {
        int b = batteryLevel / 100;
        int t = (batteryLevel / 10) % 10;
        int n = batteryLevel % 10;
        if (batteryLevel < 10) {
            t = 10;
            b = 10;
        }
        Drawable drawable1 = paramList.get(b);
        Drawable drawable2 = paramList.get(t);
        Drawable drawable3 = paramList.get(n);
        Drawable drawable4 = paramList.size() > 11 ? paramList.get(11) : null;
        int width, width2, width3, width4, height;
        width = getResolution(drawable1.getIntrinsicWidth());
        width2 = getResolution(drawable2.getIntrinsicWidth());
        width3 = getResolution(drawable3.getIntrinsicWidth());
        if(drawable4 != null){
            width4 = getResolution(drawable4.getIntrinsicWidth());
        } else {
            width4 = 0;
        }
        height = getResolution(drawable3.getIntrinsicHeight());
        if (b == 10 || b == 0) {
            width = 0;
        }
        centerX -= (width + width2 + width3 + width4) / 2;
        centerY -= height / 2;
        if (b > 0) {
            drawable1.setBounds(centerX, centerY, centerX + width, centerY + height);
            drawable1.draw(canvas);
            centerX += width;
        }
        drawable2.setBounds(centerX, centerY, centerX + width2, centerY + height);
        drawable2.draw(canvas);
        centerX += width2;
        drawable3.setBounds(centerX, centerY, centerX + width3, centerY + height);
        drawable3.draw(canvas);
        if (drawable4 != null) {
            centerX += width3;
            drawable4.setBounds(centerX, centerY, centerX + width4, centerY + height);
            drawable4.draw(canvas);
        }
    }
    private void drawBatteryPictureWithCirclePic(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int batteryLevel) {
        Drawable batteryPic;
        int index = 0;
        switch (batteryLevel){
            case 100:
                index = 10;
                break;
            case 90:
                index = 9;
                break;
            case 80:
                index = 8;
                break;
            case 70:
                index = 7;
                break;
            case 60:
                index = 6;
                break;
            case 50:
                index = 5;
                break;
            case 40:
                index = 4;
                break;
            case 30:
                index = 3;
                break;
            case 20:
                index = 2;
                break;
            case 10:
                index = 1;
                break;
            case 0:
                index = 0;
                break;
        }
        batteryPic = paramList.get(index);
        int width = getResolution(batteryPic.getIntrinsicWidth());
        int height = getResolution(batteryPic.getIntrinsicHeight());
        centerX -= width / 2;
        centerY -= height / 2;
        batteryPic.setBounds(centerX, centerY, centerX + width, centerY + height);
        batteryPic.draw(canvas);
    }
    private void drawBatteryPictureWithCircleNew(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int batteryLevel, String colorsInfo, int radius) {

        batteryLevel = Math.min(100, batteryLevel);
        batteryLevel = Math.max(0, batteryLevel);
        int h = batteryLevel / 100;
        int t = batteryLevel / 10 % 10;
        int g = batteryLevel % 10;
        int center = centerX;
        int highColor;
        int normalColor;
        Paint paint;
        if (batteryLevel < 10) {
            h = 10;
            t = 10;
        }
        Drawable drawable1 = paramList.get(h);
        Drawable drawable2 = paramList.get(t);
        Drawable drawable3 = paramList.get(g);
        Drawable drawable4 = paramList.size() > 11 ? paramList.get(11) : null;
        int width, width2, width3, width4, height;
        width = getResolution(drawable1.getIntrinsicWidth());
        width2 = getResolution(drawable2.getIntrinsicWidth());
        width3 = getResolution(drawable3.getIntrinsicWidth());
        if (drawable4 != null) {
            width4 = getResolution(drawable4.getIntrinsicWidth());
        } else {
            width4 = 0;
        }
        height = getResolution(drawable3.getIntrinsicHeight());
        if (h == 0) {
            width = 0;
        }
        centerX -= (width + width2 + width3 + width4) / 2;
        centerY -= height / 2;
        if (h > 0) {
            drawable1.setBounds(centerX, centerY, centerX + width, centerY + height);
            drawable1.draw(canvas);
        }
        centerX += width;
        drawable2.setBounds(centerX, centerY, centerX + width2, centerY + height);
        drawable2.draw(canvas);
        centerX += width2;
        drawable3.setBounds(centerX, centerY, centerX + width3, centerY + height);
        drawable3.draw(canvas);

        if (drawable4 != null) {
            centerX += width3;
            drawable4.setBounds(centerX, centerY, centerX + width4, centerY + height);
            drawable4.draw(canvas);
        }

        if (colorsInfo.contains(",")) {
            normalColor = 0xFF000000 | Integer.valueOf(colorsInfo.split(",")[0], 16);
            highColor = 0xFF000000 | Integer.valueOf(colorsInfo.split(",")[1], 16);
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(getResolution(3));
            paint.setStyle(Paint.Style.STROKE);
            paint.setAlpha(255);
            canvas.save();

            if (radius == 0) {
                centerY += height / 2;
                canvas.translate(center, centerY);
                //----HERE
                //canvas.scale(0.19f, 0.19f);
                canvas.rotate(180.0F);
                for (int i = 0; i < 20; i++) {
                    paint.setColor((i < batteryLevel / 5) ? highColor : normalColor);
                    canvas.drawLine(getResolution(3), centerY, getResolution(3), centerY + getResolution(7), paint);
                    canvas.rotate(18.0F, 0.0F, 0.0F);
                }
            } else {
                paint.setStrokeWidth(getResolution(7));
                centerY += (height + 5);
                canvas.translate(center, centerY);
                canvas.scale((float) 2 * radius / (centerY*2), (float) 2 * radius / (centerX*2));
                canvas.rotate(180.0F);
                for (int i = 0; i < 20; i++) {
                    paint.setColor((i < batteryLevel / 5) ? highColor : normalColor);
                    canvas.drawLine(getResolution(7), centerY, getResolution(7), centerY + getResolution(25), paint);
                    canvas.rotate(18.0F, 0.0F, 0.0F);
                }
            }
            canvas.restore();
        }

    }
    private void drawChargingInfo(Canvas canvas, Drawable paramDrawable, int centerX, int centerY, int paramInt3) {
        int width, height;
        if(paramDrawable != null) {
            width = getResolution(paramDrawable.getIntrinsicWidth());
            height = getResolution(paramDrawable.getIntrinsicHeight());
            paramDrawable.setBounds(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2);
            paramDrawable.draw(canvas);
            Paint localPaint = new Paint();
            localPaint.setTextSize(getResolution(20));
            localPaint.setAntiAlias(true);
            localPaint.setColor(paramInt3);
            canvas.drawText(parserUtils.processBattery(context, "#BLVL#"), paramDrawable.getBounds().right + getResolution(5), paramDrawable.getBounds().bottom - getResolution(6), localPaint);
        }
    }
    private void drawClockQuietPicture(Canvas canvas, Drawable paramDrawable, int centerX, int centerY) {
        int width = getResolution(paramDrawable.getIntrinsicWidth());
        int height = getResolution(paramDrawable.getIntrinsicHeight());
        if (width <= 3) {
            paramDrawable.setBounds(centerX - width / 2 + 1, centerY - height / 2, centerX + width / 2 + 1, centerY + height / 2);
        } else {
            paramDrawable.setBounds((centerX - width / 2) - 1, (centerY - height / 2) - 1, (centerX + width / 2) + 1, (centerY + height / 2) + 1);
        }
        paramDrawable.draw(canvas);
    }
    private void drawDigitalHourAndMinute(Canvas canvas, Drawable drawableHour1, Drawable drawableHour2, Drawable drawableColon, Drawable drawableMinute1, Drawable drawableMinute2, Drawable drawableAMPM, int centerX, int centerY, int second) {
        int widthHour = getResolution(drawableHour1.getIntrinsicWidth());
        int heightHour = getResolution(drawableHour1.getIntrinsicHeight());
        int widthColon = getResolution(drawableColon.getIntrinsicWidth());
        int heightColon = getResolution(drawableColon.getIntrinsicHeight());
        int widthAMPM, heightAMPM;
        if (drawableAMPM != null) {
            widthAMPM = getResolution(drawableAMPM.getIntrinsicWidth());
            heightAMPM = getResolution(drawableAMPM.getIntrinsicHeight());
        } else {
            widthAMPM = 0;
            heightAMPM = 0;
        }
        centerX -= (widthHour * 2 + widthColon / 2 + widthAMPM / 2);
        drawableHour1.setBounds(centerX, centerY - heightHour / 2, centerX + widthHour, centerY + heightHour / 2);
        drawableHour1.draw(canvas);
        centerX += widthHour;
        drawableHour2.setBounds(centerX, centerY - heightHour / 2, centerX + widthHour, centerY + heightHour / 2);
        drawableHour2.draw(canvas);
        centerX += widthHour;
        if (second % 2 == 0) {
            drawableColon.setBounds(centerX, centerY - heightColon / 2, centerX + widthColon, centerY + heightColon / 2);
            drawableColon.draw(canvas);
        }
        centerX += widthColon;
        drawableMinute1.setBounds(centerX, centerY - heightHour / 2, centerX + widthHour, centerY + heightHour / 2);
        drawableMinute1.draw(canvas);
        centerX += widthHour;
        drawableMinute2.setBounds(centerX, centerY - heightHour / 2, centerX + widthHour, centerY + heightHour / 2);
        drawableMinute2.draw(canvas);
        centerX += widthHour;
        if (drawableAMPM != null) {
            drawableAMPM.setBounds(centerX, centerY - heightHour / 2, centerX + widthAMPM, centerY - heightHour / 2 + heightAMPM);
            drawableAMPM.draw(canvas);
        }
    }
    private void drawDigitalMonthAndDay(Canvas canvas, Drawable month1, Drawable month2, Drawable colon, Drawable day1, Drawable day2, int centerX, int centerY) {
        int widthM1 = getResolution(month1.getIntrinsicWidth());
        int heightM1 = getResolution(month1.getIntrinsicHeight());
        int widthM2 = getResolution(month2.getIntrinsicWidth());
        int heightM2 = getResolution(month2.getIntrinsicHeight());
        int widthColon = getResolution(colon.getIntrinsicWidth());
        int heightColon = getResolution(colon.getIntrinsicHeight());
        int widthD1 = getResolution(day1.getIntrinsicWidth());
        int heightD1 = getResolution(day1.getIntrinsicHeight());
        int widthD2 = getResolution(day2.getIntrinsicWidth());
        int heightD2 = getResolution(day2.getIntrinsicHeight());
        centerX -= (widthM1 + widthM2 + widthColon + widthD1 + widthD2) / 2;
        month1.setBounds(centerX, centerY - (heightM1 / 2), centerX + widthM1, centerY + heightM1 / 2);
        month1.draw(canvas);
        centerX += widthM1;
        month2.setBounds(centerX, centerY - (heightM2 / 2), centerX + widthM2, centerY + (heightM2 / 2));
        month2.draw(canvas);
        centerX += widthM2;
        colon.setBounds(centerX, centerY - (heightColon / 2), centerX + widthColon, centerY + (heightColon / 2));
        colon.draw(canvas);
        centerX += widthColon;
        day1.setBounds(centerX, centerY - (heightD1 / 2), centerX + widthD1, centerY + (heightD1 / 2));
        day1.draw(canvas);
        centerX += widthD1;
        day2.setBounds(centerX, centerY - (heightD2 / 2), centerX + widthD2, centerY + (heightD2 / 2));
        day2.draw(canvas);
    }
    private void drawDigitalOnePicture(Canvas canvas, Drawable drawable, int centerX, int centerY) {
        int width = getResolution(drawable.getIntrinsicWidth());
        int height = getResolution(drawable.getIntrinsicHeight());
        centerX -= width / 2;
        centerY -= height / 2;
        drawable.setBounds(centerX, centerY, centerX + width, centerY + height);
        drawable.draw(canvas);
    }
    private void drawDigitalTwoPicture(Canvas canvas, Drawable drawable1, Drawable drawable2, int centerX, int centerY) {
        int width = getResolution(drawable1.getIntrinsicWidth());
        int height = getResolution(drawable1.getIntrinsicHeight());
        int width2 = getResolution(drawable2.getIntrinsicWidth());
        int height2 = getResolution(drawable2.getIntrinsicHeight());
        centerX -= (width + width2) / 2;
        centerY -= (height) / 2;
        drawable1.setBounds(centerX, centerY, centerX + width, centerY + height);
        drawable1.draw(canvas);
        centerX = centerX + width;
        centerY -= (height - height2) / 2;
        drawable2.setBounds(centerX, centerY, centerX + width2, centerY + height2);
        drawable2.draw(canvas);
    }
    private void drawDigitalYear(Canvas canvas, List<Drawable> drawables, int currentYear, int centerX, int centerY) {
        int l = (drawables.get(0)).getIntrinsicWidth();
        int k = (drawables.get(0)).getIntrinsicHeight();
        int i = getResolution(l);
        int j = getResolution(k);
        k = centerY - j / 2;
        centerY += j / 2;
        j = currentYear / 1000;
        l = currentYear / 100 % 10;
        int i1 = currentYear / 10 % 10;
        currentYear %= 10;
        centerX -= i * 2;
        int i2 = centerX + i;
        int i3 = i2 + i;
        int i4 = i3 + i;
        drawables.get(j).setBounds(centerX, k, i2, centerY);
        drawables.get(j).draw(canvas);
        drawables.get(l).setBounds(i2, k, i3, centerY);
        drawables.get(l).draw(canvas);
        drawables.get(i1).setBounds(i3, k, i4, centerY);
        drawables.get(i1).draw(canvas);
        drawables.get(currentYear).setBounds(i4, k, i4 + i, centerY);
        drawables.get(currentYear).draw(canvas);
    }
    private void drawDigitalYearMonthDay(Canvas canvas, List<Drawable> paramList, int centerX, int centerY) {
        int width = getResolution((paramList.get(0)).getIntrinsicWidth());
        int height = getResolution((paramList.get(0)).getIntrinsicHeight());
        int colonHeight = getResolution((paramList.get(10)).getIntrinsicHeight());
        centerX = centerX - (width * 8 + colonHeight * 2) / 2;
        centerY -= height / 2;
        (paramList.get(year / 1000)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(year / 1000)).draw(canvas);
        centerX += width;
        (paramList.get(year / 100 % 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(year / 100 % 10)).draw(canvas);
        centerX += width;
        (paramList.get(year / 10 % 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(year / 10 % 10)).draw(canvas);
        centerX += width;
        (paramList.get(year % 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(year % 10)).draw(canvas);
        centerX += width;
        (paramList.get(10)).setBounds(centerX, centerY, centerX + width, centerY + colonHeight);
        (paramList.get(10)).draw(canvas);
        centerX += width;
        (paramList.get(month / 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(month / 10)).draw(canvas);
        centerX += width;
        (paramList.get(month % 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(month % 10)).draw(canvas);
        centerX += width;
        (paramList.get(10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(10)).draw(canvas);
        centerX += width;
        (paramList.get(day / 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(day / 10)).draw(canvas);
        centerX += width;
        (paramList.get(day % 10)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(day % 10)).draw(canvas);
    }
    private void drawHeartRatePicture(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int hearRate) {
        int b = 0, t, n;
        hearRate = Math.min(hearRate, 999);
        hearRate = Math.max(hearRate, 0);
        if (hearRate > 99) {
            b = hearRate / 100;
            hearRate = hearRate % 100;
        }
        t = hearRate / 10 % 10;
        n = hearRate % 10;
        int width = getResolution((paramList.get(0)).getIntrinsicWidth());
        int height = getResolution((paramList.get(0)).getIntrinsicHeight());
        centerY -= height / 2;
        if (b > 0) {
            centerX -= width * 3 / 2;
            (paramList.get(b)).setBounds(centerX, centerY, centerX + width, centerY + height);
            (paramList.get(b)).draw(canvas);
            centerX += width;
        } else {
            centerX -= width;
        }
        (paramList.get(t)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(t)).draw(canvas);
        centerX += width;
        (paramList.get(n)).setBounds(centerX, centerY, centerX + width, centerY + height);
        (paramList.get(n)).draw(canvas);


    }
    private void drawSpecialSecond(Canvas canvas, String colorsInfo, int minute, int second) {
        int centerX = getWidth() / 2;
        int highColor;
        int normalColor;
        Paint paint;
        if (colorsInfo.contains(",")) {
            highColor = 0xFF000000 | Integer.valueOf(colorsInfo.split(",")[0], 16);
            normalColor = 0xFF000000 | Integer.valueOf(colorsInfo.split(",")[1], 16);
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(getResolution(10));
            paint.setStyle(Paint.Style.STROKE);
            paint.setAlpha(255);
            canvas.save();
            canvas.translate(centerX, centerX);
            float f = -centerX + 5;
            for (int i = 0; i < 60; i++) {
                if (minute % 2 == 0) {
                    paint.setColor((i < second) ? highColor : normalColor);
                } else {
                    paint.setColor((i < second) ? normalColor : highColor);
                }

                canvas.drawLine(getResolution(5), f, getResolution(5), f + getResolution(15), paint);
                canvas.rotate(6.0F, 0.0F, 0.0F);
            }

            canvas.restore();
        }
    }
    private void drawStepsPicture(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int step) {
        step = Math.max(step, 0);
        step = Math.min(step, 99999);
        int w = step / 10000;
        int k = step / 1000 % 10;
        int h = step / 100 % 10;
        int t = step / 10 % 10;
        int g = step % 10;
        int widthW = getResolution((paramList.get(w)).getIntrinsicWidth());
        int widthK = getResolution((paramList.get(k)).getIntrinsicWidth());
        int widthH = getResolution((paramList.get(h)).getIntrinsicWidth());
        int widthT = getResolution((paramList.get(t)).getIntrinsicWidth());
        int widthG = getResolution((paramList.get(g)).getIntrinsicWidth());
        int height = getResolution((paramList.get(w)).getIntrinsicHeight());
        centerX -= (widthW + widthK + widthH + widthT + widthG) / 2;
        centerY -= height / 2;
        (paramList.get(w)).setBounds(centerX, centerY, centerX + widthW, centerY + height);
        (paramList.get(w)).draw(canvas);
        centerX += widthW;
        (paramList.get(k)).setBounds(centerX, centerY, centerX + widthK, centerY + height);
        (paramList.get(k)).draw(canvas);
        centerX += widthK;
        (paramList.get(h)).setBounds(centerX, centerY, centerX + widthH, centerY + height);
        (paramList.get(h)).draw(canvas);
        centerX += widthH;
        (paramList.get(t)).setBounds(centerX, centerY, centerX + widthT, centerY + height);
        (paramList.get(t)).draw(canvas);
        centerX += widthT;
        (paramList.get(g)).setBounds(centerX, centerY, centerX + widthG, centerY + height);
        (paramList.get(g)).draw(canvas);

    }
    private void drawStepsPictureNew(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int step) {
        step = Math.max(step, 0);
        step = Math.min(step, 99999);
        int w = step / 10000;
        int k = step / 1000 % 10;
        int h = step / 100 % 10;
        int t = step / 10 % 10;
        int g = step % 10;

        int widthW = getResolution((paramList.get(w)).getIntrinsicWidth());
        int widthK = getResolution((paramList.get(k)).getIntrinsicWidth());
        int widthH = getResolution((paramList.get(h)).getIntrinsicWidth());
        int widthT = getResolution((paramList.get(t)).getIntrinsicWidth());
        int widthG = getResolution((paramList.get(g)).getIntrinsicWidth());
        int height = getResolution((paramList.get(w)).getIntrinsicHeight());
        int ljdmm = 5;
        if (w == 0) {
            widthW = 0;
            ljdmm = 4;
            if (k == 0) {
                widthK = 0;
                ljdmm = 3;
                if (h == 0) {
                    widthH = 0;
                    ljdmm = 2;
                    if (t == 0) {
                        widthT = 0;
                        ljdmm = 1;
                    }
                }

            }

        }
        centerX -= (widthW + widthK + widthH + widthT + widthG) / 2;
        centerY -= height / 2;
        switch (ljdmm){
            case 1:
                (paramList.get(t)).setBounds(centerX, centerY, centerX + widthT, centerY + height);
                (paramList.get(t)).draw(canvas);
                break;
            case 2:
                (paramList.get(h)).setBounds(centerX, centerY, centerX + widthH, centerY + height);
                (paramList.get(h)).draw(canvas);
                break;
            case 3:
                (paramList.get(k)).setBounds(centerX, centerY, centerX + widthK, centerY + height);
                (paramList.get(k)).draw(canvas);
                break;
            case 4:
                (paramList.get(w)).setBounds(centerX, centerY, centerX + widthW, centerY + height);
                (paramList.get(w)).draw(canvas);
                break;

        }
        (paramList.get(g)).setBounds(centerX, centerY, centerX + widthG, centerY + height);
        (paramList.get(g)).draw(canvas);

    }
    private void drawKalPictureNew(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, double kal) {
        kal = Math.max(kal, 0.0);
        kal = Math.min(kal, 99999.9);
        int w = (int) (kal / 10000);
        int k = (int) (kal / 1000 % 10);
        int h = (int) (kal / 100 % 10);
        int t = (int) (kal / 10 % 10);
        int g = (int) (kal % 10);
        int d = (int) (kal * 10 % 10);
        int widthW = getResolution((paramList.get(w)).getIntrinsicWidth());
        int widthK = getResolution((paramList.get(k)).getIntrinsicWidth());
        int widthH = getResolution((paramList.get(h)).getIntrinsicWidth());
        int widthT = getResolution((paramList.get(t)).getIntrinsicWidth());
        int widthG = getResolution((paramList.get(g)).getIntrinsicWidth());
        int widthDot = getResolution((paramList.get(paramList.size() - 1)).getIntrinsicWidth());
        int widthD = getResolution((paramList.get(d)).getIntrinsicWidth());
        int height = getResolution((paramList.get(w)).getIntrinsicHeight());
        int index = 5;
        if (w == 0) {
            widthW = 0;
            index = 4;
            if (k == 0) {
                widthK = 0;
                index = 3;
                if (h == 0) {
                    widthH = 0;
                    index = 2;
                    if (t == 0) {
                        widthT = 0;
                        index = 1;
                    }
                }

            }

        }
        centerX -= (widthW + widthK + widthH + widthT + widthG + widthDot + widthD) / 2;
        centerY -= height / 2;
        switch (index){
            case 1:
                (paramList.get(t)).setBounds(centerX, centerY, centerX + widthT, centerY + height);
                (paramList.get(t)).draw(canvas);
                break;
            case 2:
                (paramList.get(h)).setBounds(centerX, centerY, centerX + widthH, centerY + height);
                (paramList.get(h)).draw(canvas);
                break;
            case 3:
                (paramList.get(k)).setBounds(centerX, centerY, centerX + widthK, centerY + height);
                (paramList.get(k)).draw(canvas);
                break;
            case 4:
                (paramList.get(w)).setBounds(centerX, centerY, centerX + widthW, centerY + height);
                (paramList.get(w)).draw(canvas);
                break;
        }
        (paramList.get(g)).setBounds(centerX, centerY, centerX + widthG, centerY + height);
        (paramList.get(g)).draw(canvas);
        centerX += widthG;
        (paramList.get(paramList.size() - 1)).setBounds(centerX, centerY, centerX + widthDot, centerY + height);
        (paramList.get(paramList.size() - 1)).draw(canvas);
        centerX += widthDot;
        (paramList.get(d)).setBounds(centerX, centerY, centerX + widthD, centerY + height);
        (paramList.get(d)).draw(canvas);
    }
    private void drawStepsPictureWithCircle(Canvas canvas, List<Drawable> paramList, int centerX, int centerY, int step, String colorsInfo) {
        step = Math.max(step, 0);
        step = Math.min(step, 99999);
        int center = centerX;
        int w = step / 10000;
        int k = step / 1000 % 10;
        int h = step / 100 % 10;
        int t = step / 10 % 10;
        int g = step % 10;
        int widthW = getResolution((paramList.get(w)).getIntrinsicWidth());
        int widthK = getResolution((paramList.get(k)).getIntrinsicWidth());
        int widthH = getResolution((paramList.get(h)).getIntrinsicWidth());
        int widthT = getResolution((paramList.get(t)).getIntrinsicWidth());
        int widthG = getResolution((paramList.get(g)).getIntrinsicWidth());
        int height = getResolution((paramList.get(w)).getIntrinsicHeight());
        centerX -= (widthW + widthK + widthH + widthT + widthG) / 2;
        centerY -= height / 2;
        (paramList.get(w)).setBounds(centerX, centerY, centerX + widthW, centerY + height);
        (paramList.get(w)).draw(canvas);
        centerX += widthW;
        (paramList.get(k)).setBounds(centerX, centerY, centerX + widthK, centerY + height);
        (paramList.get(k)).draw(canvas);
        centerX += widthK;
        (paramList.get(h)).setBounds(centerX, centerY, centerX + widthH, centerY + height);
        (paramList.get(h)).draw(canvas);
        centerX += widthH;
        (paramList.get(t)).setBounds(centerX, centerY, centerX + widthT, centerY + height);
        (paramList.get(t)).draw(canvas);
        centerX += widthT;
        (paramList.get(g)).setBounds(centerX, centerY, centerX + widthG, centerY + height);
        (paramList.get(g)).draw(canvas);
        centerY += height / 2;
        int highColor;
        int normalColor;
        Paint paint;
        normalColor = Integer.valueOf(colorsInfo.split(",")[0], 16);
        highColor = Integer.valueOf(colorsInfo.split(",")[1], 16);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(getResolution(10));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(255);
        canvas.save();
        RectF rectF = new RectF();
        rectF.set(center - (float)getWidth() / 8, centerY - (float)getHeight() / 8, (float)getWidth() / 8 + center, (float)getHeight() / 8 + centerY);
        //int faceSettingsFile = Settings.System.getInt(context.getContentResolver(), "suggest_steps", 0);
        paint.setColor(0xFF000000 | normalColor);
        canvas.drawCircle(center, centerY, (float)getWidth() / 8, paint);
        paint.setColor(0xFF000000 | highColor);
        if (step > stepTarget) {
            canvas.drawCircle(center, centerY, (float)getWidth() / 8, paint);
        }
        canvas.drawArc(rectF, 270.0F, step / (float) stepTarget * 360.0F, false, paint);

        canvas.restore();
    }
    private void drawStepsCircle(Canvas canvas, int centerX, int centerY, int step) {
        step = Math.max(step, 0);
        step = Math.min(step, 99999);
        int normalColor = 0xFF3CD62B;
        Paint paint;
        TextPaint textPaint;
        textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("#ffffff"));
        textPaint.setTextSize(20);
        int r = getWidth() * 2 / 13;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(getResolution(7));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(255);
        canvas.save();
        RectF rectF = new RectF();
        rectF.set(centerX - r, centerY - r,r + centerX, r + centerY);
        paint.setColor(normalColor);
        if (step > stepTarget) {
            canvas.drawCircle(centerX, centerY, r, paint);
        }
        canvas.drawArc(rectF, 270.0F, step / (float) stepTarget * 360.0F, false, paint);
        canvas.drawText(String.valueOf(step), centerX - textPaint.measureText(String.valueOf(step)) / 2, centerY + 30, textPaint);
        canvas.restore();
    }
    private void drawBatteryCircle(Canvas canvas, int centerX, int centerY, int batteryLevel, List<Drawable> paramList) {
        int normalColor = 0xFFEF3062;
        Paint paint;
        TextPaint textPaint;
        textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("#ffffff"));
        textPaint.setTextSize(20);
        int r = getWidth() * 2 / 13;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(getResolution(7));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(255);
        canvas.save();
        RectF rectF = new RectF();
        rectF.set(centerX - (r*renderScale), centerY - (r*renderScale),(r*renderScale) + centerX, (r*renderScale) + centerY);
        //paint.setColor(normalColor);
        if (batteryLevel > 99) {
            canvas.drawCircle(centerX, centerY, (r*renderScale), paint);
        }
        canvas.drawArc(rectF, 270.0F, batteryLevel * 3.6f, false, paint);
        canvas.drawText(batteryLevel + "%", centerX - textPaint.measureText(batteryLevel + "%") / 2, centerY + 30, textPaint);
        canvas.restore();
        Drawable batteryPic;
        int index = 0;
        switch (batteryLevel){
            case 100:
                index = 10;
                break;
            case 90:
                index = 9;
                break;
            case 80:
                index = 8;
                break;
            case 70:
                index = 7;
                break;
            case 60:
                index = 6;
                break;
            case 50:
                index = 5;
                break;
            case 40:
                index = 4;
                break;
            case 30:
                index = 3;
                break;
            case 20:
                index = 2;
                break;
            case 10:
                index = 1;
                break;
            case 0:
                index = 0;
                break;
        }
        batteryPic = paramList.get(index);
        int width = getResolution(batteryPic.getIntrinsicWidth());
        int height = getResolution(batteryPic.getIntrinsicHeight());
        centerX -= (width) / 2;
        centerY -= height * 3 / 2;
        batteryPic.setBounds(centerX, centerY, centerX + width, centerY + height);
        batteryPic.draw(canvas);
    }
    private void drawSecondCircle(Canvas canvas, int centerX, int centerY, int second) {
        int juli = 25;
        Paint paint;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(32);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(255);
        canvas.save();
        RectF rectF = new RectF();
        rectF.set(centerX - ((float) getWidth() / 2 - juli), centerY - ((float) getHeight() / 2 - juli),
                centerX + ((float) getWidth() / 2 - juli), ((float) getHeight() / 2 - juli) + centerY);
        paint.setColor(0x7AFFFF00);
        for (int s = 0; s < second + 1; s++) {
            canvas.drawArc(rectF, 270 - 0.75f + s * 6, 1.5f, false, paint);
            canvas.drawArc(rectF, 270 - 0.75f + s * 6 + 3, 1.5f, false, paint);
        }
        canvas.restore();
    }
    private void drawTemperature(Canvas canvas, Drawable minus, Drawable temp1, Drawable temp2, Drawable tempUnit, int centerX, int centerY, boolean paramBoolean1) {
        int widthMinus = getResolution(minus.getIntrinsicWidth());
        int heightMinus = getResolution(minus.getIntrinsicHeight());
        int widthTemp1 = getResolution(temp1.getIntrinsicWidth());
        int heightTemp1 = getResolution(temp1.getIntrinsicHeight());
        int widthTemp2 = getResolution(temp2.getIntrinsicWidth());
        int heightTemp2 = getResolution(temp2.getIntrinsicHeight());
        int widthTempUnit = getResolution(tempUnit.getIntrinsicWidth());
        int heightTempUnit = getResolution(tempUnit.getIntrinsicHeight());
        centerX -= (widthMinus + widthTemp1 + widthTemp2 + widthTempUnit) / 2;
        if (paramBoolean1) {
            minus.setBounds(centerX, centerY - heightMinus / 2, centerX + widthMinus, centerY + heightMinus / 2);
            minus.draw(canvas);
            centerX += widthMinus;
        }
        temp1.setBounds(centerX, centerY - heightTemp1 / 2, centerX + widthTemp1, centerY + heightTemp1 / 2);
        temp1.draw(canvas);
        centerX += widthTemp1;
        temp2.setBounds(centerX, centerY - heightTemp2 / 2, centerX + widthTemp2, centerY + heightTemp2 / 2);
        temp2.draw(canvas);
        centerX += widthTemp2;
        tempUnit.setBounds(centerX, centerY - heightTemp2 / 2, centerX + widthTempUnit, centerY - heightTemp2 / 2 + heightTempUnit);
        tempUnit.draw(canvas);
    }
    private void drawValueWithProgress(Canvas canvas, ArrayList<Drawable> drawables, int valueType, String colorsInfo, float diliverArc, int dilivercount, int centerX, int centerY, int radius, int strokewidth, int textSize) {
        centerX = getResolution(centerX);
        centerY = getResolution(centerY);
        radius = getResolution(radius);
        strokewidth = getResolution(strokewidth);
        textSize = getResolution(textSize);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokewidth);
        paint.setStyle(Paint.Style.STROKE);
        final float arc;
        if (diliverArc > 0) {
            arc = (float)360 / dilivercount - diliverArc;
        } else {
            arc = 360;
        }
        final float hightArc = getHeightArc(valueType);
        final int colornormal = Integer.valueOf(colorsInfo.split(",")[0], 16) + 0xFF000000;
        final int colorhighlight = Integer.valueOf(colorsInfo.split(",")[1], 16) + 0xFF000000;
        RectF rectf = new RectF();
        rectf.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        float startArc = 0.0f;
        float arcTmp;
        canvas.save();
        while (startArc < 360.0f) {
            arcTmp = hightArc - startArc;
            if (arcTmp >= arc) {
                paint.setColor(colorhighlight);
                drawArcStartAtUp(canvas, rectf, startArc, arc, paint);
            } else if (arcTmp > 0) {
                paint.setColor(colorhighlight);
                drawArcStartAtUp(canvas, rectf, startArc, arcTmp, paint);
                paint.setColor(colornormal);
                drawArcStartAtUp(canvas, rectf, startArc + arcTmp, (arc - arcTmp), paint);
            } else {
                paint.setColor(colornormal);
                drawArcStartAtUp(canvas, rectf, startArc, arc, paint);
            }
            startArc += (arc + diliverArc);
        }
        if (drawables != null && drawables.size() > 0) {
            final int index = (int) ((drawables.size() - 1) * hightArc / 360);
            final Drawable drawable = drawables.get(index);
            int width = getResolution(drawable.getIntrinsicWidth());
            int height = getResolution(drawable.getIntrinsicHeight());
            final int left = centerX - width / 2;
            final int right = centerX + width / 2;
            final int top = centerY - height / 2;
            final int bottom = centerY + height / 2;
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(canvas);
        } else {
            final String valueString = getValueString(valueType);
            if (valueString != null) {
                final Rect rect = new Rect();
                Paint textPaint = new Paint();
                textPaint.setTextSize(textSize);
                textPaint.setColor(colorhighlight);
                textPaint.setFakeBoldText(true);
                textPaint.setAntiAlias(true);
                textPaint.setDither(true);
                textPaint.getTextBounds(valueString, 0, valueString.length(), rect);

                canvas.drawText(valueString, centerX - (float) rect.width() / 2, centerY + (float) rect.height() / 2, textPaint);
            }
        }
        canvas.restore();
    }
    private void drawValueString(Canvas canvas, int valueType, String colorsInfo,int centerX, int centerY, int textSize) {
        centerX = getResolution(centerX);
        centerY = getResolution(centerY);
        textSize = getResolution(textSize);
        final int textColor = Integer.valueOf(colorsInfo.split(",")[0], 16) + 0xFF000000;
        final String valueString = getValueString(valueType);
        final Rect rect = new Rect();
        Paint textPaint = new Paint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(true);
        textPaint.setDither(true);
        textPaint.getTextBounds(valueString, 0, valueString.length(), rect);
        canvas.drawText(valueString, centerX - (float) rect.width() / 2, centerY + (float) rect.height() / 2, textPaint);
    }
    private void drawValueWithClipPicture(Canvas canvas, int valueType, ArrayList<Drawable> drawables, int centerX, int centerY) {
        int widthMinus = getResolution(drawables.get(0).getIntrinsicWidth());
        int heightMinus = getResolution(drawables.get(0).getIntrinsicHeight());
        centerX = getResolution(centerX);
        centerY = getResolution(centerY);
        final int left = centerX - widthMinus / 2;
        final int right = centerX + widthMinus / 2;
        final int top = centerY - heightMinus / 2;
        final int bottom = centerY + heightMinus / 2;
        if (drawables.size() < 1) {
            return;
        }
        final Drawable bg = drawables.get(0);
        bg.setBounds(left, top, right, bottom);
        bg.draw(canvas);

        if (drawables.size() < 2) {
            return;
        }
        final Drawable clipBg = drawables.get(1);
        final float radius = (float) Math.max(widthMinus, heightMinus) / 2;
        final float startArc = 0;
        final float sweelArc = getHeightArc(valueType);
        final float startX = centerX;
        final float startY = centerY - radius;
        final float endX = (float) (centerX + radius * Math.sin(sweelArc * Math.PI / 180));
        final float endY = (float) (centerY - radius * Math.cos(sweelArc * Math.PI / 180));
        Path path = new Path();
        path.moveTo(centerX, centerY);
        path.lineTo(startX, startY);
        path.lineTo(endX, endY);
        path.close();
        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        path.addArc(rectF, startArc + 270, sweelArc);
        canvas.save();
        canvas.clipPath(path);
        clipBg.setBounds(left, top, right, bottom);
        clipBg.draw(canvas);
        canvas.restore();
        if (drawables.size() < 3) {
            return;
        }
        final Drawable snap = drawables.get(2);

        canvas.save();
        canvas.rotate(sweelArc, centerX, centerY);
        snap.setBounds(left, top, right, bottom);
        snap.draw(canvas);
        canvas.restore();
    }
    private void drawClockRotatePictureNew(Canvas canvas, Drawable drawable, int centerX, int centerY, float rotateAngle) {
        int width = getResolution(drawable.getIntrinsicWidth());
        int height = getResolution(drawable.getIntrinsicHeight());
        canvas.save();
        canvas.translate(0, ClockEngineConstants.PICTUTE_SHADOW_CENTERY);
        centerY += ClockEngineConstants.PICTUTE_SHADOW_CENTERY;
        canvas.rotate(rotateAngle, centerX, centerY);
        drawable.setBounds(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2);
        drawable.draw(canvas);
        canvas.restore();

    }
    private void drawArcStartAtUp(Canvas canvas, RectF oval, float startAngle, float sweepAngle, Paint paint) {
        canvas.drawArc(oval, startAngle + 270, sweepAngle, false, paint);
    }
    private void drawBackgroundFrame(Canvas canvas, float renderScale, float renderLeft, float renderTop){
        long now = SystemClock.uptimeMillis();
        if(renderStart == 0L){
            renderStart = now;
        }
        long duration = backgroundRender.duration();
        if(duration == 0){
            duration = 1000;
        }
        renderCurrentTime = (int)((now - renderStart) % duration);
        backgroundRender.setTime(renderCurrentTime);
        canvas.save();
        canvas.scale(renderScale, renderScale);
        backgroundRender.draw(canvas, renderLeft, renderTop);
        canvas.restore();
    }
    
    
    //Util
    private Path calculatePolygonPoints(float scale, int sides, int radius, int mX, int mY, float mMultiplyFactor) {
        Path mPath = new Path();
        double segment = 6.283185307179586d / ((double) sides);
        double x1 = 0.0d;
        double y1 = 0.0d;
        for (int i = 1; i <= sides; i++) {
            double x = (Math.sin(((double) i) * segment) * ((double) (((float) radius) / mMultiplyFactor))) + ((double) (((float) mX) / mMultiplyFactor));
            double y = (Math.cos(((double) i) * segment) * ((double) (((float) radius) / mMultiplyFactor))) + ((double) (((float) mY) / mMultiplyFactor));
            if (i == 1) {
                mPath.moveTo(((float) x) * scale, ((float) y) * scale);
                x1 = x;
                y1 = y;
            } else {
                mPath.lineTo(((float) x) * scale, ((float) y) * scale);
            }
        }
        mPath.lineTo(((float) x1) * scale, ((float) y1) * scale);
        mPath.close();
        return mPath;
    }
    private void calculateAlignmentOffset(float width, float height, float x, float y, int align, BitmapDrawable drawable){
        switch (align) {
            case 1:
                drawable.setBounds((int)(x - (width / 2)), (int)y, (int)(x + (width / 2)), (int)(height + y));
                break;
            case 2:
                drawable.setBounds((int)(x - width), (int)y, (int)x, (int)(height + y));
                break;
            case 3:
                drawable.setBounds((int)x, (int)(y - (height / 2)), (int)(width + x), (int)(y + (height / 2)));
                break;
            case 4:
                drawable.setBounds((int)(x - (width / 2)), (int)(y - (height / 2)), (int)(x + (width / 2)), (int)(y + (height / 2)));
                break;
            case 5:
                drawable.setBounds((int)(x - width), (int)(y - (height / 2)), (int)x, (int)(y + (height / 2)));
                break;
            case 6:
                drawable.setBounds((int)x, (int)(y - height), (int)(x + width), (int)y);
                break;
            case 7:
                drawable.setBounds((int)(x - (width / 2)), (int)(y - height), (int)(x + (width / 2)), (int)y);
                break;
            case 8:
                drawable.setBounds((int)(x - width), (int)(y - height), (int)x, (int)y);
                break;
            default:
                drawable.setBounds((int)x, (int)y, (int)(width + x), (int)(height + y));
                break;
        }
    }
    private void insertImageHash(HashMap<String, String> layer, File watchfaceFile, String key){
        File imageFile;
        byte[] converted_data;
        BitmapDrawable bitmapDrawable;
        if(layer.containsKey(key)) {
            imageFile = FileUtils.getFileFromZip(watchfaceFile, "images/" + layer.get(key));
            if (imageFile != null) {
                if (isProtected) {
                    try {
                        converted_data = Base64.decode(FileUtils.read(imageFile), 0);
                        bitmapDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(converted_data, 0, converted_data.length));
                        drawableHashMap.put(layer.get(key), bitmapDrawable);
                    } catch (IllegalArgumentException e7) {
                        bitmapDrawable = new BitmapDrawable(getResources(), decodeSampledBitmap(imageFile.getAbsolutePath(), getWidth()));
                        drawableHashMap.put(layer.get(key), bitmapDrawable);
                    }
                } else {
                    bitmapDrawable = new BitmapDrawable(getResources(), decodeSampledBitmap(imageFile.getAbsolutePath(), getWidth()));
                    drawableHashMap.put(layer.get(key), bitmapDrawable);
                }
            }
        }
    }
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth) {
        int width = options.outWidth;
        int inSampleSize = 1;
        if (width > reqWidth) {
            while ((width / 2) / inSampleSize > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    private Bitmap decodeSampledBitmap(String file, int reqWidth) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);
        if (options.outHeight > reqWidth * 2 && options.outWidth > reqWidth * 2) {
            options.inSampleSize = calculateInSampleSize(options, reqWidth);
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }
    private boolean isProtected(JSONObject object){
        if(object != null) {
            try {
                if (object.has("is_protected")) {
                    return object.getBoolean("is_protected");
                }
                return false;
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
    private Drawable scaleDrawable(Drawable drawable, float scale) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap oldBmp = drawableToBitmap(drawable);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap newBmp = Bitmap.createBitmap(oldBmp, 0, 0, width, height, matrix, true);
        return new BitmapDrawable(newBmp);
    }
    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    private int getResolution(int paramInt) {
        return (int) (paramInt * renderScale);
    }
    private float getHeightArc(int valueType) {
        switch (valueType) {
            case ClockEngineConstants.VALUE_TYPE_KCAL:
                return (360 * calories / caloriesTarget);
            case ClockEngineConstants.VALUE_TYPE_STEP:
                return (float) 360 * stepCount / stepTarget;
            case ClockEngineConstants.VALUE_TYPE_BATTERY:
                return (float) 360 * Integer.parseInt(parserUtils.processBattery(context, "BLVL"));
            default:
                return 0;
        }
    }
    private String getValueString(int valueType) {
        switch (valueType) {
            case ClockEngineConstants.VALUE_TYPE_KCAL:
                return new DecimalFormat("0.0").format(calories);
            case ClockEngineConstants.VALUE_TYPE_STEP:
                return String.valueOf(stepCount);
            case ClockEngineConstants.VALUE_TYPE_BATTERY:
                return parserUtils.processBattery(context, "BLVL");
            default:
                return String.valueOf(0);
        }
    }

    private int stepCount = 0;
    private int stepTarget = 0;
    private int calories = 0;
    private int caloriesTarget = 0;
    private int heartRate = 0;
    private int weatherTemp = 0;
    private int weatherIcon = 0;
    private int moonPhase = 0;
    private int year = 1990;
    private int month = 2;
    private int day = 11;
    private int dayOfWeek = 1;
    private int minute = 0;
    private int second = 0;
    private int hour = 0;

    private void tick(){
        year = Integer.parseInt(parserUtils.processTime(context, "#Dy#"));
        month = Integer.parseInt(parserUtils.processTime(context, "#DMM#"));
        day = Integer.parseInt(parserUtils.processTime(context, "#Dd#"));
        dayOfWeek = Integer.parseInt(parserUtils.processTime(context, "#DOWB#"));
        minute = Integer.parseInt(parserUtils.processTime(context, "#Dm#"));
        second = Integer.parseInt(parserUtils.processTime(context, "#Ds#"));
        hour = Integer.parseInt(parserUtils.processTime(context, "#Db#"));
        stepCount = Integer.parseInt(parserUtils.processHealth("#ZSC#"));
        stepTarget = Integer.parseInt(parserUtils.processHealth("#ZSTEPT#"));
        calories = Integer.parseInt(parserUtils.processHealth("#ZCAL#"));
        caloriesTarget = Integer.parseInt(parserUtils.processHealth("#ZCALT#"));
        heartRate = Integer.parseInt(parserUtils.processHealth("#ZHR#"));
        weatherIcon = Integer.parseInt(parserUtils.processWeather("#WCCI#"));
        weatherTemp = Integer.parseInt(parserUtils.processWeather("#WCT#"));
        moonPhase = 0;
        milliSecond = Calendar.getInstance().get(Calendar.MILLISECOND);
    }
}
