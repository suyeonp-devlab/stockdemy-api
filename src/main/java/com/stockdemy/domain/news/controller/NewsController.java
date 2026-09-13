package com.stockdemy.domain.news.controller;

import com.stockdemy.domain.news.dto.MentionRankingItem;
import com.stockdemy.domain.news.dto.NewsDetailResponse;
import com.stockdemy.domain.news.dto.NewsListResponse;
import com.stockdemy.domain.news.dto.NewsSearchRequest;
import com.stockdemy.domain.news.dto.SentimentSummaryResponse;
import com.stockdemy.domain.news.service.NewsService;
import com.stockdemy.global.response.ApiResponse;
import com.stockdemy.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "뉴스", description = "뉴스 조회 API")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

  private final NewsService newsService;

  @Operation(summary = "뉴스 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<NewsListResponse>> getNewsList(@Valid @ModelAttribute NewsSearchRequest request) {
    Long userId = SecurityUtil.findCurrentUserId().orElse(null);
    return ResponseEntity.ok(ApiResponse.success(newsService.getNewsList(request, userId)));
  }

  @Operation(summary = "시장 평가 조회")
  @GetMapping("/sentiment-summary")
  public ResponseEntity<ApiResponse<SentimentSummaryResponse>> getSentimentSummary() {
    return ResponseEntity.ok(ApiResponse.success(newsService.getSentimentSummary()));
  }

  @Operation(summary = "많이 언급된 종목 조회")
  @GetMapping("/top-mentions")
  public ResponseEntity<ApiResponse<List<MentionRankingItem>>> getTopMentions() {
    return ResponseEntity.ok(ApiResponse.success(newsService.getTopMentions()));
  }

  @Operation(summary = "뉴스 상세 조회")
  @GetMapping("/{newsId}")
  public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsDetail(@PathVariable Long newsId) {
    return ResponseEntity.ok(ApiResponse.success(newsService.getNewsDetail(newsId)));
  }
}
