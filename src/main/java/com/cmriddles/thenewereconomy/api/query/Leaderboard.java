package com.cmriddles.thenewereconomy.api.query;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
/** Overall values are percentile-rank scores, not money. Names are presentation concerns. */
public record Leaderboard(String selector, boolean overall, int scale, int offset, List<Entry> entries, boolean hasNext) {
    public Leaderboard { entries = List.copyOf(entries); }
    public record Entry(UUID playerId, BigDecimal value) { }
}
