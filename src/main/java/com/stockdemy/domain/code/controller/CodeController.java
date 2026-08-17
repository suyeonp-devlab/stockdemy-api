package com.stockdemy.domain.code.controller;

import com.stockdemy.domain.code.dto.CodeResponse;
import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공통코드", description = "공통 코드 관련 API")
@RestController
@RequestMapping("/api/common-codes")
@RequiredArgsConstructor
public class CodeController {

  private final CodeService codeService;

  @Operation(summary = "공통 코드 조회")
  @GetMapping("/{groupId}")
  public ApiResponse<CodeResponse> getCodeList(
    @PathVariable String groupId,
    @RequestParam(required = false) String codeValue
  ) {
    return ApiResponse.success(codeService.getCodeList(groupId, codeValue));
  }
}
