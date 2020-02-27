package com.liner.facerengineview.Engine.Util.Parser;
import android.content.Context;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
            Log.d("LOWPARSER", "KEY = "+key);
            if (key.contains("#D")) {
                mData.put(key, TagParser.parseText(mContext, key));
            } else if (key.contains("#Z")) {
                mData.put(key, TagParser.parseHealthTAG(key));
            } else if (key.contains("#B")) {
                mData.put(key, TagParser.parseBattery(mContext,key));
            } else if (key.contains("#P")) {
                mData.put(key, TagParser.parsePhoneData(mContext,key));
            } else if (key.contains("#W")) {
                // todo produce make weather parser

            } else {
                mData.put(key, "unknown");
            }
        }
    }
    
    private void checkKeyExist(String key){
        if (!mData.containsKey(key)) {
            mData.put(key, "");
        }
    }
}
