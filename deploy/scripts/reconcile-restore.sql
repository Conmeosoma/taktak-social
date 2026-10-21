-- Cung transaction voi import restore_security_before; writers phai dung.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users u LEFT JOIN restore_security_before b ON b.id=u.id WHERE b.id IS NULL) THEN
        RAISE EXCEPTION 'Restore requires separate recovery plan for unknown accounts';
    END IF;
    IF EXISTS (SELECT 1 FROM users u JOIN restore_security_before b ON b.id=u.id
               WHERE greatest(u.security_version,b.security_version)=9223372036854775807) THEN
        RAISE EXCEPTION 'Session version exhausted; restore refused';
    END IF;
END $$;
-- Giu credential/identity moi nhat; khong hoi sinh mat khau truoc reset.
UPDATE users u SET security_version=greatest(u.security_version,b.security_version)+1,
    password_hash=b.password_hash, email=b.email, username=b.username,
    email_verified_at=b.email_verified_at
FROM restore_security_before b WHERE b.id=u.id;
-- Revoke ca refresh dang active khi backup, bo moi recovery challenge cu.
UPDATE refresh_tokens SET revoked_at=coalesce(revoked_at,CURRENT_TIMESTAMP);
DELETE FROM account_recovery;
