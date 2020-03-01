package com.liner.facerengineview.Engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class RenderFacerView extends View implements Runnable {
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



    public RenderFacerView(Context context) {
        super(context);
        this.context = context;
        setLayerType(LAYER_TYPE_HARDWARE, canvasPaint);
    }

    public RenderFacerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setLayerType(LAYER_TYPE_HARDWARE, canvasPaint);
    }

    public void init(File file){
        watchFaceLayers = new ArrayList<>();
        drawableHashMap = new HashMap<>();
        typefaceHashMap = new HashMap<>();
        if (file.exists()) {
            String manifest = read(getFileFromZip(file, "watchface.json"));
            String description = read(getFileFromZip(file, "description.json"));
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
                                typefaceHashMap.put(layer.get("new_font_name"), Typeface.createFromFile(getFileFromZip(file, "fonts/"+layer.get("new_font_name"))));
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




    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        renderScale = (float)getWidth() / 320f;
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
        if(parserUtils != null){
            parserUtils.updateTagValues();
            if(watchFaceLayers.size() != 0){
                for(HashMap<String, String> layerData: watchFaceLayers){
                    if(layerData.containsKey("type")){
                        switch (Objects.requireNonNull(layerData.get("type"))){
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
    }

    //Control engine draw calls
    @Override
    public void run() {
        while (isRunning) {
            try {
                parserUtils.updateTagValues();
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
            imageFile = getFileFromZip(watchfaceFile, "images/" + layer.get(key));
            if (imageFile != null) {
                if (isProtected) {
                    try {
                        converted_data = Base64.decode(read(imageFile), 0);
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
    private String read(File mFile) {
        String mTemp = "";
        try {
            FileInputStream stream = new FileInputStream(mFile);
            FileChannel fc = stream.getChannel();
            mTemp = Charset.defaultCharset().decode(fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mTemp;
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
    private File getFileFromZip(File filePath, String name) {
        File file;
        try {
            ZipFile zipFile = new ZipFile(filePath.getAbsolutePath());
            ZipEntry ze = zipFile.getEntry(name);
            if (ze == null) {
                return null;
            }
            InputStream inputStream = zipFile.getInputStream(ze);
            file = File.createTempFile("tempfile", "_watch");
            OutputStream output = new FileOutputStream(file);
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = inputStream.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    output.write(buffer, 0, read);
                }
                output.flush();
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } catch (Throwable th) {
                output.close();
            }
            inputStream.close();
            return file;
        } catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }
}
