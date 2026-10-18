package com.cmriddles.thenewereconomy.api.event;
import java.time.Instant;
import java.util.Map;
/** A committed durable event. Transaction events cover all ledger origins, not just native plugins. */
public record NativeEvent(long sequence, String type, String sourceNamespace, String subject,
 Instant createdAt, Map<String,String> attributes) {
 public NativeEvent { attributes=Map.copyOf(attributes); }
}
