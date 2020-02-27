package com.liner.facerengineview.Engine.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;


public class WatchFaceData {
    private JSONArray watchFace;
    private JSONObject watchFaceData;
    private File watchFaceDirectory;
    private File watchFaceFontsDirectory;
    private File watchFaceImageDirectory;
    private File watchFaceDescriptionFile;
    private File watchFaceLayersDataFile;
    private File watchFaceSettingsFile;

    public WatchFaceData(String name) {
        this.watchFace = null;
        this.watchFaceData = null;
        this.watchFaceDirectory = null;
        this.watchFaceImageDirectory = null;
        this.watchFaceFontsDirectory = null;
        this.watchFaceLayersDataFile = null;
        this.watchFaceDescriptionFile = null;
        this.watchFaceSettingsFile = null;
        this.watchFaceDirectory = new File(Environment.getExternalStorageDirectory(), "Facer/" + name);
        this.watchFaceFontsDirectory = new File(watchFaceDirectory, "fonts");
        this.watchFaceImageDirectory = new File(watchFaceDirectory, "images");
        this.watchFaceLayersDataFile = new File(watchFaceDirectory, "watchface.json");
        this.watchFaceDescriptionFile = new File(watchFaceDirectory, "description.json");
        this.watchFaceSettingsFile = new File(watchFaceDirectory, "watchface_settings.json");
        if (watchFaceDirectory.exists()) {
            try {
                this.watchFaceData = new JSONObject(FacerUtil.read(watchFaceDescriptionFile));
                if (isProtected()) {
                    String tempData = new String(Base64.decode(FacerUtil.read(watchFaceLayersDataFile), 0), "UTF-8");
                    this.watchFace = new JSONArray(tempData);
                } else {
                    this.watchFace = new JSONArray(FacerUtil.read(watchFaceLayersDataFile));
                }
            } catch (JSONException | UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
    }

    public int getProtectionVersion() {
        try {
            if (watchFaceData.has("protection_version")) {
                return watchFaceData.getInt("protection_version");
            }
            return 0;
        } catch (JSONException e) {
            return 0;
        }
    }

    public boolean isProtected() {
        try {
            if (watchFaceData.has("is_protected")) {
                return watchFaceData.getBoolean("is_protected");
            }
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }


    public JSONObject getWatchFaceSettings() {
        try {
            return new JSONObject(FacerUtil.read(watchFaceSettingsFile));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }


    public File getWatchFaceDir() {
        return watchFaceDirectory;
    }

    public File getWatchFaceImageDir() {
        return watchFaceImageDirectory;
    }

    public File getWatchFaceFontDir() {
        return watchFaceFontsDirectory;
    }

    public String getName() {
        String temp = "null";
        try {
            return watchFaceData.getString("title");
        } catch (JSONException e) {
            e.printStackTrace();
            return temp;
        }
    }

    public JSONArray getWatchFaceJSON() {
        boolean singleLayer = false;
        if (singleLayer) {
            try {
                JSONArray mTemp = new JSONArray();
                int singleID = 0;
                mTemp.put(watchFace.getJSONObject(singleID));
                return mTemp;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return watchFace;
    }

    public File getPreviewFile() {
        File mTemp = new File(watchFaceDirectory, "/preview.png");
        if (!mTemp.exists()) {
            try {
                mTemp.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mTemp;
    }

    public Bitmap getPreview() {
        return BitmapFactory.decodeFile(watchFaceDirectory.getPath() + "/preview.png");
    }

    public boolean usesHighFramerate() {
        String data = watchFace.toString();
        String SPECIAL_REGEX = "\\(([^:\\r\\n]*)\\)";
        return data.contains("#DWFSS#") || data.contains("#DSMOOTH#") || Pattern.compile("\\(([^:\\r\\n]*)\\)").matcher(data).find();
    }

    public File getWatchFaceDescriptionFile() {
        return watchFaceDescriptionFile;
    }

    public File getWatchFaceDirectory() {
        return watchFaceDirectory;
    }

    public File getWatchFaceFontsDirectory() {
        return watchFaceFontsDirectory;
    }

    public File getWatchFaceImageDirectory() {
        return watchFaceImageDirectory;
    }

    public File getWatchFaceSettingsFile() {
        return watchFaceSettingsFile;
    }

    public File getWatchFaceLayersDataFile() {
        return watchFaceLayersDataFile;
    }

    public JSONObject getWatchFaceData() {
        return watchFaceData;
    }

    public JSONArray getWatchFace() {
        return watchFace;
    }
}
