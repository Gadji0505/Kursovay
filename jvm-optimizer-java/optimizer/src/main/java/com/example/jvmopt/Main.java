package com.example.jvmopt;

public class Main {

    public static void main(String[] args) throws Exception {
        // Параметры: можно вынести в args/env
        String imageName = "jvm-test-image"; // имя Docker-образа (собираем ниже)
        int population = 6;
        int iterations = 6;
        int minHeap = 128;
        int maxHeap = 1024;
        int repeats = 2; // сколько прогонов для усреднения

        System.out.println("=== JVM Optimizer (HBSA) starting ===");

        // Предварительно: собери образ вручную (см README) или можно попытаться вызвать docker build тут.
        Evaluator evaluator = new Evaluator(imageName, repeats);
        Optimizer opt = new Optimizer(population, iterations, minHeap, maxHeap, evaluator);

        Optimizer.Result result = opt.optimize();

        System.out.println("\n=== Optimization finished ===");
        System.out.printf("Best found: -Xms%dm -Xmx%dm | cost=%.4f | time=%.1fms mem=%.1fMB%n",
                result.xms, result.xmx, result.cost, result.timeMs, result.memMb);

        System.out.println("To compare: run baseline container (no JAVA_OPTS) and run with these flags.");
    }
}
