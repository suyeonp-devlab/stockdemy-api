package com.stockdemy.domain.code.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "codes", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "code_value"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Code extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long codeId;

  // 공통 코드 값
  @Column(nullable = false, length = 20)
  private String codeValue;

  // 공통 코드명
  @Column(nullable = false)
  private String codeName;

  // 공통 코드 그룹
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private CodeGroup codeGroup;

  // 정렬 순서
  @Column(nullable = false)
  private Integer sortOrder;

  // 사용 여부
  @Column(nullable = false)
  private Boolean enabled;

  // 파라미터1
  private String param1;

  // 파라미터2
  private String param2;

  // 파라미터3
  private String param3;

  @Builder
  private Code(
    String codeValue, String codeName, CodeGroup codeGroup, int sortOrder, boolean enabled,
    String param1, String param2, String param3
  ) {
    this.codeValue = codeValue;
    this.codeName = codeName;
    this.codeGroup = codeGroup;
    this.sortOrder = sortOrder;
    this.enabled = enabled;
    this.param1 = param1;
    this.param2 = param2;
    this.param3 = param3;
  }
}
