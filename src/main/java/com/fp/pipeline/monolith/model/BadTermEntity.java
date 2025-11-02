package com.fp.pipeline.monolith.model;

import jakarta.persistence.*;

@Entity @Table(name="bad_terms")
public class BadTermEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  private String pattern;

  @Column(name="is_regex", nullable=false)
  private boolean isRegex = false;

  @Column(nullable=false)
  private boolean enabled = true;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPattern() { return pattern; }
  public void setPattern(String pattern) { this.pattern = pattern; }
  public boolean isRegex() { return isRegex; }
  public void setRegex(boolean regex) { isRegex = regex; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

