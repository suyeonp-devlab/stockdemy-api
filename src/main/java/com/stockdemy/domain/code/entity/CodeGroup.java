package com.stockdemy.domain.code.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeGroup extends BaseTimeEntity {

  @Id
  @Column(length = 50)
  private String groupId;

  // 코드 그룹명
  @Column(nullable = false)
  private String groupName;

  // 코드 그룹 설명
  private String groupDesc;

  // 사용여부
  @Column(nullable = false)
  private Boolean enabled;

  // 코드 목록
  @OneToMany(mappedBy = "codeGroup", fetch = FetchType.EAGER)
  private List<Code> codes = new ArrayList<>();

  @Builder
  private CodeGroup(
    String groupId, String groupName, String groupDesc, boolean enabled
  ) {
    this.groupId = groupId;
    this.groupName = groupName;
    this.groupDesc = groupDesc;
    this.enabled = enabled;
  }
}
