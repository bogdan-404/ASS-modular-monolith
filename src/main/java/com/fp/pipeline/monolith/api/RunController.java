package com.fp.pipeline.monolith.api;

import com.fp.pipeline.monolith.service.RunService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RunController {
  private final RunService runService;
  private final JdbcTemplate jdbc;

  public RunController(RunService runService, JdbcTemplate jdbc) {
    this.runService = runService; this.jdbc = jdbc;
  }

  @PostMapping("/start-run")
  public Map<String,Object> startRun() {
    long processed = runService.runAll();
    jdbc.execute("REFRESH MATERIALIZED VIEW results_stats");
    var stats = jdbc.queryForMap("SELECT * FROM results_stats");
    return Map.of("processed", processed, "stats", stats);
  }

  @GetMapping("/stats")
  public Map<String,Object> stats() {
    jdbc.execute("REFRESH MATERIALIZED VIEW results_stats");
    return jdbc.queryForMap("SELECT * FROM results_stats");
  }
}

