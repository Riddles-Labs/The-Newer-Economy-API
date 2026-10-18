package com.cmriddles.thenewereconomy.api.event;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
/** Optional filters; empty sets match all values in this server's scope. */
public record EventFilter(Set<String> types, Optional<String> sourceNamespace, Optional<UUID> accountId) {
 public EventFilter { types=Set.copyOf(types); java.util.Objects.requireNonNull(sourceNamespace); java.util.Objects.requireNonNull(accountId);
  if(types.size()>16)throw new IllegalArgumentException("At most 16 event types"); }
 public static EventFilter all() { return new EventFilter(Set.of(),Optional.empty(),Optional.empty()); }
}
