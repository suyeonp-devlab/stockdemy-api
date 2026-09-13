package com.stockdemy.domain.journal.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.journal.dto.JournalItem;
import com.stockdemy.domain.journal.dto.JournalListResponse;
import com.stockdemy.domain.journal.dto.JournalSaveRequest;
import com.stockdemy.domain.journal.dto.JournalSearchRequest;
import com.stockdemy.domain.journal.entity.Journal;
import com.stockdemy.domain.journal.repository.JournalQueryRepository;
import com.stockdemy.domain.journal.repository.JournalRepository;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.stockdemy.global.util.DateTimeUtil.toCompactDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalService {

  private static final String MARKET_GROUP = "STOCK_MARKET";
  private static final String SECTOR_GROUP = "STOCK_SECTOR";
  private static final String TRADE_TYPE_GROUP = "TRADE_TYPE";
  private static final String STATUS_GROUP = "JOURNAL_STATUS";

  // 앱 작성 폼과 같은 형식 (수정 화면에 그대로 채움)
  private static final DateTimeFormatter TRADE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TRADE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final JournalRepository journalRepository;
  private final JournalQueryRepository journalQueryRepository;
  private final StockRepository stockRepository;
  private final CodeService codeService;

  // 일지 목록 조회
  public JournalListResponse getJournalList(Long userId, JournalSearchRequest request) {

    long totalCount = journalQueryRepository.count(userId, request.statusFilter());
    int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / request.pageSize()));

    List<Journal> journals = totalCount == 0 ? List.of() : journalQueryRepository.search(
      userId,
      request.statusFilter(),
      (request.page() - 1) * request.pageSize(),
      request.pageSize()
    );

    return new JournalListResponse(totalCount, totalPages, toItems(journals));
  }

  // 일지 단건 조회
  public JournalItem getJournal(Long userId, Long journalId) {
    return toItems(List.of(findOwnedJournal(userId, journalId))).getFirst();
  }

  // 일지 작성
  @Transactional
  public void createJournal(Long userId, JournalSaveRequest request) {

    Stock stock = findTrackedStock(request.stockCode());

    journalRepository.save(Journal.builder()
      .userId(userId)
      .stockCode(stock.getStockCode())
      .market(stock.getMarket())
      .sector(stock.getSector())
      .tradeType(request.tradeType())
      .tradeDate(request.tradeDate())
      .tradeTime(request.tradeTime())
      .price(request.price())
      .quantity(request.quantity())
      .memo(trimToNull(request.memo()))
      .build());
  }

  // 일지 수정 (분석 대기 상태만)
  @Transactional
  public void updateJournal(Long userId, Long journalId, JournalSaveRequest request) {

    Journal journal = findOwnedJournal(userId, journalId);

    if (!journal.isStandby()) {
      throw new CustomException(ErrorCode.INVALID_REQUEST, "분석을 요청한 일지는 수정할 수 없습니다.");
    }

    Stock stock = findTrackedStock(request.stockCode());

    journal.update(
      stock.getStockCode(),
      stock.getMarket(),
      stock.getSector(),
      request.tradeType(),
      request.tradeDate(),
      request.tradeTime(),
      request.price(),
      request.quantity(),
      trimToNull(request.memo())
    );
  }

  // AI 복기 요청 (접수만 하고 스케줄러가 처리, 하루 한도를 넘으면 한도가 풀린 뒤 처리된다)
  @Transactional
  public void requestReview(Long userId, Long journalId) {

    Journal journal = findOwnedJournal(userId, journalId);

    if (!journal.isStandby()) {
      throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 분석을 요청한 일지입니다.");
    }

    journal.requestReview();
  }

  // 본인 일지 조회 (다른 사용자 일지는 존재 여부를 드러내지 않도록 404)
  private Journal findOwnedJournal(Long userId, Long journalId) {
    return journalRepository.findByJournalIdAndUserId(journalId, userId)
      .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 일지입니다."));
  }

  private Stock findTrackedStock(String stockCode) {
    return stockRepository.findById(stockCode.trim())
      .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST, "선택할 수 없는 종목입니다."));
  }

  private List<JournalItem> toItems(List<Journal> journals) {

    if (journals.isEmpty()) {
      return List.of();
    }

    Map<String, String> stockNames = stockRepository.findAllById(journals.stream().map(Journal::getStockCode).distinct().toList())
      .stream()
      .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));

    Map<String, String> statusNames = codeService.getCodeNameMap(STATUS_GROUP);
    Map<String, String> marketNames = codeService.getCodeNameMap(MARKET_GROUP);
    Map<String, String> sectorNames = codeService.getCodeNameMap(SECTOR_GROUP);
    Map<String, String> tradeTypeNames = codeService.getCodeNameMap(TRADE_TYPE_GROUP);

    return journals.stream()
      .map(journal -> JournalItem.builder()
        .id(journal.getJournalId())
        .stockCode(journal.getStockCode())
        .stockName(stockNames.get(journal.getStockCode()))
        .status(journal.getStatus().name())
        .statusNm(statusNames.get(journal.getStatus().name()))
        .market(journal.getMarket())
        .marketNm(marketNames.get(journal.getMarket()))
        .sector(journal.getSector())
        .sectorNm(sectorNames.get(journal.getSector()))
        .tradeType(journal.getTradeType())
        .tradeTypeNm(tradeTypeNames.get(journal.getTradeType()))
        .tradeDate(journal.getTradeDate().format(TRADE_DATE_FORMAT))
        .tradeTime(journal.getTradeTime() == null ? null : journal.getTradeTime().format(TRADE_TIME_FORMAT))
        .price(journal.getPrice())
        .quantity(journal.getQuantity())
        .memo(journal.getMemo())
        .aiComment(journal.getAiComment())
        .createdAt(toCompactDateTime(journal.getCreatedAt()))
        .build())
      .toList();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
