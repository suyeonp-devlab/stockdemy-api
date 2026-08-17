package com.stockdemy.domain.code.dto;

import com.stockdemy.domain.code.entity.Code;
import lombok.Builder;

@Builder
public record CodeItem(
  long codeId,
  String codeValue,
  String codeName,
  String param1,
  String param2,
  String param3
) {

  public static CodeItem from(Code code) {
    return CodeItem.builder()
      .codeId(code.getCodeId())
      .codeValue(code.getCodeValue())
      .codeName(code.getCodeName())
      .param1(code.getParam1())
      .param2(code.getParam2())
      .param3(code.getParam3())
      .build();
  }
}
