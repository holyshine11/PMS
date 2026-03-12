-- =============================================
-- V5.7.0: 대량 테스트 예약 데이터 (672건)
-- 3 프로퍼티 × 14개월(2026-01 ~ 2027-02) × 월 16건
-- 상태: RESERVED, CHECK_IN, INHOUSE, CHECKED_OUT, CANCELED, NO_SHOW
-- 포함: 마스터/서브예약, 투숙객, 일별요금, 결제, 예치금
-- =============================================

-- 기존 예약 데이터 정리
DELETE FROM rsv_reservation_memo;
DELETE FROM rsv_payment_adjustment;
DELETE FROM rsv_reservation_payment;
DELETE FROM rsv_reservation_deposit;
DELETE FROM rsv_reservation_service;
DELETE FROM rsv_daily_charge;
DELETE FROM rsv_reservation_guest;
DELETE FROM rsv_sub_reservation;
DELETE FROM rsv_master_reservation;
DELETE FROM rsv_reservation_no_seq;

DO $$
DECLARE
    -- 프로퍼티
    v_pc TEXT[] := ARRAY['GMP','GMS','OBH'];
    -- 게스트 이름 풀 (성 20, 이름 40)
    v_sn TEXT[] := ARRAY['김','이','박','최','정','강','조','윤','장','임','한','오','서','신','권','황','안','송','류','홍'];
    v_gn TEXT[] := ARRAY['민준','서준','도윤','예준','시우','하준','주원','지호','지후','준서','현우','도현','건우','우진','선우','서진','민재','현준','연우','유준','정우','승현','지훈','승우','태양','지원','은서','수빈','서윤','하은','지유','채원','소윤','지안','예린','수아','다은','민서','가은','하윤'];
    v_ef TEXT[] := ARRAY['Minjun','Seojun','Doyun','Yejun','Siu','Hajun','Juwon','Jiho','Jihu','Junseo','Hyunwoo','Dohyun','Gunwoo','Woojin','Sunwoo','Seojin','Minjae','Hyunjun','Yeonwoo','Yujun','Jungwoo','Seunghyun','Jihun','Seungwoo','Taeyang','Jiwon','Eunseo','Subin','Seoyun','Haeun','Jiyu','Chaewon','Soyun','Jian','Yerin','Sua','Daeun','Minseo','Gaeun','Hayun'];
    v_el TEXT[] := ARRAY['Kim','Lee','Park','Choi','Jung','Kang','Cho','Yoon','Jang','Lim','Han','Oh','Seo','Shin','Kwon','Hwang','Ahn','Song','Ryu','Hong'];
    -- 상태 배열 (16개 패턴)
    v_st TEXT[] := ARRAY['RESERVED','RESERVED','RESERVED','CHECK_IN','INHOUSE','INHOUSE','CHECKED_OUT','CHECKED_OUT','CHECKED_OUT','CHECKED_OUT','CHECKED_OUT','CANCELED','CANCELED','CANCELED','NO_SHOW','NO_SHOW'];
    -- 채널/마켓/국적/요청사항
    v_ch TEXT[] := ARRAY['WALK_IN','PHONE','EMAIL','OTA_BOOKING','OTA_AGODA','OTA_EXPEDIA','WEBSITE'];
    v_mk TEXT[] := ARRAY['FIT','GRP','CORP','OTA','GOV','WEB'];
    v_na TEXT[] := ARRAY['KR','KR','KR','KR','KR','US','JP','CN','KR','KR','KR','KR','KR','TW','KR','KR'];
    v_rq TEXT[] := ARRAY['조용한 객실 요청','높은 층 희망','금연 객실 부탁','늦은 체크인 예정','트윈베드 요청','오션뷰 요청','연결 객실 희망','유아침대 필요','깃털베개 불가','생일 축하 세팅','허니문 요청','비즈니스 목적','공항 픽업','짐 보관 요청',NULL,NULL];
    v_cc TEXT[] := ARRAY['VISA','MASTERCARD','국민카드','삼성카드','현대카드','신한카드','롯데카드','AMEX'];
    -- 체크인일/숙박수/객실수 패턴
    v_ci_days INT[] := ARRAY[2,4,6,8,10,12,14,16,18,20,22,24,26,28,29,30];
    v_nights INT[] := ARRAY[1,2,1,2,2,3,1,2,3,2,1,2,3,2,2,3];
    v_nrooms INT[] := ARRAY[1,1,1,2,1,1,1,2,1,1,1,2,1,1,1,2];
    -- 변수
    pc TEXT; pid BIGINT; pi INT;
    mo INT; yr INT; mn INT; dim INT;
    ri INT; gi INT := 0;
    st TEXT; ci DATE; co DATE; nn INT; cid INT;
    rd TIMESTAMP; rno TEXT; sno TEXT; cno TEXT;
    mid BIGINT; sid BIGINT;
    rtc TEXT; rtid BIGINT;
    rac TEXT; raid BIGINT;
    mkc TEXT; mkid BIGINT;
    chc TEXT; chid BIGINT;
    bp NUMERIC; sp NUMERIC; tx NUMERIC; svc NUMERIC; dtot NUMERIC;
    rmtot NUMERIC; gtot NUMERIC;
    fid BIGINT; rnid BIGINT;
    nr INT; ridx INT;
    ds INT; cdx INT; cdt DATE; wkd INT;
    pt TEXT; otn TEXT; iota BOOLEAN;
    dm TEXT;
