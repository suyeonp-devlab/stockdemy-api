package com.stockdemy.domain.stock.controller;

import com.stockdemy.domain.stock.dto.FavoriteRequest;
import com.stockdemy.domain.stock.dto.MinuteBarItem;
import com.stockdemy.domain.stock.dto.PriceBarItem;
import com.stockdemy.domain.stock.dto.StockDisclosureItem;
import com.stockdemy.domain.stock.dto.StockFundamentalsResponse;
import com.stockdemy.domain.stock.dto.StockListResponse;
import com.stockdemy.domain.stock.dto.StockQuoteItem;
import com.stockdemy.domain.stock.dto.StockSearchRequest;
import com.stockdemy.domain.stock.dto.TodayQuoteResponse;
import com.stockdemy.domain.stock.service.StockDisclosureService;
import com.stockdemy.domain.stock.service.StockService;
import com.stockdemy.domain.stock.service.StockTodayService;
import com.stockdemy.global.response.ApiResponse;
import com.stockdemy.global.security.SecurityUtil;
import com.stockdemy.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "종목", description = "종목 조회/관심종목 API")
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

  private final StockService stockService;
  private final StockTodayService stockTodayService;
  private final StockDisclosureService stockDisclosureService;

  @Operation(summary = "종목 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<StockListResponse>> getStockList(@Valid @ModelAttribute StockSearchRequest request) {
    Long userId = SecurityUtil.findCurrentUserId().orElse(null);
    return ResponseEntity.ok(ApiResponse.success(stockService.getStockList(request, userId)));
  }

  @Operation(summary = "종목 실시간 시세 조회")
  @GetMapping("/quotes")
  public ResponseEntity<ApiResponse<List<StockQuoteItem>>> getStockQuotes(
    @RequestParam(required = false) List<String> codes
  ) {
    return ResponseEntity.ok(ApiResponse.success(stockService.getStockQuotes(codes)));
  }

  @Operation(summary = "공시 조회")
  @GetMapping("/disclosures")
  public ResponseEntity<ApiResponse<List<StockDisclosureItem>>> getDisclosures(
    @RequestParam(required = false) String stockCode
  ) {
    return ResponseEntity.ok(ApiResponse.success(stockDisclosureService.getDisclosures(stockCode)));
  }

  @Operation(summary = "종목 기초데이터 조회")
  @GetMapping("/{stockCode}/fundamentals")
  public ResponseEntity<ApiResponse<StockFundamentalsResponse>> getFundamentals(@PathVariable String stockCode) {
    Long userId = SecurityUtil.findCurrentUserId().orElse(null);
    return ResponseEntity.ok(ApiResponse.success(stockService.getFundamentals(stockCode, userId)));
  }

  @Operation(summary = "일봉 조회")
  @GetMapping("/{stockCode}/price-bars")
  public ResponseEntity<ApiResponse<List<PriceBarItem>>> getPriceBars(@PathVariable String stockCode) {
    return ResponseEntity.ok(ApiResponse.success(stockService.getPriceBars(stockCode)));
  }

  @Operation(summary = "당일 분봉 조회")
  @GetMapping("/{stockCode}/minute-bars")
  public ResponseEntity<ApiResponse<List<MinuteBarItem>>> getMinuteBars(@PathVariable String stockCode) {
    return ResponseEntity.ok(ApiResponse.success(stockTodayService.getMinuteBars(stockCode)));
  }

  @Operation(summary = "당일 실시간 시세 조회")
  @GetMapping("/{stockCode}/quote")
  public ResponseEntity<ApiResponse<TodayQuoteResponse>> getTodayQuote(@PathVariable String stockCode) {
    return ResponseEntity.ok(ApiResponse.success(stockTodayService.getTodayQuote(stockCode)));
  }

  @Operation(summary = "관심종목 등록/해제")
  @PutMapping("/favorite")
  public ResponseEntity<ApiResponse<Void>> updateFavorite(
    @CurrentUserId Long userId,
    @Valid @RequestBody FavoriteRequest request
  ) {
    stockService.updateFavorite(userId, request);
    String message = request.favorite() ? "관심종목에 등록되었습니다." : "관심종목에서 해제되었습니다.";
    return ResponseEntity.ok(ApiResponse.success(message, null));
  }
}
