package com.liner.facerengineview.Engine;
import android.annotation.SuppressLint;
import android.content.Context;
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
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.liner.facerengineview.Engine.Util.FacerUtil;
import com.liner.facerengineview.Engine.Util.Parser.LowMemoryParser;
import com.liner.facerengineview.Engine.Util.Parser.TagParser;
import com.liner.facerengineview.Engine.Util.WatchFaceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.liner.facerengineview.Engine.Util.FacerUtil.SHAPE_CIRCLE;
import static com.liner.facerengineview.Engine.Util.FacerUtil.SHAPE_LINE;
import static com.liner.facerengineview.Engine.Util.FacerUtil.SHAPE_POLYGON;
import static com.liner.facerengineview.Engine.Util.FacerUtil.SHAPE_SQUARE;
import static com.liner.facerengineview.Engine.Util.FacerUtil.SHAPE_TRIANGLE;

public class RenderFacerView extends View implements Runnable {
    private Context context;
    private ArrayList<HashMap<String, String>> watchFaceLayers;
    private HashMap<String, BitmapDrawable> drawableHashMap;
    private HashMap<String, Typeface> typefaceHashMap;

    private LowMemoryParser lowMemoryParser;

    private Thread mThread;
    private boolean isRunning = false;
    private int UPDATE_FREQ = 50;

    private boolean isProtected;
    private boolean canvasInited;
    private boolean isRoundWatch = true;
    private boolean isLowPower = false;

    private boolean shouldStroke = false;
    private int strokeColor = Color.BLACK;

    private float renderScale = 2f;

    private File imageDirectory;


    private Paint strokePaint = new Paint();
    private Paint canvasPaint = new Paint();

    private Path canvasPath = new Path();

    public RenderFacerView(Context context) {
        super(context);
        this.context = context;
    }

