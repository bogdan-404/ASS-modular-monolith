package com.fp.pipeline.monolith.service;

import com.fp.pipeline.monolith.model.BadTermEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TextClassifier {
  private volatile List<String> patterns = List.of();

  public void load(List<BadTermEntity> terms) {
    patterns = terms.stream().map(t -> t.getPattern().toLowerCase()).toList();
  }

  public int wordCount(String s) {
    String[] toks = s.split("\\W+");
    int c = 0; for (String t : toks) if (!t.isBlank()) c++;
    return c;
  }

  public boolean hasWords(String s) {
    String low = s.toLowerCase();
    for (String p : patterns) if (low.contains(p)) return true;
    return false;
  }

  public int score(String s) {
    int wc = wordCount(s);
    return wc * (hasWords(s) ? 2 : 1);
  }
}

