CREATE TABLE users (
  user_id                 BIGSERIAL PRIMARY KEY,
  email                   VARCHAR(255) NOT NULL,
  password                VARCHAR(255),
  token_version           INTEGER NOT NULL,
  status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  failed_login_attempts   INTEGER NOT NULL DEFAULT 0,
  provider                VARCHAR(20) NOT NULL,
  withdrawn_at            TIMESTAMP,
  password_changed_at     TIMESTAMP,
  last_login_at           TIMESTAMP,
  created_at              TIMESTAMP,
  updated_at              TIMESTAMP
);

CREATE UNIQUE INDEX uk_users_email ON users (email) WHERE status <> 'WITHDRAWN';

COMMENT ON TABLE users IS '사용자';
COMMENT ON COLUMN users.user_id IS '사용자 ID';
COMMENT ON COLUMN users.email IS '이메일';
COMMENT ON COLUMN users.password IS '비밀번호';
COMMENT ON COLUMN users.token_version IS '토큰 버전';
COMMENT ON COLUMN users.status IS '회원 상태';
COMMENT ON COLUMN users.failed_login_attempts IS '비밀번호 연속 실패 횟수';
COMMENT ON COLUMN users.provider IS '가입 경로';
COMMENT ON COLUMN users.withdrawn_at IS '탈퇴일시';
COMMENT ON COLUMN users.password_changed_at IS '비밀번호 변경일시';
COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 일시';
COMMENT ON COLUMN users.created_at IS '생성일시';
COMMENT ON COLUMN users.updated_at IS '수정일시';

CREATE TABLE code_groups (
  group_id     VARCHAR(50) PRIMARY KEY,
  group_name   VARCHAR(255) NOT NULL,
  group_desc   VARCHAR(255),
  enabled      BOOLEAN NOT NULL,
  created_at   TIMESTAMP,
  updated_at   TIMESTAMP
);

COMMENT ON TABLE code_groups IS '공통 코드 그룹';
COMMENT ON COLUMN code_groups.group_id IS '코드 그룹 ID';
COMMENT ON COLUMN code_groups.group_name IS '코드 그룹명';
COMMENT ON COLUMN code_groups.group_desc IS '코드 그룹 설명';
COMMENT ON COLUMN code_groups.enabled IS '사용 여부';
COMMENT ON COLUMN code_groups.created_at IS '생성일시';
COMMENT ON COLUMN code_groups.updated_at IS '수정일시';

CREATE TABLE codes (
  code_id      BIGSERIAL PRIMARY KEY,
  code_value   VARCHAR(20) NOT NULL,
  code_name    VARCHAR(255) NOT NULL,
  group_id     VARCHAR(50) NOT NULL,
  sort_order   INTEGER NOT NULL,
  enabled      BOOLEAN NOT NULL,
  param1       VARCHAR(255),
  param2       VARCHAR(255),
  param3       VARCHAR(255),
  created_at   TIMESTAMP,
  updated_at   TIMESTAMP,
  CONSTRAINT uk_codes_group_code UNIQUE (group_id, code_value)
);

CREATE INDEX idx_codes_group_id ON codes (group_id);

COMMENT ON TABLE codes IS '공통 코드';
COMMENT ON COLUMN codes.code_id IS '공통 코드 ID';
COMMENT ON COLUMN codes.code_value IS '공통 코드 값';
COMMENT ON COLUMN codes.code_name IS '공통 코드명';
COMMENT ON COLUMN codes.group_id IS '공통 코드 그룹 ID';
COMMENT ON COLUMN codes.sort_order IS '정렬 순서';
COMMENT ON COLUMN codes.enabled IS '사용 여부';
COMMENT ON COLUMN codes.param1 IS '파라미터1';
COMMENT ON COLUMN codes.param2 IS '파라미터2';
COMMENT ON COLUMN codes.param3 IS '파라미터3';
COMMENT ON COLUMN codes.created_at IS '생성일시';
COMMENT ON COLUMN codes.updated_at IS '수정일시';

CREATE TABLE stocks (
  stock_code           VARCHAR(20) PRIMARY KEY,
  stock_name           VARCHAR(255) NOT NULL,
  market               VARCHAR(20) NOT NULL,
  sector               VARCHAR(20) NOT NULL,
  sentiment            VARCHAR(20),
  prev_close           DOUBLE PRECISION,
  week52_high          DOUBLE PRECISION,
  week52_low           DOUBLE PRECISION,
  shares_outstanding   BIGINT,
  foreign_ownership    DOUBLE PRECISION,
  eps                  DOUBLE PRECISION,
  bps                  DOUBLE PRECISION,
  annual_dividend      DOUBLE PRECISION,
  sector_per           DOUBLE PRECISION,
  ai_comment           TEXT,
  last_price           DOUBLE PRECISION,
  last_change_percent  DOUBLE PRECISION,
  last_market_cap      BIGINT,
  last_volume          BIGINT,
  last_synced_at       TIMESTAMP,
  created_at           TIMESTAMP,
  updated_at           TIMESTAMP
);