    public RenderFacerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @SuppressLint("DrawAllocation")
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
        canvas.drawColor(Color.BLACK);
        canvasPaint.reset();
        if(lowMemoryParser != null){
            lowMemoryParser.updateData();
            if(watchFaceLayers.size() != 0){
                for(HashMap<String, String> layerData: watchFaceLayers){
                    if(layerData.containsKey("type")){
                        if(layerData.get("type").matches("text")){
                            canvasPaint.reset();
                            float alpha;
                            try {
                                alpha = Math.round(2.55d * TagParser.parseTextFloat(context,layerData.get("opacity")));
                            } catch (NumberFormatException e){
                                alpha = 255f;
                            }
                            if(alpha != 0f){
                                Typeface typeface;
                                String text;
                                if(layerData.containsKey("text")){
                                    text = TagParser.parseText(context,layerData.get("text"));
                                } else {
                                    text = "_ERR_";
                                }
                                if(layerData.containsKey("transform")) {
                                    if (layerData.get("transform").matches("1")) {
                                        text.toUpperCase();
                                    } else if (layerData.get("transform").matches("2")) {
                                        text.toLowerCase();
                                    }
                                }
                                if(isLowPower){
                                    if(layerData.containsKey("low_power_color")){
                                        canvasPaint.setColor(Integer.parseInt(layerData.get("low_power_color")));
                                    } else {
                                        canvasPaint.setColor(-1);
                                    }
                                } else {
                                    if (layerData.containsKey("color")) {
                                        canvasPaint.setColor(Integer.parseInt(layerData.get("color")));
                                    } else {
                                        canvasPaint.setColor(-1);
                                    }
                                }
                                if(layerData.containsKey("text_effect")){
                                    shouldStroke = false;
                                    strokeColor = Color.BLACK;
                                    canvasPaint.clearShadowLayer();
                                    switch (Integer.valueOf(layerData.get("text_effect"))){
                                        case 1:
                                            if(layerData.containsKey("stroke_size")){
                                                canvasPaint.setStrokeWidth(Float.parseFloat(layerData.get("stroke_size")));
                                            }
                                            if (layerData.containsKey("stroke_color")) {
                                                strokeColor = Integer.parseInt(layerData.get("stroke_color"));
                                            }
                                            shouldStroke = true;

                                            break;
                                        case 2:
                                            if(!layerData.containsKey("glow_size") || !layerData.containsKey("glow_color")){
                                                break;
                                            }
                                            canvasPaint.setShadowLayer((float) Integer.valueOf(layerData.get("glow_size")), 0.0f, 0.0f, Integer.valueOf(layerData.get("glow_color")));
                                            shouldStroke = true;
                                            break;
                                        default:
                                            break;
                                    }
                                } else {
                                    shouldStroke = false;
                                    strokeColor = Color.BLACK;
                                    canvasPaint.clearShadowLayer();
                                }
                                if(layerData.containsKey("size")){
                                    try {
                                        canvasPaint.setTextSize(renderScale*TagParser.parseTextFloat(context,layerData.get("size")));
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
                                    if (layerData.get("alignment").matches("0")) {
                                        canvasPaint.setTextAlign(Paint.Align.LEFT);
                                    } else {
                                        if (layerData.get("alignment").matches("1")) {
                                            canvasPaint.setTextAlign(Paint.Align.CENTER);
                                        } else {
                                            if (layerData.get("alignment").matches("2")) {
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
                                    } else {
                                        if (new File(FacerUtil.getFontDirectory(), layerData.get("new_font_name")).exists()) {
                                            try {
                                                typefaceHashMap.put(layerData.get("new_font_name"), Typeface.createFromFile(new File(FacerUtil.getFontDirectory(), layerData.get("new_font_name"))));
                                                canvasPaint.setTypeface(Typeface.createFromFile(new File(FacerUtil.getFontDirectory(), layerData.get("new_font_name"))));
                                            } catch (Exception e3) {
                                                e3.printStackTrace();
                                            }
                                        }
                                    }
                                }
                                if(layerData.containsKey("r")){
                                    canvas.save();
                                    canvas.rotate(TagParser.parseTextFloat(context,layerData.get("r")), (float) Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale), (float) Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale));
                                }
                                canvasPaint.setAntiAlias(true);
                                if(shouldStroke){
                                    canvasPaint.setStyle(Paint.Style.STROKE);
                                    canvasPaint.setColor(strokeColor);
                                    if(isLowPower){
                                        if(layerData.containsKey("low_power")){
                                            if (Boolean.valueOf(layerData.get("low_power"))) {
                                                canvas.drawText(text, (float) Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale), (float) Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale), canvasPaint);
                                                canvasPaint.setStyle(Paint.Style.FILL);
                                                canvasPaint.setColor(Color.BLACK);
                                            }
                                        }
                                    } else {
                                        canvas.drawText(text, (float) Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale), (float) Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale), canvasPaint);
                                    }
                                } else {
                                    if (isLowPower) {
                                        if (layerData.containsKey("low_power")) {
                                            if (Boolean.valueOf(layerData.get("low_power"))) {
                                                canvas.drawText(text, (float) Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale), (float) Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale), canvasPaint);
                                            }
                                        }
                                    }
                                    if (!isLowPower) {
                                        canvas.drawText(text, (float) Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale), (float) Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale), canvasPaint);
                                    }
                                }
                                canvas.restore();
                                canvasPaint.setTextAlign(Paint.Align.LEFT);
                            }
                        } else {
                            File file;
                            byte[] converted_data;
                            BitmapDrawable bitmapDrawable;
                            BitmapDrawable mBitmap;
                            int b1;
                            int b2;
                            int b3;
                            int b4;
                            float alpha;
                            if (layerData.get("type").matches("image")) {
                                canvasPaint.reset();
                                canvasPaint.setAntiAlias(true);
                                try {
                                    alpha = TagParser.parseTextFloat(context,layerData.get("opacity"));
                                } catch (NumberFormatException e6) {
                                    alpha = 100.0f;
                                }
                                alpha = (float) Math.round(((double) alpha) * 2.55d);
                                if (alpha != 0.0f) {
                                    if (layerData.containsKey("hash")) {
                                        if (!drawableHashMap.containsKey(layerData.get("hash"))) {
                                            file = new File(imageDirectory, layerData.get("hash"));
                                            if (file.exists()) {
                                                Log.d("RenderView", "Loading a bitmap. [ " + layerData.get("hash") + " ]");
                                                if (isProtected) {
                                                    try {
                                                        converted_data = Base64.decode(FacerUtil.read(file), 0);
                                                        bitmapDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(converted_data, 0, converted_data.length));
                                                        drawableHashMap.put(layerData.get("hash"), bitmapDrawable);
                                                    } catch (IllegalArgumentException e7) {
                                                        bitmapDrawable = new BitmapDrawable(getResources(), FacerUtil.decodeSampledBitmap(file.getPath(), getWidth()));
                                                        drawableHashMap.put(layerData.get("hash"), bitmapDrawable);
                                                    }
                                                } else {
                                                    bitmapDrawable = new BitmapDrawable(getResources(), FacerUtil.decodeSampledBitmap(file.getPath(), getWidth()));
                                                    drawableHashMap.put(layerData.get("hash"), bitmapDrawable);
                                                }
                                            }
                                        }
                                        mBitmap = drawableHashMap.get(layerData.get("hash"));
                                        int tempX = Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale);
                                        int tempY = Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale);
                                        int tempWidth = Math.round(TagParser.parseTextFloat(context,layerData.get("width")) * renderScale);
                                        int tempHeight = Math.round(TagParser.parseTextFloat(context,layerData.get("height")) * renderScale);
                                        float tempR = TagParser.parseTextFloat(context,layerData.get("r"));
                                        int tmpOffset;
                                        try {
                                            tmpOffset = Integer.valueOf(layerData.get("alignment"));
                                        } catch (NumberFormatException e8) {
                                            e8.printStackTrace();
                                            tmpOffset = 0;
                                        }
                                        switch (tmpOffset) {
                                            case 1:
                                                b1 = tempX - (tempWidth / 2);
                                                b2 = tempY;
                                                b3 = tempX + (tempWidth / 2);
                                                b4 = tempHeight + tempY;
                                                break;
                                            case 2:
                                                b1 = tempX - tempWidth;
                                                b2 = tempY;
                                                b3 = tempX;
                                                b4 = tempHeight + tempY;
                                                break;
                                            case 3:
                                                b1 = tempX;
                                                b2 = tempY - (tempHeight / 2);
                                                b3 = tempWidth + tempX;
                                                b4 = tempY + (tempHeight / 2);
                                                break;
                                            case 4:
                                                b1 = tempX - (tempWidth / 2);
                                                b2 = tempY - (tempHeight / 2);
                                                b3 = tempX + (tempWidth / 2);
                                                b4 = tempY + (tempHeight / 2);
                                                break;
                                            case 5:
                                                b1 = tempX - tempWidth;
                                                b2 = tempY - (tempHeight / 2);
                                                b3 = tempX;
                                                b4 = tempY + (tempHeight / 2);
                                                break;
                                            case 6:
                                                b1 = tempX;
                                                b2 = tempY - tempHeight;
                                                b3 = tempX + tempWidth;
                                                b4 = tempY;
                                                break;
                                            case 7:
                                                b1 = tempX - (tempWidth / 2);
                                                b2 = tempY - tempHeight;
                                                b3 = tempX + (tempWidth / 2);
                                                b4 = tempY;
                                                break;
                                            case 8:
                                                b1 = tempX - tempWidth;
                                                b2 = tempY - tempHeight;
                                                b3 = tempX;
                                                b4 = tempY;
                                                break;
                                            default:
                                                b1 = tempX;
                                                b2 = tempY;
                                                b3 = tempWidth + tempX;
                                                b4 = tempHeight + tempY;
                                                break;
                                        }
                                        if (layerData.containsKey("is_tinted")) {
                                            if (layerData.containsKey("tint_color") && mBitmap != null) {
                                                if (Boolean.valueOf(layerData.get("is_tinted"))) {
                                                    mBitmap.setColorFilter(new PorterDuffColorFilter(Integer.valueOf(layerData.get("tint_color")), PorterDuff.Mode.MULTIPLY));
                                                } else {
                                                    mBitmap.setColorFilter(null);
                                                }
                                            }
                                        }
                                        if (mBitmap != null) {
                                            mBitmap.setAlpha(Math.round(alpha));
                                        }
                                        if (Boolean.valueOf(layerData.get("low_power")) || !isLowPower) {
                                            canvas.save();
                                            canvas.rotate(tempR, (float) tempX, (float) tempY);
                                            if (mBitmap != null) {
                                                mBitmap.setBounds(b1, b2, b3, b4);
                                                mBitmap.draw(canvas);
                                            }
                                            canvas.restore();
                                        }
                                    }
                                }
                            } else if(layerData.get("type").matches("dynamic_image")){
                                canvasPaint.reset();
                                canvasPaint.setAntiAlias(true);
                                try {
                                    alpha = TagParser.parseTextFloat(context,layerData.get("opacity"));
                                } catch (NumberFormatException e6) {
                                    alpha = 100.0f;
                                }
                                alpha = (float) Math.round(((double) alpha) * 2.55d);
                                if (alpha != 0.0f) {
                                    if (layerData.containsKey("hash_round")) {
                                        if (!drawableHashMap.containsKey(layerData.get("hash_round"))) {
                                            file = new File(imageDirectory, layerData.get("hash_round"));
                                            if (file.exists()) {
                                                Log.d("RenderView", "Loading a bitmap. [ " + layerData.get("hash_round") + " ]");
                                                if (isProtected) {
                                                    try {
                                                        converted_data = Base64.decode(FacerUtil.read(file), 0);
                                                        bitmapDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(converted_data, 0, converted_data.length));
                                                        drawableHashMap.put(layerData.get("hash_round"), bitmapDrawable);
                                                    } catch (IllegalArgumentException e7) {
                                                        bitmapDrawable = new BitmapDrawable(getResources(), FacerUtil.decodeSampledBitmap(file.getPath(), getWidth()));
                                                        drawableHashMap.put(layerData.get("hash_round"), bitmapDrawable);
                                                    }
                                                } else {
                                                    bitmapDrawable = new BitmapDrawable(getResources(), FacerUtil.decodeSampledBitmap(file.getPath(), getWidth()));
                                                    drawableHashMap.put(layerData.get("hash_round"), bitmapDrawable);
                                                }
                                            }
                                        }
                                        if (layerData.containsKey("hash_round_ambient")) {
                                            if (layerData.containsKey("hash_round_ambient")) {
                                                if (!drawableHashMap.containsKey(layerData.get("hash_round_ambient")) && isRoundWatch) {
                                                    file = new File(imageDirectory, layerData.get("hash_round_ambient"));
                                                    if (file.exists()) {
                                                        Log.d("RenderView", "Loading a bitmap. [ " + layerData.get("hash_round_ambient") + " ]");
                                                        if (isProtected) {
                                                            converted_data = Base64.decode(FacerUtil.read(file), 0);
                                                            bitmapDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(converted_data, 0, converted_data.length));
                                                            drawableHashMap.put(layerData.get("hash_round_ambient"), bitmapDrawable);
                                                        } else {
                                                            bitmapDrawable = new BitmapDrawable(getResources(), FacerUtil.decodeSampledBitmap(file.getPath(), getWidth()));
                                                            drawableHashMap.put(layerData.get("hash_round_ambient"), bitmapDrawable);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        String hash = null;
                                        if(isLowPower && isRoundWatch){
                                            if (layerData.containsKey("hash_round_ambient")) {
                                                if (layerData.get("hash_round_ambient").length() != 0) {
                                                    hash = layerData.get("hash_round_ambient");
                                                }
                                            }
                                            if (layerData.containsKey("hash_square_ambient")) {
                                                hash = layerData.get("hash_square_ambient");
                                            }
                                        } else if(isRoundWatch){
                                            if (layerData.containsKey("hash_round")) {
                                                if (layerData.get("hash_round").length() != 0) {
                                                    hash = layerData.get("hash_round");
                                                }
                                            }
                                        }
                                        mBitmap = drawableHashMap.get(hash);
                                        int tempX = Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale);
                                        int tempY = Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale);
                                        int tempWidth = Math.round(TagParser.parseTextFloat(context,layerData.get("width")) * renderScale);
                                        int tempHeight = Math.round(TagParser.parseTextFloat(context,layerData.get("height")) * renderScale);
                                        float tempR = TagParser.parseTextFloat(context,layerData.get("r"));
                                        int tmpOffset;
                                        try {
                                            tmpOffset = Integer.valueOf(layerData.get("alignment"));
                                        } catch (NumberFormatException e8) {
                                            e8.printStackTrace();
                                            tmpOffset = 0;
                                        }
                                        switch (tmpOffset) {
                                            case 1:
                                                b1 = tempX - (tempWidth / 2);
                                                b2 = tempY;
                                                b3 = tempX + (tempWidth / 2);
                                                b4 = tempHeight + tempY;
                                                break;
                                            case 2:
                                                b1 = tempX - tempWidth;
                                                b2 = tempY;
                                                b3 = tempX;
                                                b4 = tempHeight + tempY;
                                                break;
                                            case 3:
                                                b1 = tempX;
                                                b2 = tempY - (tempHeight / 2);
                                                b3 = tempWidth + tempX;
                                                b4 = tempY + (tempHeight / 2);
                                                break;
                                            case 4:
                                                b1 = tempX - (tempWidth / 2);
                                                b2 = tempY - (tempHeight / 2);
                                                b3 = tempX + (tempWidth / 2);
                                                b4 = tempY + (tempHeight / 2);
                                                break;
                                            case 5:
                                                b1 = tempX - tempWidth;
                                                b2 = tempY - (tempHeight / 2);
                                                b3 = tempX;
                                                b4 = tempY + (tempHeight / 2);
                                                break;
                                            case 6:
                                                b1 = tempX;
                                                b2 = tempY - tempHeight;
                                                b3 = tempX + tempWidth;
                                                b4 = tempY;
                                                break;
                                            case 7:
                                                b1 = tempX - (tempWidth / 2);
                                                b2 = tempY - tempHeight;
                                                b3 = tempX + (tempWidth / 2);
                                                b4 = tempY;
                                                break;
                                            case 8:
                                                b1 = tempX - tempWidth;
                                                b2 = tempY - tempHeight;
                                                b3 = tempX;
                                                b4 = tempY;
                                                break;
                                            default:
                                                b1 = tempX;
                                                b2 = tempY;
                                                b3 = tempWidth + tempX;
                                                b4 = tempHeight + tempY;
                                                break;
                                        }
                                        if (layerData.containsKey("is_tinted")) {
                                            if (layerData.containsKey("tint_color") && mBitmap != null) {
                                                if (Boolean.valueOf(layerData.get("is_tinted"))) {
                                                    mBitmap.setColorFilter(new PorterDuffColorFilter(Integer.valueOf(layerData.get("tint_color")), PorterDuff.Mode.MULTIPLY));
                                                } else {
                                                    mBitmap.setColorFilter(null);
                                                }
                                            }
                                        }
                                        if (mBitmap != null) {
                                            mBitmap.setAlpha(Math.round(alpha));
                                        }
                                        if (Boolean.valueOf(layerData.get("low_power")) || !isLowPower) {
                                            canvas.save();
                                            canvas.rotate(tempR, (float) tempX, (float) tempY);
                                            if (mBitmap != null) {
                                                mBitmap.setBounds(b1, b2, b3, b4);
                                                mBitmap.draw(canvas);
                                            }
                                            canvas.restore();
                                        }
                                    }
                                }
                            } else if(layerData.get("type").matches("shape")){
                                if(layerData.containsKey("shape_type")){
                                    if(Boolean.valueOf(layerData.get("low_power")) || !isLowPower){
                                        canvasPaint.reset();
                                        canvasPaint.setAntiAlias(true);
                                        int width = 90;
                                        int height = 90;
                                        int radius = 0;
                                        boolean shouldClip = false;
                                        int tempX = Math.round(TagParser.parseTextFloat(context,layerData.get("x")) * renderScale);
                                        int tempY = Math.round(TagParser.parseTextFloat(context,layerData.get("y")) * renderScale);
                                        float tempR = TagParser.parseTextFloat(context,layerData.get("r"));
                                        int sides = Integer.parseInt(layerData.get("sides"));
                                        if(layerData.containsKey("color")){
                                            canvasPaint.setColor(Integer.parseInt(layerData.get("color")));
                                        } else {
                                            canvasPaint.setColor(-1);
                                        }
                                        switch (Integer.valueOf(layerData.get("shape_opt"))){
                                            case 0:
                                                canvasPaint.setStyle(Paint.Style.FILL);
                                                break;
                                            case 1:
                                                canvasPaint.setStyle(Paint.Style.STROKE);
                                                if(layerData.containsKey("stroke_size")){
                                                    canvasPaint.setStrokeWidth(TagParser.parseTextFloat(context,layerData.get("stroke_size"))+renderScale);
                                                }
                                                break;
                                            case 2:
                                                shouldClip = true;
                                                break;

                                        }
                                        if(layerData.containsKey("opacity")){
                                            try {
                                                alpha = TagParser.parseTextFloat(context,layerData.get("opacity"));
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
                                            radius = Math.round(TagParser.parseTextFloat(context,"radius")*renderScale);
                                        }
                                        if(layerData.containsKey("width") && layerData.containsKey("height")){
                                            width = Math.round(TagParser.parseTextFloat(context,layerData.get("width")) * renderScale);
                                            height = Math.round(TagParser.parseTextFloat(context,layerData.get("height")) * renderScale);
                                        }
                                        canvas.save();
                                        canvas.rotate(tempR, (float)tempX, (float)tempY);
                                        if(!shouldClip){
                                            switch (Integer.parseInt(layerData.get("shape_type"))) {
                                                case SHAPE_CIRCLE:
                                                    canvas.drawCircle((float) tempX, (float) tempY, (float) radius, canvasPaint);
                                                    break;
                                                case SHAPE_SQUARE:
                                                case SHAPE_LINE:
                                                    canvas.drawRect((float) tempX, (float) tempY, ((float) width) + ((float) tempX), ((float) height) + ((float) tempY), canvasPaint);
                                                    break;
                                                case SHAPE_POLYGON:

                                                    canvas.drawPath(FacerUtil.calculatePolygonPoints(renderScale, sides, radius, tempX, tempY, renderScale), canvasPaint);
                                                    break;
                                                case SHAPE_TRIANGLE:
                                                    canvas.drawPath(FacerUtil.calculatePolygonPoints(renderScale, 3, radius, tempX, tempY, renderScale), canvasPaint);
                                                    break;
                                            }
                                        }
                                        Path tempPath = new Path();
                                        switch (Integer.valueOf(layerData.get("shape_type"))) {
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
                                                canvas.clipPath(FacerUtil.calculatePolygonPoints(renderScale, sides, radius, tempX, tempY, renderScale));
                                                break;
                                            case SHAPE_TRIANGLE:
                                                canvas.clipPath(FacerUtil.calculatePolygonPoints(renderScale, 3, radius, tempX, tempY, renderScale));
                                                break;
                                        }
                                        canvas.restore();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                lowMemoryParser.updateData();
                postInvalidate();
                Thread.sleep(UPDATE_FREQ);
            } catch (InterruptedException localInterruptedException) {
                localInterruptedException.printStackTrace();
            }
        }
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

    public boolean isLowPower() {
        return isLowPower;
    }

    public boolean isRoundWatch() {
        return isRoundWatch;
    }

    public void setLowPower(boolean lowPower) {
        this.isLowPower = lowPower;
    }

    public void setRenderScale(float renderScale) {
        this.renderScale = renderScale;
    }

    public void setRoundWatch(boolean roundWatch) {
        this.isRoundWatch = roundWatch;
    }

    public void init(WatchFaceData watchFaceData){
        imageDirectory = watchFaceData.getWatchFaceImageDir();
        isProtected = watchFaceData.isProtected();
        watchFaceLayers = new ArrayList<>();
        drawableHashMap = new HashMap<>();
        typefaceHashMap = new HashMap<>();
        if(watchFaceData.getWatchFaceJSON() != null){
            Log.e("FacerView", "--init, watchFaceJSON not null, processing...");
            lowMemoryParser = new LowMemoryParser(context, watchFaceData.getWatchFaceJSON());
            watchFaceLayers.clear();
            drawableHashMap.clear();
            typefaceHashMap.clear();
            for(int i = 0; i<watchFaceData.getWatchFaceJSON().length(); i++){
                try {
                    JSONObject layerJson = watchFaceData.getWatchFaceJSON().getJSONObject(i);
                    Log.e("FacerView", "--init, load new layer: "+layerJson.toString());
                    HashMap<String, String> layer = new HashMap<>();
                    Iterator<String> iterator = layerJson.keys();
                    while (iterator.hasNext()){
                        String layerKey = iterator.next();
                        Log.e("FacerView", "--init, parsing later | key=["+layerKey+"] | value=["+layerJson.get(layerKey)+"]");
                        layer.put(layerKey, String.valueOf(layerJson.get(layerKey)));
                    }
                    watchFaceLayers.add(layer);
                    Log.e("FacerView", "--init, new layer added");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("FacerView", "--init, got exception: "+e.toString());
                }
            }
        }
    }
}
