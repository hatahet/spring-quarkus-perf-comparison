# Helidon Performance Findings

Date: 2026-07-11

## Observed behavior

- Helidon SE produced repeated HTTP 500 responses caused by Hikari pool-acquisition failures:

  ```text
  SQLTransientConnectionException: helidon4-se - Connection is not available,
  request timed out after approximately 2 seconds
  ```

- One Helidon MP run reported about 4.2K successful requests/second but 25,825 request timeouts, with latency capped at exactly one second.
- A later Helidon run reported about 3.9K successful requests/second and 1,691 request timeouts, compared with 20K+ requests/second for plain Quarkus.

## Connection-pool diagnosis

- The benchmark uses more concurrent HTTP requests than the configured 50-connection Hikari pool.
- Helidon SE was configured with a two-second Hikari `connectionTimeout`. Once all pool connections were occupied, queued requests that waited longer than two seconds became HTTP 500 responses.
- Repeated full exception stack traces create a feedback loop: logging consumes application CPU, requests retain connections longer, and additional acquisition attempts time out.
- The SE repository fully consumes Helidon DB Client's auto-closing result stream. Static inspection did not reveal a connection leak in its `findAll` path.
- The JPA implementations use fetch-join queries, so no obvious N+1 query problem was found for `GET /fruits`.
- Restoring Hikari's normal 30-second acquisition timeout would prevent ordinary short queueing from immediately becoming HTTP 500 responses, but increasing the timeout does not increase sustainable capacity.
- Increasing the pool beyond 50 could merely move the bottleneck into PostgreSQL and should not be the first response.

## Local stress-test fairness issue

The local `scripts/stress.sh` result is not a fair CPU comparison:

- It starts JVM applications with `-XX:ActiveProcessorCount=1` but does not restrict CPU affinity with `taskset`.
- Helidon handles requests using virtual threads. Its carrier and worker sizing respects the one-processor setting.
- The plain blocking Quarkus worker pool does not fully honor `ActiveProcessorCount`, as the script's own comments note, and can consume additional host CPUs.
- Consequently, roughly 4K RPS for Helidon versus 20K+ RPS for Quarkus can primarily reflect one effective Helidon CPU versus multiple CPUs used by Quarkus.

The local load is also aggressive:

```text
-t8 -c200 -d20s --timeout 1s
```

This combines 200 clients, a 50-connection database pool, one effective Helidon application CPU, and a one-second client deadline. The client can abandon a request before Hikari's two-second acquisition timeout. The exact one-second latency ceiling in the MP output supports that interpretation.

## Recommended validation

1. Use actual CPU affinity for all applications, PostgreSQL, and the load generator. For example, allocate four application CPUs with both `taskset --cpu-list 0-3` and `-XX:ActiveProcessorCount=4`, and put PostgreSQL and the load generator on separate CPUs.
2. Prefer the performance lab for cross-framework conclusions because it uses `taskset`; do not treat the current `stress.sh` numbers as comparable framework throughput.
3. Run a concurrency sweep at 10, 25, 50, 75, 100, and 200 clients. Record throughput, p99 latency, errors, Hikari active connections, idle connections, and pending acquisitions.
4. Treat the highest zero-error load before the latency knee as sustainable capacity. Do not use successful requests/second alone when the run also reports timeouts.
5. Temporarily enable Hikari leak detection to confirm pool behavior, then remove it before final measurements.
6. Compare telemetry enabled versus disabled to quantify HTTP, JDBC, metrics, and OTLP-export overhead. Ensure the collector is healthy and not retrying failed exports.
7. Restore a realistic Hikari acquisition timeout, such as the default 30 seconds, while retaining the 50-connection cap for an apples-to-apples comparison.

## Current conclusion

The observed exceptions are genuine pool saturation, but the large Helidon-versus-Quarkus gap from the local stress script is dominated by unfair CPU accounting. The timeout storm is amplified by a one-second client deadline, a two-second pool-acquisition deadline, 200 concurrent clients, and synchronous exception logging. A CPU-pinned, zero-error performance-lab run is required before attributing the remaining difference to Helidon itself.
