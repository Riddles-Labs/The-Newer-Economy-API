package com.cmriddles.thenewereconomy.api.query;
import com.cmriddles.thenewereconomy.api.account.AccountRef;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
/** Transaction filters are applied before pagination. Time range is inclusive-from, exclusive-to. */
public record HistoryQuery(AccountRef account, Optional<String> currency, Optional<String> integration,
        Optional<Instant> from, Optional<Instant> to, Map<String,String> metadata, int limit, int offset) {
    public HistoryQuery { metadata = Map.copyOf(metadata); }
    public static HistoryQuery recent(AccountRef account, int limit, int offset) {
        return new HistoryQuery(account, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Map.of(), limit, offset);
    }
}
