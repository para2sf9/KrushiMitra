package com.example.ai_agri;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.*;

public class FileUtils {

    // For gallery picks — works on all Android versions, no storage permission needed
    public static File uriToFile(Context context, Uri uri) {
        try {
            // Get original bitmap to resize it (prevents timeout/server errors)
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate scaling to keep image around 1024px max dimension
            int maxDim = Math.max(options.outWidth, options.outHeight);
            int inSampleSize = 1;
            if (maxDim > 1024) {
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than 1024.
                while ((maxDim / inSampleSize) > 1024) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            
            inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) {
                // Fallback: Try reading as a simple file if content resolver fails to decode
                try {
                    File file = new File(uri.getPath());
                    if (file.exists()) {
                        bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    }
                } catch (Exception ignored) {}
            }

            if (bitmap == null) return null;

            // Final resize to exactly 1024 if still larger
            if (Math.max(bitmap.getWidth(), bitmap.getHeight()) > 1024) {
                float scale = 1024f / Math.max(bitmap.getWidth(), bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                if (scaledBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = scaledBitmap;
                }
            }

            File file = File.createTempFile("leaf_compressed_", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out); // Slightly higher quality
            out.flush();
            out.close();
            bitmap.recycle();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // For camera captures
    public static File bitmapToFile(Context context, Bitmap bitmap) {
        try {
            File file = File.createTempFile("leaf_", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}