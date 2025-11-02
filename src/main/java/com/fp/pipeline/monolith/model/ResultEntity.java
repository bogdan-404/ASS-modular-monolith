package com.fp.pipeline.monolith.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="results")
public class ResultEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(name="text_id", nullable=false)
  private Long textId;

  private int wordCount;
  private boolean hasWords;
  private int score;

  @Column(name="processed_at", nullable=false)
  private OffsetDateTime processedAt = OffsetDateTime.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTextId() { return textId; }
  public void setTextId(Long textId) { this.textId = textId; }
  public int getWordCount() { return wordCount; }
  public void setWordCount(int wordCount) { this.wordCount = wordCount; }
  public boolean isHasWords() { return hasWords; }
  public void setHasWords(boolean hasWords) { this.hasWords = hasWords; }
  public int getScore() { return score; }
  public void setScore(int score) { this.score = score; }
  public OffsetDateTime getProcessedAt() { return processedAt; }
  public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}