COMMENT ON TABLE stocks IS '종목 정보';
COMMENT ON COLUMN stocks.stock_code IS '종목 코드';
COMMENT ON COLUMN stocks.stock_name IS '종목명';
COMMENT ON COLUMN stocks.market IS '시장 구분';
COMMENT ON COLUMN stocks.sector IS '업종';
COMMENT ON COLUMN stocks.sentiment IS 'AI 신호';
COMMENT ON COLUMN stocks.prev_close IS '전일 종가';
COMMENT ON COLUMN stocks.week52_high IS '52주 최고가';
COMMENT ON COLUMN stocks.week52_low IS '52주 최저가';
COMMENT ON COLUMN stocks.shares_outstanding IS '발행 주식 수';
COMMENT ON COLUMN stocks.foreign_ownership IS '외국인 지분율';
COMMENT ON COLUMN stocks.eps IS '주당순이익';
COMMENT ON COLUMN stocks.bps IS '주당순자산가치';
COMMENT ON COLUMN stocks.annual_dividend IS '연간 배당금';
COMMENT ON COLUMN stocks.sector_per IS '업종 평균 PER';
COMMENT ON COLUMN stocks.ai_comment IS 'AI 코멘트';
COMMENT ON COLUMN stocks.last_price IS '최근 동기화 현재가';
COMMENT ON COLUMN stocks.last_change_percent IS '최근 동기화 등락률';
COMMENT ON COLUMN stocks.last_market_cap IS '최근 동기화 시가총액';
COMMENT ON COLUMN stocks.last_volume IS '최근 동기화 거래량';
COMMENT ON COLUMN stocks.last_synced_at IS '최근 동기화 시각';
COMMENT ON COLUMN stocks.created_at IS '생성일시';
COMMENT ON COLUMN stocks.updated_at IS '수정일시';

CREATE TABLE user_favorite_stocks (
  favorite_id     BIGSERIAL PRIMARY KEY,
  user_id         BIGINT NOT NULL,
  stock_code      VARCHAR(20) NOT NULL,
  created_at      TIMESTAMP,
  updated_at      TIMESTAMP,
  CONSTRAINT uk_user_favorite_stocks_user_stock UNIQUE (user_id, stock_code)
);

CREATE INDEX idx_user_favorite_stocks_user_id ON user_favorite_stocks (user_id);

COMMENT ON TABLE user_favorite_stocks IS '사용자 관심 종목';
COMMENT ON COLUMN user_favorite_stocks.favorite_id IS '사용자 관심 종목 ID';
COMMENT ON COLUMN user_favorite_stocks.user_id IS '사용자 ID';
COMMENT ON COLUMN user_favorite_stocks.stock_code IS '종목 코드';
COMMENT ON COLUMN user_favorite_stocks.created_at IS '생성일시';
COMMENT ON COLUMN user_favorite_stocks.updated_at IS '수정일시';

CREATE TABLE price_bars (
  bar_id          BIGSERIAL PRIMARY KEY,
  stock_code      VARCHAR(20) NOT NULL,
  bar_date        DATE NOT NULL,
  open            DOUBLE PRECISION NOT NULL,
  high            DOUBLE PRECISION NOT NULL,
  low             DOUBLE PRECISION NOT NULL,
  close           DOUBLE PRECISION NOT NULL,
  volume          BIGINT NOT NULL,
  trading_value   BIGINT NOT NULL,
  CONSTRAINT uk_price_bars_stock_date UNIQUE (stock_code, bar_date)
);

CREATE INDEX idx_price_bars_stock_code ON price_bars (stock_code);

COMMENT ON TABLE price_bars IS '종목 일봉';
COMMENT ON COLUMN price_bars.bar_id IS '일봉 ID';
COMMENT ON COLUMN price_bars.stock_code IS '종목 코드';
COMMENT ON COLUMN price_bars.bar_date IS '봉 기준일';
COMMENT ON COLUMN price_bars.open IS '시가';
COMMENT ON COLUMN price_bars.high IS '고가';
COMMENT ON COLUMN price_bars.low IS '저가';
COMMENT ON COLUMN price_bars.close IS '종가';
COMMENT ON COLUMN price_bars.volume IS '거래량';
COMMENT ON COLUMN price_bars.trading_value IS '거래대금';

