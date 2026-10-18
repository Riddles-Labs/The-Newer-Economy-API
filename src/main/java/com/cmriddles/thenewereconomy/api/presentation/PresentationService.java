package com.cmriddles.thenewereconomy.api.presentation;
import com.cmriddles.thenewereconomy.api.result.Result;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
/** Currency-aware plain text and native menu opens, without executing commands. */
public interface PresentationService {
    CompletionStage<Result<String>> format(String currency, BigDecimal amount, MoneyStyle style);
    /** Result completes after opening or a typed rejection, not merely queueing a command. */
    CompletionStage<Result<MenuView>> open(UUID player, MenuView view);
}
