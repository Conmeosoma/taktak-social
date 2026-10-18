package com.taktak.server.user;

import com.taktak.server.auth.AuthFailure;
import com.taktak.server.moment.MomentFailure;
import com.taktak.server.storage.PrivateImageStorage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarService {
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PrivateImageStorage storage;
    public AvatarService(UserRepository users, JdbcTemplate jdbc, @Value("${taktak.storage.root}") String root) {
        this.users=users; this.jdbc=jdbc; storage=new PrivateImageStorage(java.nio.file.Path.of(root,"avatars").toString());
    }
    @Transactional
    public UserProfileResponse upload(UUID actor, MultipartFile image) throws IOException {
        // Chung khóa user với thao tác bạn bè; cập nhật đồng thời không làm mất reference.
        var locked=users.lockAll(List.of(actor));
        if(locked.isEmpty()) throw new AuthFailure();
        var user=locked.get(0);
        byte[] sanitized=storage.sanitize(image);
        var source=ImageIO.read(new ByteArrayInputStream(sanitized));
        var square=new java.awt.image.BufferedImage(512,512,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=square.createGraphics();
        try {
            int side=Math.min(source.getWidth(),source.getHeight());
            int x=(source.getWidth()-side)/2,y=(source.getHeight()-side)/2;
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source,0,0,512,512,x,y,x+side,y+side,null);
        } finally { graphics.dispose(); source.flush(); }
        byte[] bytes;
        try(var output=new ByteArrayOutputStream()) { ImageIO.write(square,"jpeg",output); bytes=output.toByteArray(); }
        finally { square.flush(); }
        String next=storage.write(UUID.randomUUID(),bytes),previous=user.getAvatarPath();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) storage.discard(next); }
            @Override public void afterCommit() { if(previous!=null) storage.discard(previous); }
        });
        user.updateAvatar(next); users.saveAndFlush(user);
        return UserProfileResponse.from(user);
    }
    @Transactional(readOnly=true)
    public byte[] read(UUID actor, UUID target) throws IOException {
        if(!users.existsById(actor)) throw new AuthFailure();
        if(!actor.equals(target)) {
            Integer allowed=jdbc.queryForObject("""
                SELECT count(*) FROM friendships f WHERE
                ((f.user_low_id=? AND f.user_high_id=?) OR (f.user_low_id=? AND f.user_high_id=?))
                AND NOT EXISTS (SELECT 1 FROM blocks b WHERE
                (b.blocker_id=? AND b.blocked_id=?) OR (b.blocker_id=? AND b.blocked_id=?))
                """,Integer.class,actor,target,target,actor,actor,target,target,actor);
            if(allowed==null || allowed==0) throw new MomentFailure(HttpStatus.NOT_FOUND,"AVATAR_NOT_FOUND");
        }
        var user=users.findById(target).orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND,"AVATAR_NOT_FOUND"));
        return user.getAvatarPath()==null ? null : storage.read(user.getAvatarPath());
    }
}
