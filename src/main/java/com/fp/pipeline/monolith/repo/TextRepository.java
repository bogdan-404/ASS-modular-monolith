package com.fp.pipeline.monolith.repo;

import com.fp.pipeline.monolith.model.TextEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TextRepository extends JpaRepository<TextEntity, Long> {

  @Query(value = "SELECT * FROM texts WHERE status='PENDING' ORDER BY id LIMIT :limit OFFSET :offset", nativeQuery = true)
  List<TextEntity> findPending(@Param("limit") int limit, @Param("offset") int offset);

  @Modifying @Transactional
  @Query(value = "UPDATE texts SET status='DONE' WHERE id=:id", nativeQuery = true)
  void markDone(@Param("id") Long id);
}

