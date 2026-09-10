package com.stockdemy.domain.stock.controller;

import com.stockdemy.domain.stock.dto.FavoriteRequest;
import com.stockdemy.domain.stock.dto.StockListResponse;
import com.stockdemy.domain.stock.dto.StockQuoteItem;
import com.stockdemy.domain.stock.dto.StockSearchRequest;
import com.stockdemy.domain.stock.service.StockService;
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
