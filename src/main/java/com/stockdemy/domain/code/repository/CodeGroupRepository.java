package com.stockdemy.domain.code.repository;

import com.stockdemy.domain.code.entity.CodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, String> {

  Optional<CodeGroup> findByGroupIdAndEnabled(String groupId, boolean enabled);
}
