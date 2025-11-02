package com.fp.pipeline.monolith.service;

import com.fp.pipeline.monolith.model.ResultEntity;
import com.fp.pipeline.monolith.model.TextEntity;
import com.fp.pipeline.monolith.repo.BadTermRepository;
import com.fp.pipeline.monolith.repo.ResultRepository;
import com.fp.pipeline.monolith.repo.TextRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RunService {
  private final TextRepository texts;
  private final ResultRepository results;
  private final BadTermRepository badTerms;
  private final TextClassifier classifier;
  private final Timer batchTimer;

  public RunService(TextRepository texts, ResultRepository results, BadTermRepository badTerms,
                    TextClassifier classifier, MeterRegistry mr) {
    this.texts = texts; this.results = results; this.badTerms = badTerms; this.classifier = classifier;
    this.batchTimer = mr.timer("monolith.batch.duration_seconds");
  }

  /** Process all PENDING texts and return total processed count. */
  @Transactional
  public long runAll() {
    classifier.load(badTerms.findByEnabledTrue());
    return batchTimer.record(() -> {
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
    });
  }
}

