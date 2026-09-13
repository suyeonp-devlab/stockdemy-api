ALTER TABLE stocks ADD COLUMN search_keyword VARCHAR(100);

COMMENT ON COLUMN stocks.search_keyword IS '뉴스 검색어 (비어 있으면 종목명 사용)';

-- 영문 종목명으로는 한국어 뉴스가 거의 검색되지 않는 해외 종목만 한글 검색어를 둔다 (AMD는 한글 표기도 동일)
UPDATE stocks SET search_keyword = '애플' WHERE stock_code = 'AAPL';
UPDATE stocks SET search_keyword = '엔비디아' WHERE stock_code = 'NVDA';
UPDATE stocks SET search_keyword = '브로드컴' WHERE stock_code = 'AVGO';
UPDATE stocks SET search_keyword = '마이크로소프트' WHERE stock_code = 'MSFT';
UPDATE stocks SET search_keyword = '구글' WHERE stock_code = 'GOOGL';
UPDATE stocks SET search_keyword = '메타' WHERE stock_code = 'META';
UPDATE stocks SET search_keyword = '넷플릭스' WHERE stock_code = 'NFLX';
UPDATE stocks SET search_keyword = '아마존' WHERE stock_code = 'AMZN';
UPDATE stocks SET search_keyword = '테슬라' WHERE stock_code = 'TSLA';