BEGIN
    FOR pi IN 1..3 LOOP
        pc := v_pc[pi];
        SELECT id INTO pid FROM htl_property WHERE property_code = pc;

        FOR mo IN 0..13 LOOP
            yr := 2026 + mo / 12;
            mn := 1 + mo % 12;
            dim := EXTRACT(DAY FROM (make_date(yr, mn, 1) + INTERVAL '1 month' - INTERVAL '1 day'))::INT;

            FOR ri IN 1..16 LOOP
                gi := gi + 1;
                st := v_st[ri];

                -- 체크인/체크아웃
                cid := LEAST(v_ci_days[ri], dim);
                ci := make_date(yr, mn, cid);
                nn := v_nights[ri];
                co := ci + nn;

                -- 예약일 (체크인 7~30일 전)
                rd := (ci - (7 + gi % 20))::TIMESTAMP + (10 + gi % 8) * INTERVAL '1 hour';

                -- 예약번호 시퀀스
                INSERT INTO rsv_reservation_no_seq (property_id, seq_date, last_seq)
                VALUES (pid, ci, 0)
                ON CONFLICT (property_id, seq_date) DO NOTHING;

                UPDATE rsv_reservation_no_seq SET last_seq = last_seq + 1
                WHERE property_id = pid AND seq_date = ci
                RETURNING last_seq INTO ds;

                rno := pc || TO_CHAR(ci, 'YYMMDD') || '-' || LPAD(ds::TEXT, 4, '0');
                cno := CASE pc WHEN 'GMP' THEN 'M' WHEN 'GMS' THEN 'S' WHEN 'OBH' THEN 'H' END
                       || LPAD(gi::TEXT, 7, '0');

                -- 객실타입
                rtc := CASE pc
                    WHEN 'GMP' THEN (ARRAY['STD-D','DLX-T','DLX-D','SUI-R','STD-S'])[1 + (ri-1) % 5]
                    WHEN 'GMS' THEN (ARRAY['STD-D','DLX-T','SUI-R','STD-S'])[1 + (ri-1) % 4]
                    WHEN 'OBH' THEN (ARRAY['STD-D','DLX-O','DLX-D','SUI-P','STD-S'])[1 + (ri-1) % 5]
                END;
                SELECT id INTO rtid FROM rm_room_type
                WHERE property_id = pid AND room_type_code = rtc AND deleted_at IS NULL;

                -- 레이트코드
                rac := CASE pc
                    WHEN 'GMP' THEN (ARRAY['RACK','EARLY','PKG-BF','CORP-SP'])[1 + (ri-1) % 4]
                    WHEN 'GMS' THEN (ARRAY['RACK','EARLY','PKG-BF'])[1 + (ri-1) % 3]
                    WHEN 'OBH' THEN (ARRAY['RACK','EARLY','PKG-BF','RESORT'])[1 + (ri-1) % 4]
                END;
                SELECT id INTO raid FROM rt_rate_code
                WHERE property_id = pid AND rate_code = rac AND deleted_at IS NULL;

                -- 마켓코드/채널
                mkc := v_mk[1 + (ri-1) % 6];
                SELECT id INTO mkid FROM htl_market_code WHERE property_id = pid AND market_code = mkc;
                chc := v_ch[1 + (ri-1) % 7];
                SELECT id INTO chid FROM htl_reservation_channel WHERE property_id = pid AND channel_code = chc;

                -- 프로모션
                pt := CASE rac
                    WHEN 'EARLY' THEN 'EARLY_BIRD' WHEN 'PKG-BF' THEN 'PACKAGE'
                    WHEN 'CORP-SP' THEN 'COMPANY' WHEN 'RESORT' THEN 'SEASONAL'
                    ELSE NULL
                END;
                iota := chc IN ('OTA_BOOKING','OTA_AGODA','OTA_EXPEDIA');
                IF iota AND pt IS NULL THEN pt := 'OTA'; END IF;
                IF iota THEN otn := 'OTA-' || UPPER(SUBSTR(MD5(gi::TEXT), 1, 12));
                ELSE otn := NULL;
                END IF;

                -- 기본가격 (공급가)
                bp := CASE rtc
                    WHEN 'STD-S' THEN 150000 WHEN 'STD-D' THEN 200000
                    WHEN 'DLX-T' THEN 280000 WHEN 'DLX-D' THEN 300000
                    WHEN 'DLX-O' THEN 320000 WHEN 'SUI-R' THEN 450000
                    WHEN 'SUI-P' THEN 500000 ELSE 200000
                END;
                IF pc = 'GMS' THEN bp := bp * 0.85; END IF;
                IF mn IN (7,8) THEN bp := bp * 1.15;
                ELSIF mn = 12 THEN bp := bp * 1.10;
                ELSIF mn IN (1,2) THEN bp := bp * 0.90;
                END IF;

                -- 마스터 예약 INSERT
                INSERT INTO rsv_master_reservation (
                    property_id, master_reservation_no, confirmation_no, reservation_status,
                    master_check_in, master_check_out, reservation_date,
                    guest_name_ko, guest_first_name_en, guest_last_name_en,
                    phone_country_code, phone_number, email, birth_date, gender, nationality,
                    rate_code_id, market_code_id, reservation_channel_id,
                    promotion_type, ota_reservation_no, is_ota_managed,
                    customer_request, created_at, created_by
                ) VALUES (
                    pid, rno, cno, st,
                    ci, co, rd,
                    v_sn[1+(gi-1)%20] || v_gn[1+(gi-1)%40],
                    v_ef[1+(gi-1)%40], v_el[1+(gi-1)%20],
                    '+82',
                    '010-' || LPAD((1000+gi%9000)::TEXT,4,'0') || '-' || LPAD((1000+(gi*7)%9000)::TEXT,4,'0'),
                    LOWER(v_ef[1+(gi-1)%40]) || '.' || LOWER(v_el[1+(gi-1)%20]) || gi || '@example.com',
                    make_date(1970+gi%30, 1+gi%12, 1+gi%28),
                    CASE WHEN gi%2=0 THEN 'M' ELSE 'F' END,
                    v_na[ri],
                    raid, mkid, chid,
                    pt, otn, iota,
                    v_rq[ri], rd, 'admin'
                ) RETURNING id INTO mid;

                -- 서브 예약 (1~2 객실)
                nr := v_nrooms[ri];
                rmtot := 0;

                FOR ridx IN 1..nr LOOP
                    sno := rno || '-' || ridx;

                    -- 두 번째 객실은 스위트
                    IF ridx = 2 THEN
                        rtc := CASE pc WHEN 'OBH' THEN 'SUI-P' ELSE 'SUI-R' END;
                        SELECT id INTO rtid FROM rm_room_type
                        WHERE property_id = pid AND room_type_code = rtc AND deleted_at IS NULL;
                        bp := CASE rtc WHEN 'SUI-R' THEN 450000 WHEN 'SUI-P' THEN 500000 ELSE 400000 END;
                        IF pc = 'GMS' THEN bp := bp * 0.85; END IF;
                        IF mn IN (7,8) THEN bp := bp * 1.15;
                        ELSIF mn = 12 THEN bp := bp * 1.10;
                        ELSIF mn IN (1,2) THEN bp := bp * 0.90;
                        END IF;
                    END IF;

                    -- 객실배정 (체크인/투숙/체크아웃만)
                    fid := NULL; rnid := NULL;
                    IF st IN ('CHECK_IN','INHOUSE','CHECKED_OUT') THEN
                        SELECT rtf.floor_id, rtf.room_number_id INTO fid, rnid
                        FROM rm_room_type_floor rtf
                        WHERE rtf.room_type_id = rtid
                        ORDER BY rtf.id LIMIT 1 OFFSET ((gi+ridx) % 5);
                        IF fid IS NULL THEN
                            SELECT rtf.floor_id, rtf.room_number_id INTO fid, rnid
                            FROM rm_room_type_floor rtf
                            WHERE rtf.room_type_id = rtid
                            ORDER BY rtf.id LIMIT 1;
                        END IF;
                    END IF;

                    INSERT INTO rsv_sub_reservation (
                        master_reservation_id, sub_reservation_no, room_reservation_status,
                        room_type_id, floor_id, room_number_id,
                        adults, children, check_in, check_out,
                        early_check_in, late_check_out,
                        actual_check_in_time, actual_check_out_time,
                        early_check_in_fee, late_check_out_fee,
                        sort_order, created_at, created_by
                    ) VALUES (
                        mid, sno, st,
                        rtid, fid, rnid,
                        CASE WHEN rtc = 'STD-S' THEN 1 ELSE 2 END,
                        CASE WHEN rtc LIKE 'SUI%' THEN 1 ELSE 0 END,
                        ci, co,
                        FALSE, FALSE,
                        CASE WHEN st IN ('CHECK_IN','INHOUSE','CHECKED_OUT')
                             THEN ci::TIMESTAMP + INTERVAL '15 hours' + (gi%3) * INTERVAL '30 min'
                             ELSE NULL END,
                        CASE WHEN st = 'CHECKED_OUT'
                             THEN co::TIMESTAMP + INTERVAL '10 hours' + (gi%4) * INTERVAL '30 min'
                             ELSE NULL END,
                        0, 0,
                        ridx, rd, 'admin'
                    ) RETURNING id INTO sid;

                    -- 투숙객
                    INSERT INTO rsv_reservation_guest (
                        sub_reservation_id, guest_seq, guest_name_ko,
                        guest_first_name_en, guest_last_name_en, created_at, updated_at
                    ) VALUES (
                        sid, 1,
                        v_sn[1+((gi+ridx-1)%20)] || v_gn[1+((gi+ridx*10-1)%40)],
                        v_ef[1+((gi+ridx*10-1)%40)],
                        v_el[1+((gi+ridx-1)%20)],
                        NOW(), NOW()
                    );

                    -- 일별 요금
                    FOR cdx IN 0..(nn-1) LOOP
                        cdt := ci + cdx;
                        wkd := EXTRACT(ISODOW FROM cdt)::INT;
                        sp := bp;
                        IF wkd IN (5,6) THEN sp := sp * 1.20;
                        ELSIF wkd = 7 THEN sp := sp * 1.10;
                        END IF;
                        sp := ROUND(sp, 0);
                        tx := ROUND(sp * 0.10, 0);
                        svc := ROUND(sp * 0.10, 0);
                        dtot := sp + tx + svc;
                        rmtot := rmtot + dtot;

                        INSERT INTO rsv_daily_charge (
                            sub_reservation_id, charge_date,
                            supply_price, tax, service_charge, total,
                            created_at, updated_at
                        ) VALUES (sid, cdt, sp, tx, svc, dtot, NOW(), NOW());
                    END LOOP;

                END LOOP; -- room loop

                -- 결제 정보
                gtot := rmtot;
                INSERT INTO rsv_reservation_payment (
                    master_reservation_id,
                    total_room_amount, total_service_amount, total_service_charge_amount,
                    total_adjustment_amount, total_early_late_fee, grand_total,
                    payment_status, payment_date, payment_method,
                    sort_order, created_at, created_by
                ) VALUES (
                    mid,
                    rmtot, 0, 0, 0, 0, gtot,
                    CASE WHEN st = 'CHECKED_OUT' THEN 'COMPLETED' ELSE 'PENDING' END,
                    CASE WHEN st = 'CHECKED_OUT' THEN co::TIMESTAMP + INTERVAL '11 hours' ELSE NULL END,
                    CASE WHEN st = 'CHECKED_OUT' THEN 'CREDIT_CARD' ELSE NULL END,
                    0, rd, 'admin'
                );

                -- 예치금 (모든 예약에 생성, 상태별 차등)
                dm := CASE WHEN gi % 5 = 0 THEN 'CASH' ELSE 'CREDIT_CARD' END;
                INSERT INTO rsv_reservation_deposit (
                    master_reservation_id, deposit_method,
                    card_company,
                    card_number_encrypted, card_cvc_encrypted, card_expiry_date,
                    currency, amount,
                    sort_order, created_at, created_by
                ) VALUES (
                    mid, dm,
                    CASE WHEN dm = 'CREDIT_CARD' THEN v_cc[1+gi%8] ELSE NULL END,
                    CASE WHEN dm = 'CREDIT_CARD' THEN 'ENC_' || LPAD(gi::TEXT,10,'0') ELSE NULL END,
                    CASE WHEN dm = 'CREDIT_CARD' THEN 'CVC_' || LPAD(gi::TEXT,5,'0') ELSE NULL END,
                    CASE WHEN dm = 'CREDIT_CARD' THEN LPAD((1+gi%12)::TEXT,2,'0') || '/20' || (27+gi%3)::TEXT ELSE NULL END,
                    'KRW',
                    ROUND(gtot / nn * 0.5, 0),
                    0, rd, 'admin'
                );

            END LOOP; -- reservation loop
        END LOOP; -- month loop
    END LOOP; -- property loop

    RAISE NOTICE '총 예약 생성 완료: % 건', gi;
END $$;

-- 시퀀스 리셋
SELECT setval('rsv_master_reservation_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_master_reservation));
SELECT setval('rsv_sub_reservation_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_sub_reservation));
SELECT setval('rsv_reservation_guest_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_guest));
SELECT setval('rsv_daily_charge_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_daily_charge));
SELECT setval('rsv_reservation_payment_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_payment));
SELECT setval('rsv_reservation_deposit_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_deposit));
SELECT setval('rsv_reservation_no_seq_id_seq', (SELECT COALESCE(MAX(id), 0) FROM rsv_reservation_no_seq));
