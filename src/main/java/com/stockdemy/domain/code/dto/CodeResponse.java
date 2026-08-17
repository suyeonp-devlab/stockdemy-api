package com.stockdemy.domain.code.dto;

import java.util.List;

public record CodeResponse(
  String groupId,
  String groupName,
  List<CodeItem> codes) {
}
