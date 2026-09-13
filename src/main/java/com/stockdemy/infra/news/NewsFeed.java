package com.stockdemy.infra.news;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 수집 대상 언론사 RSS
 *
 * <p>증권·경제·산업 섹션만 둔다. 종합 섹션은 스포츠·연예 기사가 섞여 종목명 오탐을 만들어 제외했다.
 * 언론사명은 피드에서 확정되므로 기사 URL로 역산하지 않는다. 막히거나 형식이 바뀐 피드는 건너뛰고
 * 로그를 남긴다.
 */
@Getter
@RequiredArgsConstructor
public enum NewsFeed {

  HANKYUNG_FINANCE("한국경제", "https://www.hankyung.com/feed/finance"),
  HANKYUNG_ECONOMY("한국경제", "https://www.hankyung.com/feed/economy"),
  HANKYUNG_IT("한국경제", "https://www.hankyung.com/feed/it"),
  MK_STOCK("매일경제", "https://www.mk.co.kr/rss/30000001/"),
  SEDAILY_MARKET("서울경제", "https://www.sedaily.com/rss/market"),
  SEDAILY_ECONOMY("서울경제", "https://www.sedaily.com/rss/economy"),
  SEDAILY_BUSINESS("서울경제", "https://www.sedaily.com/rss/business"),
  HERALD_STOCK("헤럴드경제", "https://biz.heraldcorp.com/rss/google/stock"),
  HERALD_INDUSTRY("헤럴드경제", "https://biz.heraldcorp.com/rss/google/industry"),
  HERALD_ECONOMY("헤럴드경제", "https://biz.heraldcorp.com/rss/google/economy"),
  ASIAE_STOCK("아시아경제", "https://www.asiae.co.kr/rss/stock.htm"),
  FNNEWS_STOCK("파이낸셜뉴스", "https://www.fnnews.com/rss/r20/fn_realnews_stock.xml"),
  NEWSIS_ECONOMY("뉴시스", "https://www.newsis.com/RSS/economy.xml"),
  YNA_ECONOMY("연합뉴스", "https://www.yna.co.kr/rss/economy.xml");

  private final String pressName;
  private final String url;
}
