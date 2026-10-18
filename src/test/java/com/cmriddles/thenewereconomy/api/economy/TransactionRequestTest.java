package com.cmriddles.thenewereconomy.api.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cmriddles.thenewereconomy.api.account.AccountRef;
import com.cmriddles.thenewereconomy.api.currency.ScopeId;
import com.cmriddles.thenewereconomy.api.result.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionRequestTest {
    @Test
    void callerCannotChangeAnAtomicRequestOrReceiptAfterConstruction() {
        Payment payment = payment();
        List<Payment> payments = new ArrayList<>(List.of(payment));
        Map<String, String> metadata = new HashMap<>(Map.of("order", "17"));
        TransactionRequest request = new TransactionRequest(payments, "order:17", metadata);
        payments.clear();
        metadata.put("order", "18");

        assertThat(request.payments()).containsExactly(payment);
        assertThat(request.metadata()).containsExactlyEntriesOf(Map.of("order", "17"));
        assertThatThrownBy(() -> request.payments().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.metadata().clear()).isInstanceOf(UnsupportedOperationException.class);

        TransactionEntry debit = new TransactionEntry(payment.source(), "Pool", payment.currency(),
                ScopeId.network(), payment.amount().negate());
        TransactionEntry credit = new TransactionEntry(payment.destination(), "Player", payment.currency(),
                ScopeId.network(), payment.amount());
        List<TransactionEntry> entries = new ArrayList<>(List.of(debit, credit));
        Map<String, String> audit = new HashMap<>(Map.of("integrationName", "Example"));
        TransactionReceipt receipt = new TransactionReceipt(UUID.randomUUID(), UUID.randomUUID(), "Example", "order:17",
                Instant.parse("2026-10-17T00:00:00Z"), entries, audit);
        entries.clear();
        audit.clear();

        assertThat(receipt.entries()).containsExactly(debit, credit);
        assertThat(receipt.metadata()).containsEntry("integrationName", "Example");
        assertThatThrownBy(() -> receipt.entries().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> receipt.metadata().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void atomicRequestBoundsAllowTheDocumentedMaximumAndRejectOversizedPayloads() {
        List<Payment> maximum = new ArrayList<>();
        for (int i = 0; i < TransactionRequest.MAX_PAYMENTS; i++) maximum.add(payment());
        Map<String, String> metadata = new HashMap<>();
        for (int i = 0; i < TransactionRequest.MAX_METADATA_ENTRIES; i++) {
            metadata.put("k".repeat(62) + String.format("%02d", i), "v".repeat(512));
        }
        TransactionRequest accepted = new TransactionRequest(maximum, "batch:1", metadata);
        assertThat(accepted.payments()).hasSize(100);
        assertThat(accepted.metadata()).hasSize(16);

        maximum.add(payment());
        assertThatThrownBy(() -> new TransactionRequest(maximum, "batch:2"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TransactionRequest(List.of(), "batch:empty"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TransactionRequest(List.of(payment()), "batch:3", Map.of("key", "v".repeat(513))))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TransactionRequest(List.of(payment()), "batch:4", Map.of("k".repeat(65), "value")))
                .isInstanceOf(ValidationException.class);
        metadata.put("seventeenth", "value");
        assertThatThrownBy(() -> new TransactionRequest(List.of(payment()), "batch:5", metadata))
                .isInstanceOf(ValidationException.class);
    }

    private static Payment payment() {
        return new Payment(AccountRef.account(UUID.randomUUID()), AccountRef.player(UUID.randomUUID()),
                "coins", new BigDecimal("12.50"));
    }
}
