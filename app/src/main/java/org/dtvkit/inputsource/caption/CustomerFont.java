package org.dtvkit.inputsource.caption;

import android.content.Context;

import androidx.annotation.NonNull;

import org.droidlogic.dtvkit.DtvkitGlueClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CustomerFont {
    public static void initFontInTread(@NonNull Context context) {
        new Thread(() -> {
            String unCryptDirStr = "font";
            File unCryptDir = new File(context.getDataDir(), unCryptDirStr);
            File[] lists = unCryptDir.listFiles();
            if (lists == null || lists.length == 0) {
                unzipUnCrypt(context, unCryptDir);
            }
        }).start();
    }

    private static void unzipUnCrypt(@NonNull Context context, File unCryptDir) {
        File zipFile = new File(context.getDataDir(), "fonts.zip");
        String unZipDirStr = "vendorfont";
        File unZipDir = new File(context.getDataDir(), unZipDirStr);
        try {
            String fonts = "font";
            copyToFileOrThrow(context.getAssets().open(fonts), zipFile);
            upZipFile(zipFile, unZipDir.getCanonicalPath());
            if (unCryptDir.mkdirs()) {
                DtvkitGlueClient.getInstance().doUnCrypt(unZipDir.getCanonicalPath() + "/",
                        unCryptDir.getCanonicalPath() + "/");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void copyToFileOrThrow(InputStream inputStream, File destFile)
            throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }
        FileOutputStream out = new FileOutputStream(destFile);
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            out.flush();
            try {
                out.getFD().sync();
            } catch (IOException ignored) {
            }
            out.close();
        }
    }

    private static void upZipFile(File zipFile, String folderPath) throws IOException {
        File desDir = new File(folderPath);
        if (!desDir.exists()) {
            desDir.mkdirs();
        }
        ZipFile zf = new ZipFile(zipFile);
        for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = ((ZipEntry) entries.nextElement());
            InputStream in = zf.getInputStream(entry);
            File desFile = new File(folderPath, java.net.URLEncoder.encode(
                    entry.getName(), "UTF-8"));
            if (!desFile.exists()) {
                File fileParentDir = desFile.getParentFile();
                if (fileParentDir != null && !fileParentDir.exists()) {
                    fileParentDir.mkdirs();
                }
            }
            OutputStream out = new FileOutputStream(desFile);
            byte[] buffer = new byte[1024 * 1024];
            int realLength = in.read(buffer);
            while (realLength != -1) {
                out.write(buffer, 0, realLength);
                realLength = in.read(buffer);
            }
            out.close();
            in.close();
        }
    }
}
