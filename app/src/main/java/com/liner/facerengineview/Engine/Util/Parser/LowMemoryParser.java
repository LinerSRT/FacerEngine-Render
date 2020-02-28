package com.liner.facerengineview.Engine.Util.Parser;
import android.content.Context;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LowMemoryParser {
    private Context mContext;
    private HashMap<String, String> mData = new HashMap<>();

    public LowMemoryParser(Context context, JSONArray jsonArray) {
        this.mContext = context;
        fillHashMap(jsonArray);
    }
    private void fillHashMap(JSONArray array){
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject layer = array.getJSONObject(i);
                if (layer.getString("type").matches("image")) {
                    if (layer.has("opacity")) {
                        checkKeyExist(layer.getString("opacity"));
                        
                    }
                    if (layer.has("r")) {
                        checkKeyExist(layer.getString("r"));
                    }
                    if (layer.has("x")) {
                        checkKeyExist(layer.getString("x"));
                    }
                    if (layer.has("y")) {
                        checkKeyExist(layer.getString("y"));
                    }
                    if (layer.has("width")) {
                        checkKeyExist(layer.getString("width"));
                    }
                    if (layer.has("height")) {
                        checkKeyExist(layer.getString("height"));
                    }
                } else if (layer.getString("type").matches("dynamic_image")) {
                    if (layer.has("opacity")) {
                        checkKeyExist(layer.getString("opacity"));
                    }
                    if (layer.has("r")) {
                        checkKeyExist(layer.getString("r"));
                    }
                    if (layer.has("x")) {
                        checkKeyExist(layer.getString("x"));
                    }
                    if (layer.has("y")) {
                        checkKeyExist(layer.getString("y"));
                    }
                    if (layer.has("width")) {
                        checkKeyExist(layer.getString("width"));
                    }
                    if (layer.has("height")) {
                        checkKeyExist(layer.getString("height"));
                    }
                } else if (layer.getString("type").matches("text")) {
                    if (layer.has("opacity")) {
                        checkKeyExist(layer.getString("opacity"));
                    }
                    if (layer.has("r")) {
                        checkKeyExist(layer.getString("r"));
                    }
                    if (layer.has("x")) {
                        checkKeyExist(layer.getString("x"));
                    }
                    if (layer.has("y")) {
                        checkKeyExist(layer.getString("y"));
                    }
                    if (layer.has("text")) {
                        checkKeyExist(layer.getString("text"));
                    }
                    if (layer.has("size")) {
                        checkKeyExist(layer.getString("size"));
                    }
                } else if (layer.getString("type").matches("shape")) {
                    if (layer.has("opacity")) {
                        checkKeyExist(layer.getString("opacity"));
                    }
                    if (layer.has("r")) {
                        checkKeyExist(layer.getString("r"));
                    }
                    if (layer.has("x")) {
                        checkKeyExist(layer.getString("x"));
                    }
                    if (layer.has("y")) {
                        checkKeyExist(layer.getString("y"));
                    }
                    if (layer.has("width")) {
                        checkKeyExist(layer.getString("width"));
                    }
                    if (layer.has("height")) {
                        checkKeyExist(layer.getString("height"));
                    }
                    if (layer.has("radius")) {
                        checkKeyExist(layer.getString("radius"));
                    }
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    public void updateData() {
        for (Map.Entry<String, String> entry : mData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("#D")) {
                mData.put(key, TagParser.processText(mContext, key));
            } else if (key.startsWith("#Z")) {
                mData.put(key, TagParser.processHealth(key));
            } else if (key.startsWith("#B")) {
                mData.put(key, TagParser.processBattery(mContext,key));
            } else if (key.startsWith("#P")) {
                mData.put(key, TagParser.processPhone(mContext,key));
            } else if (key.startsWith("#W")) {
                // todo produce make weather parser
            } else {
                mData.put(key, "unknown");
            }
        }
    }
    
    private void checkKeyExist(String key){
        Pattern pattern = Pattern.compile("#\\w*#");
        Matcher matcher = pattern.matcher(key);
        matcher.reset(key);
        while (matcher.find()) {
            if (!mData.containsKey(matcher.group())) {
                mData.put(matcher.group(), "");
            }
        }
    }

    public String parse(String key){
        String temp = "";
        if(mData != null){
            if (key != null) {
                for (int i = 0; i < key.length() - 1; i++) {
                    if (key.charAt(i) == '#') {
                        for (int i2 = i + 1; i2 < key.length(); i2++) {
                            if (key.charAt(i2) == '#') {
                                temp = key.substring(i, i2 + 1);
                                break;
                            }
                        }
                        if (mData.containsKey(temp)) {
                            key = key.replace(temp, mData.get(temp));
                        }
                    }
                }
                if (!key.contains("(") && !key.contains(")") && !key.contains("$")) {
                    return key;
                }
                try {
                    return TagParser.processFinal(TagParser.processMath(key));
                } catch (NumberFormatException e) {
                    return key;
                }
            }

        }
        return key;
    }

    public float parseFloat(String key){
        String temp = "";
        if(mData != null){
            if (key != null) {
                for (int i = 0; i < key.length() - 1; i++) {
                    if (key.charAt(i) == '#') {
                        for (int i2 = i + 1; i2 < key.length(); i2++) {
                            if (key.charAt(i2) == '#') {
                                temp = key.substring(i, i2 + 1);
                                break;
                            }
                        }
                        if (mData.containsKey(temp)) {
                            key = key.replace(temp, mData.get(temp));
                        }
                    }
                }
                if (!key.contains("(") && !key.contains(")") && !key.contains("$")) {
                    try {
                        return Float.parseFloat(key);
                    } catch (NumberFormatException e){
                       return 0f;
                    }
                }
                try {
                    return Float.parseFloat(TagParser.processFinal(TagParser.processMath(key)));
                } catch (NumberFormatException e) {
                    return 0f;
                }
            }

        }
        return 0f;
    }
}
