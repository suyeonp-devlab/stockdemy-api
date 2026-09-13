package com.stockdemy.infra.news;

import com.stockdemy.infra.news.dto.NewsArticleItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 언론사 RSS 수집
 *
 * <p>언론사 경제·증권 RSS를 모아 최근 기사 목록을 만든다. 기사 URL이 언론사 원문이라 AI가 본문을
 * 직접 읽을 수 있다. 같은 기사가 여러 피드에 있으면 URL 기준으로 한 번만 남긴다.
 */
@Slf4j
@Component
public class RssNewsProvider implements NewsProvider {

  // 브라우저로 위장하지 않고 수집 주체를 밝힌다 (이 UA를 막는 피드는 수집에서 빠진다)
  private static final String USER_AGENT = "Stockdemy/1.0 (portfolio project)";

  // 발행일시 표기가 언론사마다 달라 순서대로 시도한다
  private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
    DateTimeFormatter.RFC_1123_DATE_TIME,
    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
  );

  private final RestClient restClient = RestClient.builder()
    .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
    .build();

  // 전체 피드 최근 기사 조회
  @Override
  public List<NewsArticleItem> fetchRecent() {

    Map<String, NewsArticleItem> articles = new LinkedHashMap<>();

    for (NewsFeed feed : NewsFeed.values()) {

      List<NewsArticleItem> items = fetchFeed(feed);
      items.forEach(item -> articles.putIfAbsent(item.sourceUrl(), item));

      log.debug("RSS 수집 ({}, {}): {}건", feed.getPressName(), feed.getUrl(), items.size());
    }

    return List.copyOf(articles.values());
  }

  // 피드 1건 조회
  private List<NewsArticleItem> fetchFeed(NewsFeed feed) {

    try {
      byte[] body = restClient.get().uri(feed.getUrl()).retrieve().body(byte[].class);

      if (body == null || body.length == 0) {
        return List.of();
      }

      return parse(feed, body);

    } catch (Exception e) {
      log.warn("RSS 조회 실패 ({}): {}", feed.getUrl(), e.getMessage());
      return List.of();
    }
  }

  // RSS 파싱 (인코딩은 XML 선언을 따르도록 바이트로 넘긴다)
  private List<NewsArticleItem> parse(NewsFeed feed, byte[] body) throws Exception {

    Document document = newDocumentBuilder().parse(new ByteArrayInputStream(body));
    NodeList items = document.getElementsByTagName("item");
    List<NewsArticleItem> articles = new ArrayList<>();

    for (int i = 0; i < items.getLength(); i++) {

      Element item = (Element) items.item(i);
      String title = text(item, "title");
      String link = text(item, "link");

      if (title == null || link == null) continue;

      articles.add(NewsArticleItem.builder()
        .title(normalizeTitle(title))
        .sourceUrl(link)
        .sourceName(feed.getPressName())
        .publishedAt(toPublishedAt(text(item, "pubDate")))
        .build());
    }

    return articles;
  }

  // 제목에 섞여 오는 태그와 HTML 엔티티 정리 (RSS 제목이 이스케이프된 채 오는 언론사가 있다)
  private String normalizeTitle(String title) {

    return title.replaceAll("<[^>]*>", "")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")
      .replace("&#39;", "'")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replaceAll("\\s+", " ")
      .trim();
  }

  // 외부 엔티티 참조 차단 (XXE 방지)
  private DocumentBuilder newDocumentBuilder() throws Exception {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    return factory.newDocumentBuilder();
  }

  private String text(Element item, String tagName) {

    NodeList nodes = item.getElementsByTagName(tagName);

    if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) return null;

    String value = nodes.item(0).getTextContent().trim();

    return value.isEmpty() ? null : value;
  }

  // 발행일시 → 서버 시간대 일시
  private LocalDateTime toPublishedAt(String pubDate) {

    if (pubDate == null) return null;

    // "Sat,12 Sep 2026"처럼 쉼표 뒤 공백이 빠진 표기가 있어 보정한다
    String normalized = pubDate.replaceFirst("^(\\w{3}),(?=\\S)", "$1, ");

    for (DateTimeFormatter format : DATE_FORMATS) {

      try {
        return ZonedDateTime.parse(normalized, format)
          .withZoneSameInstant(ZoneId.systemDefault())
          .toLocalDateTime();
      } catch (Exception ignored) {
        // 다음 형식으로 시도
      }

      try {
        return LocalDateTime.parse(normalized, format);
      } catch (Exception ignored) {
        // 다음 형식으로 시도
      }
    }

    log.warn("기사 발행일시 파싱 실패 (pubDate={})", pubDate);
    return null;
  }
}
