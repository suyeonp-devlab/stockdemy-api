ALTER TABLE stocks ADD COLUMN fund_synced_at TIMESTAMP;

COMMENT ON COLUMN stocks.fund_synced_at IS '펀더멘털 최근 수집 시각';