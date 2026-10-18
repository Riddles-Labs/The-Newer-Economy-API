package com.cmriddles.thenewereconomy.api.event;

import java.util.List;

/** Save nextCursor only after processing the page. Replay is at least once. */
public record EventPage(List<NativeEvent> events, long nextCursor) {
    public EventPage {
        events = List.copyOf(events);
    }
}
