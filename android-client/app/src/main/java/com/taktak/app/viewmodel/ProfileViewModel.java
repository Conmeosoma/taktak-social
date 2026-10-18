package com.taktak.app.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.*;
import androidx.lifecycle.*;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.ProfileRepository;
import com.taktak.app.ui.profile.AvatarPhoto;
import java.io.IOException;
import java.util.concurrent.*;

public final class ProfileViewModel extends AndroidViewModel {
    public static final class State {
        public final UserProfile profile;
        public final byte[] avatar;
        public final boolean busy,expired;
        public final int message;
        State(UserProfile profile,byte[] avatar,boolean busy,boolean expired,int message) {
            this.profile=profile;this.avatar=avatar;this.busy=busy;this.expired=expired;this.message=message;
        }
    }
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(null,null,false,false,0));
    private final SavedStateHandle saved;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean cleared;
    private long ticket;
    private ProfileRepository repository;
    private final PushEventBus.Listener listener=new PushEventBus.Listener() {
        public void onMomentCreated(String id) {}
        public void onMomentDeleted(String id) {}
        public void onSessionCleared() { main.post(() -> {
            if(cleared || repository!=null && repository.publishIfCurrent(() -> {})) return;
            ticket++; saved.remove("profileName"); state.setValue(new State(null,null,false,true,0));
        }); }
    };
    public ProfileViewModel(Application app,SavedStateHandle saved) { super(app);this.saved=saved;PushEventBus.register(listener); }
    public LiveData<State> state() { return state; }
    public String draft() { return saved.get("profileName"); }
    public void draft(String name) { saved.set("profileName",name); }
    public void load() { run(ProfileRepository::load,false,0); }
    public void save() {
        String name=draft();
        if(name==null || name.strip().isEmpty() || name.strip().length()>255) {
            var old=state.getValue();state.setValue(new State(old.profile,old.avatar,false,false,R.string.profile_invalid_name));return;
        }
        run(repo -> repo.update(name),true,R.string.profile_saved);
    }
    public void upload(Uri uri) {
        run(repo -> repo.upload(AvatarPhoto.read(getApplication().getContentResolver(),uri)),false,R.string.profile_avatar_saved);
    }
    public boolean beginAvatarSelection() {
        try { saved.set("avatarSelectionGeneration",AuthGraph.repository(getApplication()).sessionGeneration());return true; }
        catch(IOException failure) { expire();return false; }
    }
    public void uploadSelection(Uri uri) {
        Long generation=saved.get("avatarSelectionGeneration");saved.remove("avatarSelectionGeneration");
        try {
            if(uri!=null && generation!=null && AuthGraph.repository(getApplication()).sessionGeneration()==generation) upload(uri);
        } catch(IOException ignored) { /* Không chuyển ảnh picker sang phiên khác. */ }
    }
    private interface Operation { UserProfile execute(ProfileRepository repo) throws IOException; }
    private void run(Operation operation,boolean resetName,int success) {
        var old=state.getValue(); if(cleared || old.busy) return;
        try { repository=AuthGraph.repository(getApplication()).profiles(); }
        catch(IOException failure) { state.setValue(new State(null,null,false,false,R.string.profile_error));return; }
        var producer=repository; long request=++ticket;
        state.setValue(new State(old.profile,old.avatar,true,false,0));
        worker.execute(() -> {
            try {
                var profile=operation.execute(producer); byte[] avatar=producer.avatar(profile.id);
                main.post(() -> {
                    if(cleared || request!=ticket) return;
                    if(!producer.publishIfCurrent(() -> {
                        if(resetName || draft()==null) draft(profile.displayName);
                        state.setValue(new State(profile,avatar,false,false,success));
                    })) expire();
                });
            } catch(Exception failure) {
                main.post(() -> {
                    if(cleared || request!=ticket) return;
                    if(!producer.publishIfCurrent(() -> {})) { expire();return; }
                    boolean expired=failure instanceof ApiFailure api && (api.kind==ApiFailure.Kind.UNAUTHORIZED || api.kind==ApiFailure.Kind.CANCELLED);
                    int message=failure instanceof ApiFailure api && api.kind==ApiFailure.Kind.VALIDATION
                            ? R.string.profile_invalid_name : failure instanceof IOException ? R.string.profile_network_error : R.string.profile_error;
                    if(expired) expire(); else state.setValue(new State(old.profile,old.avatar,false,false,message));
                });
            }
        });
    }
    private void expire() { saved.remove("profileName");state.setValue(new State(null,null,false,true,0)); }
    @Override protected void onCleared() { cleared=true; ticket++; PushEventBus.unregister(listener);main.removeCallbacksAndMessages(null);worker.shutdownNow(); }
}
