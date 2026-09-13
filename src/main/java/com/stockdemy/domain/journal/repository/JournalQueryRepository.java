package com.stockdemy.domain.journal.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockdemy.domain.journal.entity.Journal;
import com.stockdemy.domain.journal.entity.JournalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.stockdemy.domain.journal.entity.QJournal.journal;

@Repository
@RequiredArgsConstructor
public class JournalQueryRepository {

  private final JPAQueryFactory queryFactory;

  // 본인 일지 페이지 조회 (거래일시 최신순, 시각 미기록 건은 같은 날 뒤쪽)
  public List<Journal> search(Long userId, JournalStatus status, int offset, int limit) {

    return queryFactory.selectFrom(journal)
      .where(journal.userId.eq(userId), eqStatus(status))
      .orderBy(journal.tradeDate.desc(), journal.tradeTime.desc().nullsLast(), journal.journalId.desc())
      .offset(offset)
      .limit(limit)
      .fetch();
  }

  // 본인 일지 건수
  public long count(Long userId, JournalStatus status) {

    Long count = queryFactory.select(journal.count())
      .from(journal)
      .where(journal.userId.eq(userId), eqStatus(status))
      .fetchOne();

    return count == null ? 0 : count;
  }

  private BooleanExpression eqStatus(JournalStatus status) {
    return status == null ? null : journal.status.eq(status);
  }
}
