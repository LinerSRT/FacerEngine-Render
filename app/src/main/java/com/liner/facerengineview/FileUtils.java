package com.liner.facerengineview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {
    public static File getFileFromZip(File filePath, String name) {
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
    public static String getDateForFormat(String formatString) {
        if (formatString == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(java.lang.System.currentTimeMillis());
        format.setCalendar(calendar);
        return format.format(calendar.getTime());
    }
}
