package com.liner.facerengineview.Engine;

import android.graphics.Color;
import android.graphics.drawable.Drawable;


import java.io.InputStream;
import java.util.ArrayList;


public class ClockSkinLayer {
    private int angle;
    private int arrayType;
    private int positionX;
    private int positionY;
    private int color;
    private String colorArray;
    private int direction;
    private Drawable backgroundDrawable;
    private InputStream backgroundStream;

    private ArrayList<Drawable> drawables;
    private ArrayList<Integer> durations;
    private ArrayList<DrawableInfo> clockEngineInfos;
    private float currentAngle;
    private float rotateSpeed;
    private int duration;
    private int mulRotate = 1;
    private int rotate;
    private int startAngle;
    private int textsize = 19;
    private String className;
    private String packageName;
    private double offset = 1;
    private float progressDiliverArc;
    private String childrenFolderName;
    private int ValusType;
    private  int progressDiliverCount;
    private int circleRadius;
    private int circleStroken;
    private String drawString = "";
    private Drawable shadowDrawable;
    private int range = 30;
    private float framerateClockSkin;
    private int framerate;

    private float renderScale = 1f;

    public ClockSkinLayer() {
        this.rotate= 0;
        this.mulRotate = 1;
        this.direction = 1;
        this.textsize = 19;
        this.framerateClockSkin = 10f;
        this.color = Color.WHITE;
    }

    public int getFramerate() {
        return framerate;
    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    String getClassName() {
        return className;
    }
    String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public int getAngle(){
        return this.angle;
    }
    public int getArrayType()
    {
        return this.arrayType;
    }
    int getInstinctX(){
        return this.positionX;
    }
    int getInstinctY(){
        return this.positionY;
    }

   public int getPositionX(){
       return (int) (this.positionX * renderScale);
   }
   public int getPositionY(){
       return (int) (this.positionY * renderScale);
   }

    public void setRenderScale(float renderScale) {
        this.renderScale = renderScale;
    }

    public float getFramerateClockSkin() {
        return framerateClockSkin;
    }

    public void setFramerateClockSkin(float framerateClockSkin) {
        this.framerateClockSkin = framerateClockSkin;
    }

    public int getColor()
    {
        return this.color;
    }

    public String getColorArray()
    {
        return this.colorArray;
    }

    public int getDirection()
    {
        return this.direction;
    }

    public Drawable getBackgroundDrawable()
    {
        return this.backgroundDrawable;
    }

    public InputStream getBackgroundStream() {
        return backgroundStream;
    }

    public void setBackgroundStream(InputStream backgroundStream) {
        this.backgroundStream = backgroundStream;
    }

    public ArrayList<Drawable> getDrawableArrays()
    {
        return this.drawables;
    }

    public ArrayList<Integer> getDurationArrays(){
        return this.durations;
    }

    public ArrayList<DrawableInfo> getClockEngineInfos() {
        return clockEngineInfos;
    }

    public int getMulRotate()
    {
        return this.mulRotate;
    }

    public int getRotate()
    {
        return this.rotate;
    }

    public int getStartAngle()
    {
        return this.startAngle;
    }

    public int getTextsize()
    {
        return this.textsize;
    }

    public int getDuration() {
        return duration;
    }

    public float getCurrentAngle() {
        return currentAngle;
    }

    public float getRotateSpeed() {
        return rotateSpeed;
    }

    public void setAngle(int paramInt)
    {
        this.angle = paramInt;
    }

    public void setArrayType(int paramInt)
    {
        this.arrayType = paramInt;
    }

    public void setPositionX(int paramInt)
    {
        this.positionX = paramInt;
    }

    public void setPositionY(int paramInt)
    {
        this.positionY = paramInt;
    }

    public void setColor(int paramInt)
    {
        this.color = paramInt;
    }

    public void setColorArray(String paramString)
    {
        this.colorArray = paramString;
    }

    public void setDirection(int paramInt)
    {
        this.direction = paramInt;
    }

    public void setBackgroundDrawable(Drawable paramDrawable)
    {
        this.backgroundDrawable = paramDrawable;
    }

    public void setDrawableArrays(ArrayList<Drawable> paramArrayList) {
        this.drawables = paramArrayList;
    }

    public void setDurationArrays(ArrayList<Integer> paramArrayList){
        this.durations = paramArrayList;
    }

    public void setClockEngineInfos(ArrayList<DrawableInfo> clockEngineInfos) {
        this.clockEngineInfos = clockEngineInfos;
    }

    public void setMulRotate(int paramInt)
    {
        this.mulRotate = paramInt;
    }

    public void setRotate(int paramInt)
    {
        this.rotate = paramInt;
    }

    public void setStartAngle(int paramInt)
    {
        this.startAngle = paramInt;
    }

    public void setTextsize(int paramInt)
    {
        this.textsize = paramInt;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setCurrentAngle(float currentAngle) {
        this.currentAngle = currentAngle;
    }

    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    String getChildrenFolderName() {
        return childrenFolderName;
    }
    public void setChildrenFolderName(String childrenFolderName) {
        this.childrenFolderName = childrenFolderName;
    }
    public int getValusType() {
        return ValusType;
    }
    public void setValusType(int valusType) {
        ValusType = valusType;
    }
    public float getProgressDiliverArc() {
        return progressDiliverArc;
    }
    public void setProgressDiliverArc(float progressDiliverArc) {
        this.progressDiliverArc = progressDiliverArc;
    }
    public int getProfressDiliverCount() {
        return progressDiliverCount;
    }
    public void setProfressDiliverCount(int profressDiliverCount) {
        this.progressDiliverCount = profressDiliverCount;
    }
    public int getCircleRadius() {
        return circleRadius;
    }
    public void setCircleRadius(int circleRadius) {
        this.circleRadius = circleRadius;
    }
    public int getCircleStroken() {
        return circleStroken;
    }
    public void setCircleStroken(int circleStroken) {
        this.circleStroken = circleStroken;
    }
    public int getCenterXNew()
    {
        return this.positionX;
    }
    public int getCenterYNew()
    {
        return this.positionY;
    }
    public Drawable getShadowDrawable() {
        return shadowDrawable;
    }
    public void setShadowDrawable(Drawable shadowDrawable) {
        this.shadowDrawable = shadowDrawable;
    }





    public static class DrawableInfo {
        private Drawable drawable;
        private float drawableScale, drawableX, drawableY, rotateSpeed;

        public DrawableInfo(Drawable drawable, float drawableScale, float drawableX, float drawableY, float rotateSpeed) {
            this.drawable = drawable;
            this.drawableScale = drawableScale;
            this.drawableX = drawableX;
            this.drawableY = drawableY;
            this.rotateSpeed = rotateSpeed;
        }

        public DrawableInfo(){
        }
        public Drawable getDrawable() {
            return drawable;
        }
        public float getDrawableScale() {
            return drawableScale;
        }
        public float getDrawableX() {
            return drawableX;
        }
        public float getDrawableY() {
            return drawableY;
        }
        public float getRotateSpeed() {
            return rotateSpeed;
        }
        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }
        public void setDrawableScale(float drawableScale) {
            this.drawableScale = drawableScale;
        }
        public void setDrawableX(float drawableX) {
            this.drawableX = drawableX;
        }
        public void setDrawableY(float drawableY) {
            this.drawableY = drawableY;
        }
        public void setRotateSpeed(float rotateSpeed) {
            this.rotateSpeed = rotateSpeed;
        }
    }
}