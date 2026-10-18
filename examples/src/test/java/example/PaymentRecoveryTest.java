package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cmriddles.thenewereconomy.api.result.ResultCode;
import com.cmriddles.thenewereconomy.api.testing.EconomySandbox;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Runs without Minecraft. The sandbox is a core-money test fixture, not a JDBC or inventory
 * emulator.
 */
final class PaymentRecoveryTest {
    @Test
    void lostReplyRecoversTheOriginalReceiptAndDoesNotPayTwiceAfterRestart() {
        var sandbox = new EconomySandbox();
        UUID payer = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        sandbox.seedPlayer(payer, "Payer", "coins", new BigDecimal("10.00"));
        sandbox.seedPlayer(receiver, "Receiver", "coins", BigDecimal.ZERO);
        var client = sandbox.client("MyPlugin");
        String savedKey = "payment:812"; // Persist this before the real business operation.

        sandbox.loseNextPaymentReply();
        var uncertain =
                client.transfer(payer, receiver, "coins", new BigDecimal("2.00"), savedKey)
                        .toCompletableFuture()
                        .join();
        assertFalse(uncertain.isSuccess());
        assertEquals(ResultCode.INTERNAL_ERROR, uncertain.failure().code());

        var afterRestart = sandbox.restart().client("MyPlugin");
        var recovered = afterRestart.receipt(savedKey).toCompletableFuture().join();
        assertTrue(recovered.isSuccess());
        assertTrue(recovered.value().isPresent());

        var duplicate =
                afterRestart
                        .transfer(payer, receiver, "coins", new BigDecimal("2.00"), savedKey)
                        .toCompletableFuture()
                        .join();
        assertFalse(duplicate.isSuccess());
        assertEquals(ResultCode.CONFLICT, duplicate.failure().code());
        assertEquals(
                0,
                new BigDecimal("2.00")
                        .compareTo(
                                afterRestart
                                        .balance(receiver)
                                        .toCompletableFuture()
                                        .join()
                                        .value()
                                        .amount()));
        assertEquals(
                0,
                new BigDecimal("8.00")
                        .compareTo(
                                afterRestart
                                        .balance(payer)
                                        .toCompletableFuture()
                                        .join()
                                        .value()
                                        .amount()));
    }
}
