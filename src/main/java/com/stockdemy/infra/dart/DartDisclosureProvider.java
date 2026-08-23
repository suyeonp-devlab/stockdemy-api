package com.stockdemy.infra.dart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.stockdemy.infra.dart.dto.DisclosureItem;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
public class DartDisclosureProvider implements DisclosureProvider {

  private static final String BASE_URL = "https://opendart.fss.or.kr";
  private static final String LIST_PATH = "/api/list.json";
  private static final DateTimeFormatter DATE_PARAM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final int RECENT_DAYS = 5;

  private final String apiKey;
  private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

  // 최근 공시 조회
  @Override
  public List<DisclosureItem> fetchRecent() {

    if (StringUtils.isEmpty(apiKey)) {
      log.warn("DART_API_KEY가 설정되지 않아 공시 조회를 건너뜁니다");
      return List.of();
    }

    try {
      LocalDate today = LocalDate.now();
      String url = UriComponentsBuilder.fromPath(LIST_PATH)
        .queryParam("crtfc_key", apiKey)
        .queryParam("bgn_de", today.minusDays(RECENT_DAYS).format(DATE_PARAM_FORMAT))
        .queryParam("end_de", today.format(DATE_PARAM_FORMAT))
        .queryParam("page_count", 30)
        .toUriString();

      DartListResponse response = restClient.get().uri(url).retrieve().body(DartListResponse.class);

      if (response == null || response.list() == null) {
        return List.of();
      }

      return response.list().stream().map(this::toItem).toList();

    } catch (Exception e) {
      log.warn("DART 공시 조회 실패: {}", e.getMessage());
      return List.of();
    }
  }

  private DisclosureItem toItem(DartDisclosure d) {

    return DisclosureItem.builder()
      .receiptNo(d.rceptNo())
      .corpName(d.corpName())
      .stockCode(d.stockCode())
      .reportName(d.reportNm())
      .receivedAt(d.rceptDt())
      .sourceUrl("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + d.rceptNo())
      .build();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DartListResponse(String status, List<DartDisclosure> list) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DartDisclosure(
    @JsonProperty("rcept_no") String rceptNo,
    @JsonProperty("corp_name") String corpName,
    @JsonProperty("stock_code") String stockCode,
    @JsonProperty("report_nm") String reportNm,
    @JsonProperty("rcept_dt") String rceptDt) {
  }
}
