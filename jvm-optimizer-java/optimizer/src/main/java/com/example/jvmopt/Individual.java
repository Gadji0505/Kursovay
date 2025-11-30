package com.example.jvmopt;

/** Одна "бактерия" / частица: параметры + личный лучший и скорость для PSO */
public class Individual {
    public int xms;
    public int xmx;
    public double[] vel = new double[2]; // velocity for xms,xmx
    public double bestCost = Double.POSITIVE_INFINITY;
    public int bestXms, bestXmx;

    public Individual(int xms, int xmx) {
        this.xms = xms;
        this.xmx = xmx;
        this.bestXms = xms;
        this.bestXmx = xmx;
    }

    public Individual copy() {
        Individual i = new Individual(this.xms, this.xmx);
        i.vel = new double[]{this.vel[0], this.vel[1]};
        i.bestCost = this.bestCost;
        i.bestXms = this.bestXms;
        i.bestXmx = this.bestXmx;
        return i;
    }
}
