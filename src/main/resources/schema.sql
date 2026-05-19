-- Hibernate / migrasi lama mungkin membuat CHECK hanya untuk CREATE, UPDATE, DELETE.
-- Enum Java menambah APPROVE, REJECT, REVIEW, CANCEL — constraint harus dihapus.
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_type_check;
