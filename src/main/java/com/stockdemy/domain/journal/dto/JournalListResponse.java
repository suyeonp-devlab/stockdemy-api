package com.stockdemy.domain.journal.dto;

import java.util.List;

public record JournalListResponse(
  long totalCount,
  int totalPages,
  List<JournalItem> items
) {
}
