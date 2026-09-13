package com.stockdemy.domain.journal.entity;

/** 일지 AI 복기 상태 (공통코드 JOURNAL_STATUS와 값이 같아야 한다) */
public enum JournalStatus {

  // 분석 대기 (수정 가능)
  STANDBY,

  // AI 분석 중 (복기 요청 접수, 스케줄러 처리 대기)
  PENDING,

  // AI 분석 완료
  DONE
}
