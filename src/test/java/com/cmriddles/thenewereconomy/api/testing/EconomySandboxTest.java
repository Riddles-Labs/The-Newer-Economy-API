package com.cmriddles.thenewereconomy.api.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.economy.Payment;
import com.cmriddles.thenewereconomy.api.economy.TransactionRequest;
import com.cmriddles.thenewereconomy.api.result.ResultCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class EconomySandboxTest {
    @Test
    void lostPaymentReplyCanBeReconciledAcrossRestartWithoutPayingAgain() {
        var sandbox = new EconomySandbox();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        sandbox.seedPlayer(alice, "Alice", "coins", BigDecimal.TEN);
        sandbox.seedPlayer(bob, "Bob", "coins", BigDecimal.ZERO);
        var client = sandbox.client("Shop");
        sandbox.loseNextPaymentReply();
        assertThat(
                        client.transfer(alice, bob, "coins", BigDecimal.ONE, "order:1")
                                .toCompletableFuture()
                                .join()
                                .failure()
                                .code())
                .isEqualTo(ResultCode.INTERNAL_ERROR);
        var restarted = sandbox.restart().client("Shop");
        assertThat(restarted.receipt("order:1").toCompletableFuture().join().value()).isPresent();
        assertThat(restarted.balance(bob).toCompletableFuture().join().value().amount())
                .isEqualByComparingTo("1");
        assertThat(
                        restarted
                                .transfer(alice, bob, "coins", BigDecimal.ONE, "order:1")
                                .toCompletableFuture()
                                .join()
                                .failure()
                                .code())
                .isEqualTo(ResultCode.CONFLICT);
    }

    @Test
    void batchFundingFailureLeavesEveryParticipantUnchangedAndInjectedFailuresAreBeforeCommit() {
        var sandbox = new EconomySandbox();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        sandbox.seedPlayer(alice, "Alice", "coins", BigDecimal.ONE);
        sandbox.seedPlayer(bob, "Bob", "coins", BigDecimal.ZERO);
        var client = sandbox.client("Shop");
        var request =
                new TransactionRequest(
                        List.of(
                                new Payment(
                                        AccountRef.player(alice),
                                        AccountRef.player(bob),
                                        "coins",
                                        new BigDecimal("2"))),
                        "order:1");
        assertThat(client.post(request).toCompletableFuture().join().failure().code())
                .isEqualTo(ResultCode.INSUFFICIENT_FUNDS);
        assertThat(client.balance(alice).toCompletableFuture().join().value().amount())
                .isEqualByComparingTo("1");
        assertThat(client.receipt("order:1").toCompletableFuture().join().value()).isEmpty();
        sandbox.failNextPayment(ResultCode.DEPENDENCY_UNAVAILABLE);
        assertThat(
                        client.transfer(alice, bob, BigDecimal.ONE, "next")
                                .toCompletableFuture()
                                .join()
                                .failure()
                                .code())
                .isEqualTo(ResultCode.DEPENDENCY_UNAVAILABLE);
        assertThat(client.receipt("next").toCompletableFuture().join().value()).isEmpty();
    }
}
