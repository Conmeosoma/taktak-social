package com.taktak.app.ui.qr;

import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

public class QrLuminanceTest {
    private ByteBuffer padded() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{99, 1,0,2,0,3,0,99,99,4,0,5,0,6});
        buffer.position(1); return buffer;
    }
    @Test public void stridePaddingAndBufferPosition() {
        ByteBuffer input = padded();
        var frame = QrLuminance.unpack(input, 3, 2, 8, 2, 0);
        assertArrayEquals(new byte[]{1,2,3,4,5,6}, frame.data());
        assertEquals(1, input.position()); assertEquals(3, frame.width()); assertEquals(2, frame.height());
    }
    @Test public void allRotationsOfNonSquareFrame() {
        assertArrayEquals(new byte[]{4,1,5,2,6,3}, QrLuminance.unpack(padded(),3,2,8,2,90).data());
        assertArrayEquals(new byte[]{6,5,4,3,2,1}, QrLuminance.unpack(padded(),3,2,8,2,180).data());
        var frame = QrLuminance.unpack(padded(),3,2,8,2,270);
        assertArrayEquals(new byte[]{3,6,2,5,1,4}, frame.data()); assertEquals(2,frame.width()); assertEquals(3,frame.height());
    }
    @Test public void rejectsTruncatedOrInvalidPlanes() {
        assertThrows(IllegalArgumentException.class, () -> QrLuminance.unpack(ByteBuffer.allocate(2),3,2,3,1,0));
        assertThrows(IllegalArgumentException.class, () -> QrLuminance.unpack(padded(),3,2,2,2,0));
        assertThrows(IllegalArgumentException.class, () -> QrLuminance.unpack(padded(),3,2,8,2,45));
    }
}
