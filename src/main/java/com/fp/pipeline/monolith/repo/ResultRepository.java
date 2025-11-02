package com.fp.pipeline.monolith.repo;

import com.fp.pipeline.monolith.model.ResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultRepository extends JpaRepository<ResultEntity, Long> {}

