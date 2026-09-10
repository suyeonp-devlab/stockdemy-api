package com.stockdemy.infra.marketdata.dto;

import java.util.List;

/** 당일 시세와 분봉 (Yahoo 한 번 호출로 함께 받는다) */
public record IntradayItem(
  QuoteItem quote,
  List<BarItem> bars) {
}
