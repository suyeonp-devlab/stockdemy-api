package com.stockdemy.domain.journal.repository;

import com.stockdemy.domain.journal.entity.Journal;
import com.stockdemy.domain.journal.entity.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

  // 본인 일지 단건
  Optional<Journal> findByJournalIdAndUserId(Long journalId, Long userId);

  // 상태별 일지 (요청 접수 순, 복기 대기열 처리용)
  List<Journal> findByStatusOrderByUpdatedAtAscJournalIdAsc(JournalStatus status);

  /**
   * 복기 결과 반영
   *
   * <p>복기 처리는 외부 호출이 길어 엔티티를 트랜잭션에 묶지 않고 단건 갱신한다. 처리 도중 상태가
   * 바뀐 일지를 덮어쓰지 않도록 이전 상태를 조건으로 건다.
   */
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE Journal j SET j.status = :to, j.aiComment = :aiComment, j.updatedAt = :now "
    + "WHERE j.journalId = :journalId AND j.status = :from")
  int changeStatus(
    @Param("journalId") Long journalId,
    @Param("from") JournalStatus from,
    @Param("to") JournalStatus to,
    @Param("aiComment") String aiComment,
    @Param("now") LocalDateTime now
  );
}
