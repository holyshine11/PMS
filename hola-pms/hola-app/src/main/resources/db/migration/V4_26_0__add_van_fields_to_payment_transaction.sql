-- PaymentTransaction에 VAN 결제 필드 추가
ALTER TABLE rsv_payment_transaction ADD COLUMN payment_channel VARCHAR(20);
ALTER TABLE rsv_payment_transaction ADD COLUMN workstation_id BIGINT;
ALTER TABLE rsv_payment_transaction ADD COLUMN van_provider VARCHAR(20);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_auth_code VARCHAR(50);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_rrn VARCHAR(50);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_pan VARCHAR(50);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_issuer_code VARCHAR(20);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_issuer_name VARCHAR(50);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_acquirer_code VARCHAR(20);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_acquirer_name VARCHAR(50);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_terminal_id VARCHAR(50);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_sequence_no VARCHAR(30);
ALTER TABLE rsv_payment_transaction ADD COLUMN van_raw_response TEXT;
