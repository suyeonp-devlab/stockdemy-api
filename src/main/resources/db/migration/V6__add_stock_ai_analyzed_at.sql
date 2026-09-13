ALTER TABLE stocks ADD COLUMN ai_analyzed_at TIMESTAMP;

COMMENT ON COLUMN stocks.ai_analyzed_at IS 'AI 종목 분석 최근 시각';
