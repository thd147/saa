package io.stairpilot;

import java.util.ArrayDeque;
import java.util.Locale;

/** In-memory, bounded technical log only. Never saves screen pixels or personal data. */
public final class Diagnostics {
    private static final int MAX = 35;
    private static final ArrayDeque<String> entries = new ArrayDeque<>();
    private Diagnostics() { }
    public static synchronized void record(long timeMs, String message) {
        String safe = message == null ? "" : message.replace('\n',' ').replace('\r',' ');
        if (safe.length() > 120) safe = safe.substring(0, 120);
        if (entries.size() == MAX) entries.removeFirst();
        entries.addLast(String.format(Locale.ROOT, "%ds: %s", timeMs / 1000, safe));
    }
    public static synchronized String snapshot() {
        StringBuilder b = new StringBuilder("무계매크로 v1.5.0 · 화면/개인정보 비포함\n");
        for (String line : entries) b.append(line).append('\n');
        return b.toString();
    }
    public static synchronized void clear() { entries.clear(); }
}
