package com.stockdemy.infra.ai;

import com.stockdemy.infra.ai.dto.*;

import java.util.Optional;

public interface AiClient {

  Optional<NewsAnalysisResponse> analyzeNews(NewsAnalysisRequest request);

  Optional<StockAnalysisResponse> analyzeStock(StockAnalysisRequest request);

  Optional<String> reviewJournal(JournalReviewRequest request);
}
