-- Local demo data only (classpath:db/seed, local profile).
-- Login: username demo / password password (BCrypt hash stored below).

INSERT INTO merchant (id, name, currency) VALUES
    (1001, 'Example Merchant', 'GBP'),
    (1002, 'Northwind Retail', 'GBP'),
    (1003, 'Unlinked Merchant', 'GBP');

INSERT INTO app_user (username, password_hash) VALUES
    ('demo', '$2a$10$3eopnnUCTm2MPvnu6SC65OSr9Bv1cHgADLJAFerXjfYOiTY/a4Jj6');

INSERT INTO user_merchant (user_id, merchant_id)
SELECT id, 1001 FROM app_user WHERE username = 'demo'
UNION ALL
SELECT id, 1002 FROM app_user WHERE username = 'demo';

-- ~8 sales per day over 180 days for Example Merchant (~1,440 rows).
INSERT INTO sale_transaction (merchant_id, transaction_date, sale_amount, status)
SELECT
    1001,
    (d + (n || ' hours')::INTERVAL + ((n * 7) || ' minutes')::INTERVAL) AT TIME ZONE 'UTC',
    ROUND((8 + (EXTRACT(DOY FROM d)::INTEGER % 90) + n * 4.75 + (EXTRACT(DAY FROM d)::INTEGER % 5) * 2.1)::NUMERIC, 4),
    CASE
        WHEN (EXTRACT(DAY FROM d)::INTEGER + n) % 13 = 0 THEN 'DECLINED'
        WHEN (EXTRACT(DAY FROM d)::INTEGER + n) % 6 = 0 THEN 'PENDING'
        ELSE 'APPROVED'
    END
FROM generate_series(CURRENT_DATE - 180, CURRENT_DATE - 1, INTERVAL '1 day') AS d
CROSS JOIN generate_series(0, 7) AS n;

-- Extra pending commissions for the to-do counter on Example Merchant.
INSERT INTO sale_transaction (merchant_id, transaction_date, sale_amount, status)
SELECT
    1001,
    CURRENT_TIMESTAMP - (n || ' hours')::INTERVAL,
    ROUND((5 + n * 3.5)::NUMERIC, 4),
    'PENDING'
FROM generate_series(1, 25) AS n;

-- ~3 sales per day over 90 days for Northwind.
INSERT INTO sale_transaction (merchant_id, transaction_date, sale_amount, status)
SELECT
    1002,
    (d + (n || ' hours')::INTERVAL) AT TIME ZONE 'UTC',
    ROUND((12 + (EXTRACT(DOY FROM d)::INTEGER % 40) + n * 6.25)::NUMERIC, 4),
    CASE
        WHEN (EXTRACT(DAY FROM d)::INTEGER + n) % 9 = 0 THEN 'DECLINED'
        WHEN (EXTRACT(DAY FROM d)::INTEGER + n) % 4 = 0 THEN 'PENDING'
        ELSE 'APPROVED'
    END
FROM generate_series(CURRENT_DATE - 90, CURRENT_DATE - 1, INTERVAL '1 day') AS d
CROSS JOIN generate_series(1, 3) AS n;

-- Daily clicks for the last 90 days (missing today is fine; windows end yesterday).
INSERT INTO click_daily (merchant_id, click_date, click_count)
SELECT
    1001,
    d::DATE,
    40 + (EXTRACT(DOY FROM d)::INTEGER % 120)
FROM generate_series(CURRENT_DATE - 90, CURRENT_DATE - 1, INTERVAL '1 day') AS d;

INSERT INTO click_daily (merchant_id, click_date, click_count)
SELECT
    1002,
    d::DATE,
    15 + (EXTRACT(DOY FROM d)::INTEGER % 50)
FROM generate_series(CURRENT_DATE - 90, CURRENT_DATE - 1, INTERVAL '1 day') AS d;

-- Expired ACTIVE offers feed "remove expired offers" (plus some live / removed).
INSERT INTO offer (merchant_id, expires_at, status)
SELECT
    1001,
    CURRENT_TIMESTAMP - (n || ' days')::INTERVAL,
    'ACTIVE'
FROM generate_series(1, 45) AS n;

INSERT INTO offer (merchant_id, expires_at, status)
SELECT
    1001,
    CURRENT_TIMESTAMP + (n || ' days')::INTERVAL,
    'ACTIVE'
FROM generate_series(1, 20) AS n;

INSERT INTO offer (merchant_id, expires_at, status)
SELECT
    1001,
    CURRENT_TIMESTAMP - (n || ' days')::INTERVAL,
    'REMOVED'
FROM generate_series(1, 15) AS n;

INSERT INTO offer (merchant_id, expires_at, status)
SELECT
    1002,
    CURRENT_TIMESTAMP - (n || ' days')::INTERVAL,
    'ACTIVE'
FROM generate_series(1, 12) AS n;

INSERT INTO offer (merchant_id, expires_at, status)
SELECT
    1002,
    CURRENT_TIMESTAMP + (n || ' days')::INTERVAL,
    'ACTIVE'
FROM generate_series(1, 8) AS n;

-- Unmapped categories feed "map product categories".
INSERT INTO product_category (merchant_id, mapped)
SELECT 1001, FALSE
FROM generate_series(1, 85) AS n;

INSERT INTO product_category (merchant_id, mapped)
SELECT 1001, TRUE
FROM generate_series(1, 20) AS n;

INSERT INTO product_category (merchant_id, mapped)
SELECT 1002, FALSE
FROM generate_series(1, 30) AS n;

INSERT INTO product_category (merchant_id, mapped)
SELECT 1002, TRUE
FROM generate_series(1, 10) AS n;

-- Pending applications feed "review applications".
INSERT INTO affiliate_application (merchant_id, status)
SELECT 1001, 'PENDING'
FROM generate_series(1, 40) AS n;

INSERT INTO affiliate_application (merchant_id, status)
SELECT 1001, 'REVIEWED'
FROM generate_series(1, 15) AS n;

INSERT INTO affiliate_application (merchant_id, status)
SELECT 1002, 'PENDING'
FROM generate_series(1, 12) AS n;

INSERT INTO affiliate_application (merchant_id, status)
SELECT 1002, 'REVIEWED'
FROM generate_series(1, 5) AS n;
