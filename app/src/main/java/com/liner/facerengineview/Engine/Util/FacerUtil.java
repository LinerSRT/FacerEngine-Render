package com.liner.facerengineview.Engine.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FacerUtil {
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_LINE = 3;
    public static final int SHAPE_POLYGON = 2;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_TRIANGLE = 4;


    public static File getFontDirectory() {
        File tempFile = new File(Environment.getExternalStorageDirectory(), "/Facer2/fonts/");
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        return tempFile;
    }


    public static Path calculatePolygonPoints(float scale, int sides, int radius, int mX, int mY, float mMultiplyFactor) {
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

    public static String read(File mFile) {
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

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (width > reqWidth) {
            while ((width / 2) / inSampleSize > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmap(String file, int reqWidth) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);
        if (options.outHeight > reqWidth * 2 && options.outWidth > reqWidth * 2) {
            options.inSampleSize = calculateInSampleSize(options, reqWidth);
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }

}
