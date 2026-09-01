package com.derk.easyinventorycrafter.client;

/** Pure presentation contract for the nearby panel's header and stable loading/empty states. */
public final class NearbyPanelPresentation {
    public static final int HEADER_COLOR = 0xFFFFFFFF;
    public static final int PARTIAL_HEADER_COLOR = 0xFFFFD166;
    public static final int STATUS_COLOR = 0xFFB8B8B8;

    private NearbyPanelPresentation() {
    }

    public static String header(boolean truncated) {
        return truncated ? "Nearby (partial)" : "Nearby";
    }

    public static Status status(boolean loading, boolean receivedPayload, int totalEntries, int filteredEntries) {
        if (filteredEntries > 0) {
            return Status.CONTENT;
        }
        if (loading || !receivedPayload) {
            return Status.LOADING;
        }
        return totalEntries == 0 ? Status.EMPTY : Status.NO_MATCHES;
    }

    public enum Status {
        CONTENT(null),
        LOADING("Loading..."),
        EMPTY("No nearby items"),
        NO_MATCHES("No matches");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
