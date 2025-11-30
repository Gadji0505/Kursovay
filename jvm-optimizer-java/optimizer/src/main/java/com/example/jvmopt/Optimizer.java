package com.example.jvmopt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Hybrid Bacterial Swarm (BFO + PSO) optimizer.
 *
 * Простая, понятная версия:
 * - популяция N (individuals),
 * - для каждой итерации: PSO-обновление скоростей и позиций,
 * - хемотаксис (малые случайные шаги),
 * - репродукция (берём лучших половину и дублируем),
 * - элиминация/разброс с вероятностью p.
 */
public class Optimizer {

    private final int populationSize;
    private final int iterations;
    private final int minHeap;
    private final int maxHeap;
    private final double inertia = 0.6;
    private final double c1 = 1.2;
    private final double c2 = 1.2;
    private final double eliminationProb = 0.05;
    private final Evaluator evaluator;
    private final Random rnd = new Random();

    public Optimizer(int populationSize, int iterations, int minHeap, int maxHeap, Evaluator evaluator) {
        this.populationSize = populationSize;
        this.iterations = iterations;
        this.minHeap = minHeap;
        this.maxHeap = maxHeap;
        this.evaluator = evaluator;
    }

    public Result optimize() throws Exception {
        // 1) init population
        List<Individual> pop = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            int xms = rnd.nextInt(maxHeap - minHeap + 1) + minHeap;
            int xmx = rnd.nextInt(maxHeap - Math.max(xms, minHeap) + 1) + Math.max(xms, minHeap);
            Individual ind = new Individual(xms, xmx);
            pop.add(ind);
        }

        // compute baseline once
        evaluator.computeBaseline();

        // evaluate initial
        Individual globalBest = null;
        double globalBestCost = Double.POSITIVE_INFINITY;
        for (Individual ind : pop) {
            Evaluator.Measurement meas = evaluator.measure(ind.xms, ind.xmx);
            double cost = evaluator.costFromMeasurement(meas);
            ind.bestCost = cost;
            ind.bestXms = ind.xms;
            ind.bestXmx = ind.xmx;
            if (cost < globalBestCost) {
                globalBestCost = cost;
                globalBest = ind.copy();
            }
            System.out.printf("Init: Xms=%d Xmx=%d -> cost=%.4f (time=%.1fms mem=%.1fMB)%n",
                    ind.xms, ind.xmx, cost, meas.timeMs, meas.memMb);
        }

        // main loop
        for (int it = 0; it < iterations; it++) {
            System.out.printf("=== Iteration %d/%d; globalBestCost=%.4f (Xms=%d Xmx=%d)%n",
                    it+1, iterations, globalBestCost, globalBest.xms, globalBest.xmx);

            // PSO step + chemotaxis
            for (Individual ind : pop) {
                // velocity update for xms,xmx
                double r1 = rnd.nextDouble(), r2 = rnd.nextDouble();
                int[] pos = new int[]{ind.xms, ind.xmx};
                int[] pbest = new int[]{ind.bestXms, ind.bestXmx};
                int[] gbest = new int[]{globalBest.xms, globalBest.xmx};

                for (int d = 0; d < 2; d++) {
                    double vel = inertia * ind.vel[d] + c1 * r1 * (pbest[d] - pos[d]) + c2 * r2 * (gbest[d] - pos[d]);
                    ind.vel[d] = vel;
                    // update position (as double, later clamp to int)
                    pos[d] = (int) Math.round(pos[d] + vel);
                }

                // chemotaxis: небольшой случайный «поворот» (tumble) с вероятностью
                if (rnd.nextDouble() < 0.3) {
                    pos[0] += rnd.nextInt(21) - 10;
                    pos[1] += rnd.nextInt(41) - 20;
                }

                // clamp & enforce xms <= xmx
                pos[0] = clamp(pos[0], minHeap, maxHeap);
                pos[1] = clamp(pos[1], Math.max(pos[0], minHeap), maxHeap);

                // evaluate new position
                Evaluator.Measurement meas = evaluator.measure(pos[0], pos[1]);
                double cost = evaluator.costFromMeasurement(meas);

                // if better than personal best -> update
                if (cost < ind.bestCost) {
                    ind.bestCost = cost;
                    ind.bestXms = pos[0];
                    ind.bestXmx = pos[1];
                }

                // update individual's position
                ind.xms = pos[0];
                ind.xmx = pos[1];

                // update global best
                if (cost < globalBestCost) {
                    globalBestCost = cost;
                    globalBest = ind.copy();
                }

                System.out.printf("  Tried: Xms=%d Xmx=%d cost=%.4f (time=%.1fms mem=%.1fMB)%n",
                        pos[0], pos[1], cost, meas.timeMs, meas.memMb);
            }

            // reproduction: keep top half
            pop.sort(Comparator.comparingDouble(a -> a.bestCost));
            List<Individual> topHalf = new ArrayList<>();
            for (int i = 0; i < populationSize/2; i++) topHalf.add(pop.get(i).copy());

            // duplicate topHalf to rebuild pop
            pop.clear();
            for (Individual ind : topHalf) pop.add(ind.copy());
            for (Individual ind : topHalf) pop.add(ind.copy());

            // elimination-dispersal
            for (int i = 0; i < pop.size(); i++) {
                if (rnd.nextDouble() < eliminationProb) {
                    int xms = rnd.nextInt(maxHeap - minHeap + 1) + minHeap;
                    int xmx = rnd.nextInt(maxHeap - Math.max(xms, minHeap) + 1) + Math.max(xms, minHeap);
                    pop.set(i, new Individual(xms, xmx));
                    System.out.printf("  Eliminated & reinitialized idx=%d -> Xms=%d Xmx=%d%n", i, xms, xmx);
                }
            }
        }

        // final global best: evaluate to show numbers
        Evaluator.Measurement finalMeas = evaluator.measure(globalBest.xms, globalBest.xmx);
        double finalCost = evaluator.costFromMeasurement(finalMeas);
        return new Result(globalBest.xms, globalBest.xmx, finalCost, finalMeas.timeMs, finalMeas.memMb);
    }

    private int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    public static class Result {
        public final int xms, xmx;
        public final double cost;
        public final double timeMs, memMb;
        public Result(int xms, int xmx, double cost, double timeMs, double memMb) {
            this.xms = xms; this.xmx = xmx; this.cost = cost; this.timeMs = timeMs; this.memMb = memMb;
        }
    }
}
