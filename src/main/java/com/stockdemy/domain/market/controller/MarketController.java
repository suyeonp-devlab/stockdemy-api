package com.stockdemy.domain.market.controller;

import com.stockdemy.domain.market.dto.MarketIndexItem;
import com.stockdemy.domain.market.dto.SectorSummaryItem;
import com.stockdemy.domain.market.service.MarketService;
import com.stockdemy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "시장", description = "시장 지수/업종 API")
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

  private final MarketService marketService;

  @Operation(summary = "시장 지수 및 환율 조회")
  @GetMapping("/indices")
  public ResponseEntity<ApiResponse<List<MarketIndexItem>>> getIndices() {
    return ResponseEntity.ok(ApiResponse.success(marketService.getIndices()));
  }

  @Operation(summary = "업종별 등락 요약 조회")
  @GetMapping("/sectors")
  public ResponseEntity<ApiResponse<List<SectorSummaryItem>>> getSectorSummaries() {
    return ResponseEntity.ok(ApiResponse.success(marketService.getSectorSummaries()));
  }
}
