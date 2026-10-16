package com.taktak.app.data.session;

import java.io.IOException;

public interface SessionStore {
    Session load() throws IOException;
    void save(Session session) throws IOException;
    void clear() throws IOException;
}
