package com.stockdemy.domain.code.repository;

import com.stockdemy.domain.code.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepository extends JpaRepository<Code, Long> {
}
