package com.taktak.app.ui.profile;

import android.content.ContentResolver;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import com.taktak.app.ui.camera.PhotoProcessor;

public final class AvatarPhoto {
    private AvatarPhoto() {}
    public static byte[] read(ContentResolver resolver, Uri uri) throws IOException {
        byte[] bytes;
        try(var input=resolver.openInputStream(uri);var output=new ByteArrayOutputStream()) {
            if(input==null) throw new IOException("Image unavailable");
            byte[] buffer=new byte[8192]; int read;
            while((read=input.read(buffer))!=-1) { output.write(buffer,0,read); if(output.size()>16*1024*1024) throw new IOException("Image too large"); }
            bytes=output.toByteArray();
        }
        var bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth<1 || bounds.outHeight<1 || bounds.outWidth>20000 || bounds.outHeight>20000) throw new IOException("Invalid image");
        var options=new BitmapFactory.Options(); options.inSampleSize=1;
        while(Math.max(bounds.outWidth/options.inSampleSize,bounds.outHeight/options.inSampleSize)>2048) options.inSampleSize*=2;
        var bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(bitmap==null) throw new IOException("Invalid image");
        try {
            int orientation=1;
            try(var input=new ByteArrayInputStream(bytes)) {
                orientation=new androidx.exifinterface.media.ExifInterface(input).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,1);
            } catch(IOException ignored) { }
            int rotation=switch(orientation) { case 3,4 -> 180; case 5,6 -> 90; case 7,8 -> 270; default -> 0; };
            boolean mirror=orientation==2 || orientation==4 || orientation==5 || orientation==7;
            return PhotoProcessor.squareJpeg(bitmap,rotation,mirror);
        } finally { bitmap.recycle(); }
    }
}
