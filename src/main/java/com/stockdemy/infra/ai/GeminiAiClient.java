package com.stockdemy.infra.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.stockdemy.infra.ai.dto.*;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class GeminiAiClient implements AiClient {

  private static final String BASE_URL = "https://generativelanguage.googleapis.com";
  private static final String ENDPOINT_PATH = "/v1beta/models/{model}:generateContent?key={key}";

  private final String apiKey;
  private final String model;
  private final ObjectMapper objectMapper;
  private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

  // AI 뉴스 분석
  @Override
  public Optional<NewsAnalysisResponse> analyzeNews(NewsAnalysisRequest request) {

    if (StringUtils.isEmpty(apiKey)) {
      log.warn("GEMINI_API_KEY가 설정되지 않아 뉴스 AI 분석을 건너뜁니다");
      return Optional.empty();
    }

    try {
      String responseText = callGemini(buildNewsPrompt(request));
      return Optional.of(objectMapper.readValue(extractJson(responseText), NewsAnalysisResponse.class));
    } catch (Exception e) {
      log.error("Gemini 뉴스 분석 실패: {}", e.getMessage());
      return Optional.empty();
    }
  }

  // AI 종목 분석
  @Override
  public Optional<StockAnalysisResponse> analyzeStock(StockAnalysisRequest request) {

    if (StringUtils.isEmpty(apiKey)) {
      log.warn("GEMINI_API_KEY가 설정되지 않아 종목 AI 분석을 건너뜁니다");
      return Optional.empty();
    }

    try {
      String responseText = callGemini(buildStockPrompt(request));
      return Optional.of(objectMapper.readValue(extractJson(responseText), StockAnalysisResponse.class));
    } catch (Exception e) {
      log.error("Gemini 종목 분석 실패: {}", e.getMessage());
      return Optional.empty();
    }
  }

  // AI 일지 복기
  @Override
  public Optional<String> reviewJournal(JournalReviewRequest request) {

    if (StringUtils.isEmpty(apiKey)) {
      log.warn("GEMINI_API_KEY가 설정되지 않아 일지 AI 복기를 건너뜁니다");
      return Optional.empty();
    }

    try {
      String aiComment = callGemini(buildJournalPrompt(request)).trim();
      return Optional.of(aiComment);
    } catch (Exception e) {
      log.error("Gemini 일지 복기 실패: {}", e.getMessage());
      return Optional.empty();
    }
  }

  // AI 뉴스 분석 프롬프트
  private String buildNewsPrompt(NewsAnalysisRequest request) {

    return """
                당신은 한국 주식 시장 뉴스 분석가입니다. 아래 뉴스 정보와 원본 URL을 분석해서 JSON으로만 답하세요. JSON 앞뒤로 다른 설명은 절대 붙이지 마세요.

                종목 코드: %s
                뉴스 제목: %s
                뉴스 원본: %s
                관련 가능 종목 코드 후보: %s

                다음 JSON 형식으로만 답하세요.
                {
                  "summary": "뉴스 내용을 최대 10문장 이내로 자체 요약",
                  "sentiment": "POSITIVE 또는 NEUTRAL 또는 NEGATIVE 중 하나",
                  "confidence": 0에서 100 사이 정수,
                  "reasoning": "그렇게 판단한 근거를 5문장 이내로",
                  "relatedStocks": [{"stockCode": "후보 중 실제 연관있는 종목코드", "impact": "BENEFIT 또는 LIMITED 또는 ADVERSE"}]
                }
           
                confidence는 sentiment를 판단한 확신 수준을 의미합니다.
                relatedStocks의 impact는 동반 수혜(BENEFIT), 제한적 영향(LIMITED), 동반 약세(ADVERSE) 중 하나입니다.
                relatedStocks는 실제로 뉴스와 연관성이 뚜렷한 경우에만 최대 4개까지 포함하고, 없으면 빈 배열로 답하세요.
           """.formatted(
             request.stockCode(), request.title(), request.sourceUrl(),
             String.join(", ", request.candidateStockCode())
           );
  }

  // AI 종목 분석 프롬프트
  private String buildStockPrompt(StockAnalysisRequest request) {

    return """
                당신은 한국 주식 시장 분석가입니다. 아래 종목의 펀더멘털/최근 등락 지표와 최신 뉴스 원본 URL을 분석해서 JSON으로만 답하세요.
                최신 뉴스 원본 URL이 존재않지 않는 경우, 주어진 수치로만 판단하세요. JSON 앞뒤로 다른 설명은 절대 붙이지 마세요.

                종목 코드: %s
                시장: %s
                업종: %s
                전일종가: %s
                52주 최고가: %s
                52주 최저가: %s
                발행 주식 수: %s
                EPS: %s
                BPS: %s
                업종평균 PER: %s
                현재가: %s
                등락률: %s
                거래량: %s
                최신 뉴스 원본: %s
           
                다음 JSON 형식으로만 답하세요.
                {
                  "sentiment": "POSITIVE 또는 NEUTRAL 또는 NEGATIVE 중 하나",
                  "aiComment": "투자 참고용 코멘트 2~5문장, 마지막 문장에 투자 판단은 본인 책임이라는 취지를 담을 것"
                }
           """.formatted(
             request.stockCode(), request.marketName(), request.sectorName(), request.prevClose(),
             request.week52High(), request.week52Low(), request.sharesOutstanding(), request.eps(),
             request.bps(), request.sectorPer(), request.lastPrice(), request.lastChangePercent(),
             request.lastVolume(), String.join(", ", request.newsSourceUrls())
           );
  }

  // AI 일지 복기 프롬프트
  private String buildJournalPrompt(JournalReviewRequest request) {

    String memoPart = StringUtils.isEmpty(request.memo()) ? "" : ("메모: " + request.memo() + "\n");

    return """
                당신은 주식 투자 코치입니다. 아래 매매 기록을 보고 짧은 복기 코멘트를 작성하세요.
                존댓말로 자연스럽게 5~10문장으로 답하고, JSON이나 마크다운 없이 코멘트 텍스트만 답하세요.

                종목 코드: %s
                시장: %s
                거래유형: %s
                거래일: %s
                거래시간: %s
                가격: %s
                수량: %d
                %s
           """.formatted(
             request.stockCode(), request.marketName(), request.tradeTypeName(), request.tradeDate(),
             request.tradeTime(), request.price(), request.quantity(), memoPart
           );
  }

  // AI 호출
  private String callGemini(String prompt) {

    Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

    GeminiResponse response = restClient.post()
      .uri(ENDPOINT_PATH, model, apiKey)
      .contentType(MediaType.APPLICATION_JSON)
      .body(body)
      .retrieve()
      .body(GeminiResponse.class);

    if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
      throw new IllegalStateException("Gemini 응답이 비어있음");
    }

    List<Part> parts = response.candidates().getFirst().content().parts();
    if (parts == null || parts.isEmpty()) {
      throw new IllegalStateException("Gemini 응답에 parts가 없음");
    }

    return parts.stream()
      .map(Part::text)
      .filter(Objects::nonNull)
      .collect(Collectors.joining());
  }

  // AI 응답에서 순수 JSON 부분 추출
  private String extractJson(String text) {

    String trimmed = text.trim();
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');

    if (start == -1 || end == -1) throw new IllegalStateException("응답에서 JSON을 찾을 수 없음");
    return trimmed.substring(start, end + 1);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GeminiResponse(List<Candidate> candidates) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Candidate(Content content) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Content(List<Part> parts) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Part(String text) {
  }
}
