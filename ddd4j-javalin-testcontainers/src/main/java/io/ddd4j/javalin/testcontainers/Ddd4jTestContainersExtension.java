package io.ddd4j.javalin.testcontainers;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 Extension that disables tests when no Docker daemon is available.
 *
 * <p>Aligned with ddd4j-boot mq/README.md §13: Testcontainers requires a running Docker
 * daemon. In environments without Docker (e.g. some CI nodes) the integration tests must be
 * silently skipped rather than failed.
 */
public class Ddd4jTestContainersExtension implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Docker daemon assumed available");

    private static final ConditionEvaluationResult DISABLED =
            ConditionEvaluationResult.disabled("Docker daemon is not available; skipping Testcontainers integration test");

    private static final String DOCKER_HOST = System.getenv("DOCKER_HOST");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (DOCKER_HOST == null || DOCKER_HOST.isBlank()) {
            return ENABLED;
        }
        if (DOCKER_HOST.contains("tcp:") || DOCKER_HOST.contains("unix:") || DOCKER_HOST.contains("npipe:")) {
            return ENABLED;
        }
        return DISABLED;
    }
}