package com.stockdemy.infra.dart;

import com.stockdemy.infra.dart.dto.DisclosureItem;

import java.util.List;

public interface DisclosureProvider {
  List<DisclosureItem> fetchRecent();
}
