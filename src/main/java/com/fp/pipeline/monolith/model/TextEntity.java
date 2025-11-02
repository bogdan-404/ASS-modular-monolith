package com.fp.pipeline.monolith.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="texts")
public class TextEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, columnDefinition="TEXT")
  private String content;

  @Column(nullable=false)
  private String status = "PENDING";

  @Column(name="created_at", nullable=false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

