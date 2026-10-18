package com.taktak.app.data.push;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PushEventBusTest {

    @Before
    @After
    public void cleanup() {
        PushEventBus.clear();
    }

    @Test
    public void registerAndReceiveCreatedAndDeletedEvents() {
        List<String> created = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        PushEventBus.Listener listener = new PushEventBus.Listener() {
            @Override
            public void onMomentCreated(String momentId) {
                created.add(momentId);
            }

            @Override
            public void onMomentDeleted(String momentId) {
                deleted.add(momentId);
            }
        };

        PushEventBus.register(listener);

        PushEventBus.emitMomentCreated("moment-123");
        PushEventBus.emitMomentDeleted("moment-456");

        assertEquals(1, created.size());
        assertEquals("moment-123", created.get(0));

        assertEquals(1, deleted.size());
        assertEquals("moment-456", deleted.get(0));
    }

    @Test
    public void unregisterStopsReceivingEvents() {
        List<String> created = new ArrayList<>();

        PushEventBus.Listener listener = new PushEventBus.Listener() {
            @Override
            public void onMomentCreated(String momentId) {
                created.add(momentId);
            }

            @Override
            public void onMomentDeleted(String momentId) {}
        };

        PushEventBus.register(listener);
        PushEventBus.emitMomentCreated("m1");
        assertEquals(1, created.size());

        PushEventBus.unregister(listener);
        PushEventBus.emitMomentCreated("m2");
        assertEquals(1, created.size());
    }

    @Test
    public void nullOrBlankMomentIdDoesNotEmit() {
        List<String> events = new ArrayList<>();

        PushEventBus.register(new PushEventBus.Listener() {
            @Override
            public void onMomentCreated(String momentId) {
                events.add("created:" + momentId);
            }

            @Override
            public void onMomentDeleted(String momentId) {
                events.add("deleted:" + momentId);
            }
        });

        PushEventBus.emitMomentCreated(null);
        PushEventBus.emitMomentCreated("");
        PushEventBus.emitMomentCreated("   ");
        PushEventBus.emitMomentDeleted(null);
        PushEventBus.emitMomentDeleted("");
        PushEventBus.emitMomentDeleted("   ");

        assertTrue(events.isEmpty());
    }

    @Test
    public void exceptionInOneListenerDoesNotBreakOthers() {
        List<String> received = new ArrayList<>();

        PushEventBus.register(new PushEventBus.Listener() {
            @Override
            public void onMomentCreated(String momentId) {
                throw new RuntimeException("Faulty listener");
            }

            @Override
            public void onMomentDeleted(String momentId) {
                throw new RuntimeException("Faulty listener");
            }
        });

        PushEventBus.register(new PushEventBus.Listener() {
            @Override
            public void onMomentCreated(String momentId) {
                received.add("created:" + momentId);
            }

            @Override
            public void onMomentDeleted(String momentId) {
                received.add("deleted:" + momentId);
            }
        });

        PushEventBus.emitMomentCreated("m100");
        PushEventBus.emitMomentDeleted("m200");

        assertEquals(2, received.size());
        assertEquals("created:m100", received.get(0));
        assertEquals("deleted:m200", received.get(1));
    }
}
