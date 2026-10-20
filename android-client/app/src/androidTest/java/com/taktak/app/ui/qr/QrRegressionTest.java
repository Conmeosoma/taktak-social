package com.taktak.app.ui.qr;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class QrRegressionTest {
    @Test public void generatedQrAndLegacyFormatsRoundTrip() {
        String raw = QrCodeUtils.buildFriendPayload("qa_qr_alice", "00000000-0000-4000-8000-000000000022");
        assertEquals("qa_qr_alice", QrCodeUtils.parsePayload(raw).username);
        for (String text : new String[]{"@QA_QR_ALICE", "https://taktak.app/u/qa_qr_alice", "taktak://user/qa_qr_alice", "https://taktak.app/friend?u=qa_qr_alice"}) {
            assertEquals("qa_qr_alice", QrCodeUtils.parsePayload(text).username);
        }
        Bitmap bitmap = QrCodeUtils.generateQrBitmap(raw, 512, Color.WHITE, Color.BLACK);
        try { assertEquals(raw, QrCodeUtils.decodeFromBitmap(bitmap)); } finally { bitmap.recycle(); }
    }
    @Test public void paddedCameraFramesDecodeAtEveryRotation() throws Exception {
        String raw = QrCodeUtils.buildFriendPayload("qa_qr_alice", null);
        Bitmap bitmap = QrCodeUtils.generateQrBitmap(raw, 512, Color.BLACK, Color.WHITE);
        int rowStride = 512 * 2 + 16;
        var buffer = java.nio.ByteBuffer.allocate(rowStride * 512);
        for (int y=0;y<512;y++) for (int x=0;x<512;x++) buffer.put(y*rowStride+x*2,(byte)(bitmap.getPixel(x,y)&255));
        bitmap.recycle();
        for (int rotation : new int[]{0,90,180,270}) {
            var frame = QrLuminance.unpack(buffer,512,512,rowStride,2,rotation);
            var source = new com.google.zxing.PlanarYUVLuminanceSource(frame.data(),frame.width(),frame.height(),0,0,frame.width(),frame.height(),false);
            assertEquals(raw,new com.google.zxing.qrcode.QRCodeReader().decode(new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source))).getText());
        }
    }
    @Test public void galleryDecodeBoundsAndInvalidImage() throws Exception {
        var context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
        java.io.File file = new java.io.File(context.getCacheDir(), "qr-regression.png");
        Bitmap source = Bitmap.createBitmap(3072,512,Bitmap.Config.ARGB_8888); source.eraseColor(Color.WHITE);
        try (var output = new java.io.FileOutputStream(file)) { assertTrue(source.compress(Bitmap.CompressFormat.PNG,100,output)); }
        finally { source.recycle(); }
        Bitmap read = QrImageDecoder.read(context.getContentResolver(),android.net.Uri.fromFile(file));
        try { assertTrue(read.getWidth()<=2048); assertNotEquals(Bitmap.Config.HARDWARE,read.getConfig()); assertNull(QrCodeUtils.decodeFromBitmap(read)); }
        finally { read.recycle(); assertTrue(file.delete()); }
        try (var output = new java.io.FileOutputStream(file)) { output.write(new byte[]{1,2,3}); }
        try { assertThrows(java.io.IOException.class, () -> QrImageDecoder.read(context.getContentResolver(),android.net.Uri.fromFile(file))); }
        finally { assertTrue(file.delete()); }
    }
    @Test public void galleryHardwareBitmapStillDecodes() throws Exception {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 28);
        String payload = "taktak://friend?u=qa_qr_alice&id=00000000-0000-4000-8000-000000000022";
        Bitmap source = QrCodeUtils.generateQrBitmap(payload, 512, Color.BLACK, Color.WHITE);
        assertNotNull(source);
        var bytes = new java.io.ByteArrayOutputStream();
        source.compress(Bitmap.CompressFormat.PNG, 100, bytes); source.recycle();
        Bitmap hardware = ImageDecoder.decodeBitmap(ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes.toByteArray())),
                (decoder, info, encoded) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_HARDWARE));
        try { assertEquals(payload, QrCodeUtils.decodeFromBitmap(hardware)); }
        finally { hardware.recycle(); }
    }
    @Test public void spoofedHostsPathsAndMalformedIdentityAreRejected() {
        for (String value : new String[]{"taktak://friend.evil?u=alice", "https://taktak.app/friend.evil?u=alice",
                "taktak://friend?u=alice&id=not-a-uuid", "taktak://friend?u=alice&u=bob",
                "taktak://friend?u=../alice", "https://taktak.app/u/alice?u=bob"}) {
            assertNull("Invalid QR must not resolve to a friend", QrCodeUtils.parsePayload(value));
        }
    }
}
