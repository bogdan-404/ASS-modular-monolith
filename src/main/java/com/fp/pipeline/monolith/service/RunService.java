package com.fp.pipeline.monolith.service;

import com.fp.pipeline.monolith.model.ResultEntity;
import com.fp.pipeline.monolith.model.TextEntity;
import com.fp.pipeline.monolith.model.BadTermEntity;
import com.fp.pipeline.monolith.repo.BadTermRepository;
import com.fp.pipeline.monolith.repo.ResultRepository;
import com.fp.pipeline.monolith.repo.TextRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Array;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class RunService {
  private final TextRepository texts;
  private final ResultRepository results;
  private final BadTermRepository badTerms;
  private final TextClassifier classifier;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactionTemplate;

  public RunService(TextRepository texts, ResultRepository results, BadTermRepository badTerms,
                    TextClassifier classifier, JdbcTemplate jdbc, TransactionTemplate transactionTemplate) {
    this.texts = texts; this.results = results; this.badTerms = badTerms; this.classifier = classifier;
    this.jdbc = jdbc;
    this.transactionTemplate = transactionTemplate;
  }

  /** Process all PENDING texts and return total processed count. */
  @Transactional
  public long runAll() {
    classifier.load(badTerms.findByEnabledTrue());
    int batch = 1000, offset = 0, total = 0;
    List<TextEntity> page;
    do {
      page = texts.findPending(batch, offset);
      for (TextEntity t : page) {
        int wc = classifier.wordCount(t.getContent());
        boolean hw = classifier.hasWords(t.getContent());
        int score = wc * (hw ? 2 : 1);

        ResultEntity re = new ResultEntity();
        re.setTextId(t.getId());
        re.setWordCount(wc);
        re.setHasWords(hw);
        re.setScore(score);
        results.save(re);
        texts.markDone(t.getId());
      }
      total += page.size();
      offset += batch;
    } while (!page.isEmpty());
    return total;
  }

  private void ensureSchema() {
    jdbc.execute("CREATE TABLE IF NOT EXISTS runs (" +
      "id BIGSERIAL PRIMARY KEY, " +
      "requested_count INT NOT NULL, " +
      "started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
      "finished_at TIMESTAMPTZ)");
    
    jdbc.execute("CREATE TABLE IF NOT EXISTS filtered_strings (" +
      "id BIGSERIAL PRIMARY KEY, " +
      "text_id BIGINT NOT NULL REFERENCES texts(id) ON DELETE CASCADE, " +
      "run_id BIGINT REFERENCES runs(id) ON DELETE SET NULL, " +
      "filtered_content TEXT NOT NULL, " +
      "removed_bad_term_ids BIGINT[] NOT NULL, " +
      "processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");
    
    jdbc.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS run_id BIGINT");
    
    jdbc.execute("DO $$ " +
      "BEGIN " +
      "IF NOT EXISTS ( " +
      "SELECT 1 FROM information_schema.table_constraints " +
      "WHERE constraint_name = 'fk_results_runs' AND table_name = 'results' " +
      ") THEN " +
      "ALTER TABLE results " +
      "ADD CONSTRAINT fk_results_runs " +
      "FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE SET NULL; " +
      "END IF; " +
      "END$$");
  }

  public Map<String, Object> runCount(int count) {
    ensureSchema();
    
    List<BadTermEntity> terms = badTerms.findByEnabledTrue();
    classifier.load(terms);
    
    List<Map<String, Object>> textsList = jdbc.query(
      "SELECT id, content FROM texts ORDER BY id",
      (rs, i) -> {
        Map<String, Object> m = new HashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("content", rs.getString("content"));
        return m;
      }
    );
    
    if (textsList.isEmpty()) {
      return Map.of("error", "No texts found in database");
    }
    
    int total = textsList.size();
    Long runId = transactionTemplate.execute(status -> 
      jdbc.queryForObject(
        "INSERT INTO runs(requested_count) VALUES (?) RETURNING id",
        Long.class,
        count
      )
    );
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < count; i++) {
      Map<String, Object> t = textsList.get(i % total);
      Long textId = (Long) t.get("id");
      String content = (String) t.get("content");
      
      int wc = classifier.wordCount(content);
      boolean hw = classifier.hasWords(content);
      int score = wc * (hw ? 2 : 1);
      
      TextClassifier.FilterOutcome fo = classifier.filterAndCollectIds(content, terms);
      
      // Process each item in its own transaction (like microservices)
      transactionTemplate.execute(status -> {
        ResultEntity re = new ResultEntity();
        re.setTextId(textId);
        re.setWordCount(wc);
        re.setHasWords(hw);
        re.setScore(score);
        re.setRunId(runId);
        results.save(re);
        
        Array removedArray = jdbc.execute((ConnectionCallback<Array>) conn -> 
          conn.createArrayOf("bigint", fo.removedIds.toArray(new Long[0]))
        );
        
        jdbc.update(
          "INSERT INTO filtered_strings(text_id, run_id, filtered_content, removed_bad_term_ids) VALUES (?,?,?,?)",
          textId, runId, fo.filtered, removedArray
        );
        return null;
      });
    }
    
    long durationMs = System.currentTimeMillis() - startTime;
    
    transactionTemplate.execute(status -> {
      jdbc.update("UPDATE runs SET finished_at = NOW() WHERE id = ?", runId);
      jdbc.execute("REFRESH MATERIALIZED VIEW results_stats");
      return null;
    });
    
    Map<String, Object> stats = jdbc.queryForMap("SELECT * FROM results_stats");
    
    Map<String, Object> result = new HashMap<>();
    result.put("runId", runId);
    result.put("requestedCount", count);
    result.put("processed", count);
    result.put("durationMs", durationMs);
    result.put("stats", stats);
    return result;
  }
}

