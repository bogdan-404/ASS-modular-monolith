package com.fp.pipeline.monolith.repo;

import com.fp.pipeline.monolith.model.BadTermEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadTermRepository extends JpaRepository<BadTermEntity, Long> {
  List<BadTermEntity> findByEnabledTrue();
}

