// app/TestApp.java
public class TestApp {
    public static void main(String[] args) {
        int iterations = 40_000_000; // можно поменять через аргумент
        if (args.length > 0) {
            try { iterations = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }

        long start = System.nanoTime();
        double s = 0;
        for (int i = 0; i < iterations; i++) {
            s += Math.sin(i) * Math.cos(i % 100);
        }
        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;

        long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("duration_ms:" + durationMs);
        System.out.println("used_memory_mb:" + (usedBytes / (1024 * 1024)));
        // keep output deterministic
        System.out.println("dummy:" + s);
    }
}
