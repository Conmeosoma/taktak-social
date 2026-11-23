package com.taktak.app.ui.theme;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.outbox.*;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Hai lượt instrumentation/process riêng; chỉ tạo và xóa đúng record QA sở hữu. */
@RunWith(AndroidJUnit4.class)
public class OutboxProcessReloadTest {
    @Test public void checkpointAndIdempotencySurviveProcessRestart() throws Exception {
        var inst=InstrumentationRegistry.getInstrumentation();
        var context=inst.getTargetContext();
        var prefs=context.getSharedPreferences("qa_outbox_process_reload",Context.MODE_PRIVATE);
        String phase=InstrumentationRegistry.getArguments().getString("outboxPhase");
        assertTrue("Run explicit write or read phase", "write".equals(phase)||"read".equals(phase));
        try(var db=new OutboxDatabaseHelper(context)) {
            if("write".equals(phase)) {
                assertFalse("Do not overwrite unfinished QA record",prefs.contains("id"));
                String id=UUID.randomUUID().toString(),key=UUID.randomUUID().toString();
                var record=new OutboxRecord(id,"qa-process-owner",987654321L,key,"PHOTO","qa-no-payload",null,
                        "QA",List.of(),null,System.currentTimeMillis(),OutboxStatus.PENDING,0,0,null,0);
                db.insert(record);
                assertTrue(db.saveCreatedMomentId(id,987654321L,"qa-checkpoint"));
                assertTrue(prefs.edit().putString("id",id).putString("key",key).commit());
            } else {
                String id=prefs.getString("id",null);assertNotNull("Write phase must run first",id);
                try {
                    var record=db.get(id);assertNotNull(record);
                    assertEquals(prefs.getString("key",null),record.idempotencyKey);
                    assertEquals("qa-process-owner",record.ownerUserId);
                    assertEquals("qa-checkpoint",db.createdMomentId(id,987654321L));
                } finally {db.delete(id);assertTrue(context.deleteSharedPreferences("qa_outbox_process_reload"));}
            }
        }
    }
}