CREATE TABLE news (
  news_id        BIGSERIAL PRIMARY KEY,
  stock_code     VARCHAR(20) NOT NULL,
  title          VARCHAR(500) NOT NULL,
  summary        TEXT,
  published_at   TIMESTAMP NOT NULL,
  category       VARCHAR(20),
  sentiment      VARCHAR(20),
  source_url     VARCHAR(500) NOT NULL,
  source_name    VARCHAR(255),
  confidence     INTEGER,
  reasoning      TEXT,
  created_at     TIMESTAMP,
  updated_at     TIMESTAMP,
  CONSTRAINT uk_news_source_url UNIQUE (source_url)
);

CREATE INDEX idx_news_stock_code ON news (stock_code);
CREATE INDEX idx_news_published_at ON news (published_at);
CREATE INDEX idx_news_category ON news (category);

COMMENT ON TABLE news IS '뉴스';
COMMENT ON COLUMN news.news_id IS '뉴스 ID';
COMMENT ON COLUMN news.stock_code IS '종목 코드';
COMMENT ON COLUMN news.title IS '제목';
COMMENT ON COLUMN news.summary IS '요약';
COMMENT ON COLUMN news.published_at IS '발행일시';
COMMENT ON COLUMN news.category IS '카테고리';
COMMENT ON COLUMN news.sentiment IS 'AI 신호';
COMMENT ON COLUMN news.source_url IS '원문 URL';
COMMENT ON COLUMN news.source_name IS '언론사명';
COMMENT ON COLUMN news.confidence IS 'AI 신호 퍼센트';
COMMENT ON COLUMN news.reasoning IS 'AI 분석 근거';
COMMENT ON COLUMN news.created_at IS '생성일시';
COMMENT ON COLUMN news.updated_at IS '수정일시';

CREATE TABLE news_related_stocks (
  related_id   BIGSERIAL PRIMARY KEY,
  news_id      BIGINT NOT NULL,
  stock_code   VARCHAR(20) NOT NULL,
  impact       VARCHAR(20)
);

CREATE INDEX idx_news_related_stocks_news_id ON news_related_stocks (news_id);

COMMENT ON TABLE news_related_stocks IS '뉴스 관련 종목 매핑';
COMMENT ON COLUMN news_related_stocks.related_id IS '뉴스 관련 종목 매핑 ID';
COMMENT ON COLUMN news_related_stocks.news_id IS '뉴스 ID';
COMMENT ON COLUMN news_related_stocks.stock_code IS '종목 코드';
COMMENT ON COLUMN news_related_stocks.impact IS '영향';

CREATE TABLE journals (
  journal_id   BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL,
  stock_code   VARCHAR(20) NOT NULL,
  status       VARCHAR(20) NOT NULL,
  market       VARCHAR(20) NOT NULL,
  sector       VARCHAR(20) NOT NULL,
  trade_type   VARCHAR(20) NOT NULL,
  trade_date   DATE NOT NULL,
  trade_time   TIME,
  price        DOUBLE PRECISION NOT NULL,
  quantity     INTEGER NOT NULL,
  memo         TEXT,
  ai_comment   TEXT,
  created_at   TIMESTAMP,
  updated_at   TIMESTAMP
);

CREATE INDEX idx_journals_user_id ON journals (user_id);

COMMENT ON TABLE journals IS '매매일지';
COMMENT ON COLUMN journals.journal_id IS '매매일지 ID';
COMMENT ON COLUMN journals.user_id IS '사용자 ID';
COMMENT ON COLUMN journals.stock_code IS '종목 코드';
COMMENT ON COLUMN journals.status IS '매매 상태';
COMMENT ON COLUMN journals.market IS '시장 구분';
COMMENT ON COLUMN journals.sector IS '업종';
COMMENT ON COLUMN journals.trade_type IS '매매 구분';
COMMENT ON COLUMN journals.trade_date IS '매매일';
COMMENT ON COLUMN journals.trade_time IS '매매 시각';
COMMENT ON COLUMN journals.price IS '매매 단가';
COMMENT ON COLUMN journals.quantity IS '수량';
COMMENT ON COLUMN journals.memo IS '메모';
COMMENT ON COLUMN journals.ai_comment IS 'AI 코멘트';
COMMENT ON COLUMN journals.created_at IS '생성일시';
COMMENT ON COLUMN journals.updated_at IS '수정일시';
