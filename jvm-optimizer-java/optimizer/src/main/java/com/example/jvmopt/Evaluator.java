package com.example.jvmopt;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Evaluator {

    private final String imageName;
    private final int repeats;

    // baseline for normalization
    private double baselineTime = -1;
    private double baselineMem = -1;

    public Evaluator(String imageName, int repeats) {
        this.imageName = imageName;
        this.repeats = Math.max(1, repeats);
    }

    /** Получаем baseline (без JAVA_OPTS) */
    public void computeBaseline() throws Exception {
        double tSum = 0;
        double mSum = 0;
        for (int i = 0; i < repeats; i++) {
            List<String> out = DockerUtil.runContainer(imageName, "", null);
            Measurement meas = parseOutput(out);
            tSum += meas.timeMs;
            mSum += meas.memMb;
        }
        baselineTime = tSum / repeats;
        baselineMem = mSum / repeats;
        System.out.printf("Baseline: time=%.1f ms, mem=%.1f MB%n", baselineTime, baselineMem);
    }

    /** Запуск контейнера с указанными параметрами (Xms,Xmx), возвращает средние измерения */
    public Measurement measure(int xms, int xmx) throws Exception {
        double tSum = 0;
        double mSum = 0;
        String javaOpts = "-Xms" + xms + "m -Xmx" + xmx + "m";
        for (int i = 0; i < repeats; i++) {
            List<String> out = DockerUtil.runContainer(imageName, javaOpts, null);
            Measurement meas = parseOutput(out);
            tSum += meas.timeMs;
            mSum += meas.memMb;
        }
        double tAvg = tSum / repeats;
        double mAvg = mSum / repeats;
        return new Measurement(tAvg, mAvg);
    }

    /** Возвращает cost: нижe — лучше.
     * cost = (time / baselineTime) + (mem / baselineMem) — равномерно распределено
     */
    public double costFromMeasurement(Measurement meas) {
        if (baselineTime <= 0 || baselineMem <= 0) {
            throw new IllegalStateException("Baseline not computed");
        }
        double cost = (meas.timeMs / baselineTime) + (meas.memMb / baselineMem);
        return cost;
    }

    private Measurement parseOutput(List<String> out) {
        Pattern pTime = Pattern.compile("duration_ms:(\\d+)");
        Pattern pMem  = Pattern.compile("used_memory_mb:(\\d+)");
        double timeMs = -1;
        double memMb = -1;
        for (String line : out) {
            Matcher mt = pTime.matcher(line);
            if (mt.find()) {
                timeMs = Double.parseDouble(mt.group(1));
            }
            Matcher mm = pMem.matcher(line);
            if (mm.find()) {
                memMb = Double.parseDouble(mm.group(1));
            }
        }
        if (timeMs < 0) {
            throw new RuntimeException("Could not parse duration_ms from container output");
        }
        if (memMb < 0) {
            // если не найдено, можно поставить 0
            memMb = 0;
        }
        return new Measurement(timeMs, memMb);
    }

    public static class Measurement {
        public final double timeMs;
        public final double memMb;
        public Measurement(double timeMs, double memMb) {
            this.timeMs = timeMs;
            this.memMb = memMb;
        }
    }
}
