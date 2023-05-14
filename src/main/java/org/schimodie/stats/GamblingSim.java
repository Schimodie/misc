package org.schimodie.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class GamblingSim {
    @FunctionalInterface
    private interface InvestmentStrategy {
        double invest(double capital);
    }

    private static class Stats {
        private static final double[] PERCENTILES = {0.0, 0.01, 0.1, 0.25, 0.5, 0.75, 0.9, 0.99, 1.0};
        private final List<Double> data;

        private double[] percentiles;
        private double mean;
        private double standardDeviation;
        private double variance;

        public Stats() {
            data = new ArrayList<>();
            percentiles = new double[PERCENTILES.length];
        }

        public void add(double datum) {
            data.add(datum);
        }

        public void compute() {
            Collections.sort(data);

            percentiles = Arrays.stream(PERCENTILES)
                    .map(perc -> Math.round(perc * (data.size() - 1)))
                    .boxed()
                    .mapToInt(Double::intValue)
                    .mapToDouble(data::get)
                    .toArray();

            mean = 0.0;
            for (Double datum : data) {
                mean += datum;
            }
            mean /= data.size();

            variance = 0.0;
            for (Double datum : data) {
                variance += (datum - mean) * (datum - mean);
            }
            variance /= data.size();
            standardDeviation = Math.sqrt(variance);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Stats:\n");
            sb.append(">>    elements: ").append(data.size()).append("\n");
            sb.append(">>        mean: ").append(String.format("%.2f", mean)).append("\n");
            sb.append(">>         std: ").append(String.format("%.2f", standardDeviation)).append("\n");
            sb.append(">>    variance: ").append(String.format("%.2f", variance)).append("\n");
            sb.append(">> percentiles:\n");
            sb.append("  >> p000: ").append(String.format("%.2f", percentiles[0])).append("\n");
            sb.append("  >> p001: ").append(String.format("%.2f", percentiles[1])).append("\n");
            sb.append("  >> p010: ").append(String.format("%.2f", percentiles[2])).append("\n");
            sb.append("  >> p025: ").append(String.format("%.2f", percentiles[3])).append("\n");
            sb.append("  >> p050: ").append(String.format("%.2f", percentiles[4])).append("\n");
            sb.append("  >> p075: ").append(String.format("%.2f", percentiles[5])).append("\n");
            sb.append("  >> p090: ").append(String.format("%.2f", percentiles[6])).append("\n");
            sb.append("  >> p099: ").append(String.format("%.2f", percentiles[7])).append("\n");
            sb.append("  >> p100: ").append(String.format("%.2f", percentiles[8])).append("\n");
            return sb.toString();
        }
    }

    public static void simulate(int numIterations, int numRounds, double startingCapital,
            double minCapitalCap, double maxCapitalCap, double coinHeadProbability,
            InvestmentStrategy investmentStrategy) {
        Stats capitalStats = new Stats();
        Stats roundsStats = new Stats();
        Random random = new Random();
        double coinThreshold = 1.0 - coinHeadProbability;

        for (int i = 0; i < numIterations; ++i) {
            double capital = startingCapital;
            int round = 1;

            for (; round <= numRounds && capital > minCapitalCap && capital < maxCapitalCap; ++round) {
                double investment = Math.max(0, Math.min(investmentStrategy.invest(capital), capital));
                if (random.nextDouble() > coinThreshold) { // head
                    capital += investment;
                } else { // tails
                    capital -= investment;
                }
            }

            capital = Math.max(Math.min(capital, maxCapitalCap), minCapitalCap);
            capitalStats.add(capital);
            roundsStats.add(round);
        }

        capitalStats.compute();
        roundsStats.compute();

        System.out.print("Capital ");
        System.out.println(capitalStats);
        System.out.print("Round ");
        System.out.println(roundsStats);
    }

    public static void main(String[] args) {
        final Random random = new Random();
        InvestmentStrategy randomStrategy = capital -> random.nextDouble(0.0, capital + 0.00000001);
        Function<Integer, InvestmentStrategy> percentStrategy = percent -> capital -> percent * capital / 100.0;

        System.out.println("Random:");
        simulate(1_000_000, 100, 25, 0, 250, 0.6, randomStrategy);
        for (int i = 0; i <= 100; i += 5) {
            int percent = i > 0 ? i : 1;
            System.out.println("Percent " + percent + ":");
            simulate(1_000_000, 100, 25, 0, 250, 0.6, percentStrategy.apply(percent));
        }
    }
}
