package com.stockdemy.domain.code.service;

import com.stockdemy.domain.code.dto.CodeItem;
import com.stockdemy.domain.code.dto.CodeResponse;
import com.stockdemy.domain.code.entity.Code;
import com.stockdemy.domain.code.entity.CodeGroup;
import com.stockdemy.domain.code.repository.CodeGroupRepository;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

  private final CodeGroupRepository codeGroupRepository;

  // 공통코드 목록 조회
  public CodeResponse getCodeList(String groupId, String codeValue) {

    CodeGroup group = codeGroupRepository.findByGroupIdAndEnabled(groupId, true)
      .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 공통코드 그룹입니다."));

    boolean hasCodeValue = StringUtils.hasText(codeValue);

    List<CodeItem> codes = group.getCodes().stream()
      .filter(Code::getEnabled)
      .filter(code -> !hasCodeValue || codeValue.equals(code.getCodeValue()))
      .map(CodeItem::from)
      .toList();

    return new CodeResponse(group.getGroupId(), group.getGroupName(), codes);
  }

  // 공통코드명 조회
  public String getCodeName(String groupId, String codeValue) {

    if (!StringUtils.hasText(codeValue)) return null;

    CodeGroup group = codeGroupRepository.findByGroupIdAndEnabled(groupId, true).orElse(null);
    if (group == null) return null;

    return group.getCodes().stream()
      .filter(code -> codeValue.equals(code.getCodeValue()))
      .map(Code::getCodeName)
      .findFirst().orElse(null);
  }
}
