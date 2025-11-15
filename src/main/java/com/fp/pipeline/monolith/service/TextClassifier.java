package com.fp.pipeline.monolith.service;

import com.fp.pipeline.monolith.model.BadTermEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

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

  public static final class FilterOutcome {
    public final String filtered;
    public final List<Long> removedIds;
    public FilterOutcome(String f, List<Long> r) {
      this.filtered = f;
      this.removedIds = r;
    }
  }

  public FilterOutcome filterAndCollectIds(String content, List<BadTermEntity> terms) {
    String out = content;
    String low = content.toLowerCase();
    List<Long> removed = new ArrayList<>();
    for (var t : terms) {
      String pat = t.getPattern();
      String patLow = pat.toLowerCase();
      if (low.contains(patLow)) {
        removed.add(t.getId());
        String regex = "(?i)(?<!\\w)" + Pattern.quote(pat) + "(?!\\w)";
        out = out.replaceAll(regex, " ");
        low = out.toLowerCase();
      }
    }
    out = out.replaceAll("\\s+", " ").trim();
    return new FilterOutcome(out, removed);
  }
}

