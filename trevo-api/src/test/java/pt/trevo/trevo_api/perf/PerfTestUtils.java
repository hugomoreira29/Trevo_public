package pt.trevo.trevo_api.perf;

import java.util.Arrays;

final class PerfTestUtils {

    private PerfTestUtils() {}

    static long p95(long[] durations) {
        long[] sorted = Arrays.copyOf(durations, durations.length);
        Arrays.sort(sorted);
        return sorted[(int)(sorted.length * 0.95)];
    }
}
