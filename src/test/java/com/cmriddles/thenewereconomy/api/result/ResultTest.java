package com.cmriddles.thenewereconomy.api.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ResultTest {
    @Test
    void typedFailuresPreserveCodeMessageAndCorrelationId() {
        CorrelationId correlationId = new CorrelationId(UUID.randomUUID());
        Failure failure = new Failure(ResultCode.VALIDATION_ERROR, "bad input", correlationId);
        Result<String> result = Result.failure(failure);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.failure().code()).isEqualTo(ResultCode.VALIDATION_ERROR);
        assertThat(result.failure().message()).isEqualTo("bad input");
        assertThat(result.failure().correlationId()).isEqualTo(correlationId);
    }
}
