package com.stockdemy.domain.journal.controller;

import com.stockdemy.domain.journal.dto.JournalItem;
import com.stockdemy.domain.journal.dto.JournalListResponse;
import com.stockdemy.domain.journal.dto.JournalSaveRequest;
import com.stockdemy.domain.journal.dto.JournalSearchRequest;
import com.stockdemy.domain.journal.service.JournalService;
import com.stockdemy.global.response.ApiResponse;
import com.stockdemy.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주식 일지", description = "매매일지 작성/조회와 AI 복기 요청 API")
@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

  private final JournalService journalService;

  @Operation(summary = "일지 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<JournalListResponse>> getJournalList(
    @CurrentUserId Long userId,
    @Valid @ModelAttribute JournalSearchRequest request
  ) {
    return ResponseEntity.ok(ApiResponse.success(journalService.getJournalList(userId, request)));
  }

  @Operation(summary = "일지 단건 조회")
  @GetMapping("/{journalId}")
  public ResponseEntity<ApiResponse<JournalItem>> getJournal(
    @CurrentUserId Long userId,
    @PathVariable Long journalId
  ) {
    return ResponseEntity.ok(ApiResponse.success(journalService.getJournal(userId, journalId)));
  }

  @Operation(summary = "일지 작성")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> createJournal(
    @CurrentUserId Long userId,
    @Valid @RequestBody JournalSaveRequest request
  ) {
    journalService.createJournal(userId, request);
    return ResponseEntity.ok(ApiResponse.success("일지가 저장되었습니다.", null));
  }

  @Operation(summary = "일지 수정 (분석 대기 상태만)")
  @PutMapping("/{journalId}")
  public ResponseEntity<ApiResponse<Void>> updateJournal(
    @CurrentUserId Long userId,
    @PathVariable Long journalId,
    @Valid @RequestBody JournalSaveRequest request
  ) {
    journalService.updateJournal(userId, journalId, request);
    return ResponseEntity.ok(ApiResponse.success("일지가 수정되었습니다.", null));
  }

  @Operation(summary = "AI 복기 분석 요청")
  @PostMapping("/{journalId}/ai-review")
  public ResponseEntity<ApiResponse<Void>> requestReview(
    @CurrentUserId Long userId,
    @PathVariable Long journalId
  ) {
    journalService.requestReview(userId, journalId);
    return ResponseEntity.ok(ApiResponse.success("AI 복기 분석 요청이 접수되었습니다.", null));
  }
}
