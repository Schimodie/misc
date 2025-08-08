package org.schimodie.random;

import org.schimodie.common.data.Tuple2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RandomDailyOrder {
    private static final Random RANDOM = new Random();
    private static final String[] NAMES = {"Florin", "Masu", "Mihai", "Oly", "Robert", "Vlad"};
    private static final Set<String> NAMES_SET = new HashSet<>(Arrays.stream(NAMES).toList());
    private static final double[] PROBABILITY_TO_SHUFFLE_AWAY_BY_NPOS = {0.00, 0.05, 0.30, 0.70, 0.95, 1.00};
    private static final String PREVIOUS_ITERATIONS_FILE_PATH =
            "/Users/radug/db/random-daily-order/previous-iterations.csv";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00.00");

    private RandomDailyOrder() {
    }

    public static void main(String[] args) {
        run();
    }

    public static void analysis(String[] args) {
        int[] displacementCounts = new int[5];

        for (int numRuns = 1; numRuns <= 100; numRuns *= 10) {
            Arrays.fill(displacementCounts, 0);

            Map<String, int[]> freqs = new HashMap<>();
            for (int i = 0; i < numRuns; ++i) {
                Tuple2<int[], String[]> result = run();

                for (int displacement : result.t1()) {
                    ++displacementCounts[displacement];
                }

                for (int j = 0; j < result.t2().length; ++j) {
                    if (!freqs.containsKey(result.t2()[j])) {
                        freqs.put(result.t2()[j], new int[result.t2().length]);
                    }

                    ++freqs.get(result.t2()[j])[j];
                }
            }

            System.out.printf("runs: %3d, ", numRuns);
            for (int i = 0; i < displacementCounts.length; ++i) {
                System.out.printf("%2d: ", i);
                System.out.printf("%s, ",
                        DECIMAL_FORMAT.format((100.0d * displacementCounts[i]) / (NAMES.length * numRuns)));
            }
            System.out.println();

            for (var freq : freqs.entrySet()) {
                System.out.printf("%6s: ", freq.getKey());
                for (int i = 0; i < freq.getValue().length; ++i) {
                    System.out.printf("%d: %s, ", i, DECIMAL_FORMAT.format((100.0d * freq.getValue()[i]) / numRuns));
                }
                System.out.println();
            }
        }
    }

    public static Tuple2<int[], String[]> run() {
        String[] shuffle;
        String previousIteration = getPreviousIteration();

        if (previousIteration == null) {
            shuffle = new String[NAMES.length];
            System.arraycopy(NAMES, 0, shuffle, 0, NAMES.length);
        } else {
            shuffle = Arrays.stream(previousIteration.split(",")).filter(NAMES_SET::contains).toArray(String[]::new);
        }

        BitSet hasBeenUsed = new BitSet(shuffle.length);
        String[] newShuffle = new String[shuffle.length];
        int[] actualDisplacement = new int[shuffle.length];
        for (int position = 0; position < shuffle.length; ++position) {
            boolean hasBeenSet = false;
            while (!hasBeenSet) {
                int displacement = 0;
                double displacementProb = RANDOM.nextDouble();

                for (int j = 1; j < PROBABILITY_TO_SHUFFLE_AWAY_BY_NPOS.length; ++j) {
                    if (PROBABILITY_TO_SHUFFLE_AWAY_BY_NPOS[j - 1] <= displacementProb
                            && displacementProb <= PROBABILITY_TO_SHUFFLE_AWAY_BY_NPOS[j]) {
                        displacement = j - 1;
                        break;
                    }
                }

                int newPosition;
                if (RANDOM.nextBoolean()) {
                    newPosition = (shuffle.length + position + displacement) % shuffle.length;
                } else {
                    newPosition = (shuffle.length + position - displacement) % shuffle.length;
                }

                if (!hasBeenUsed.get(newPosition)) {
                    hasBeenSet = true;
                    newShuffle[newPosition] = shuffle[position];
                    actualDisplacement[position] = displacement;

                    hasBeenUsed.set(newPosition);
                }
            }
        }

        System.out.println(Arrays.toString(newShuffle));
        writeNextIteration(newShuffle);
        return Tuple2.of(actualDisplacement, newShuffle);
    }

    private static String getPreviousIteration() {
        try (BufferedReader reader = new BufferedReader(new FileReader(createPreviousIterationsFile()))) {
            String line = null;

            while (true) {
                String currentLine = reader.readLine();
                if (currentLine == null) {
                    break;
                }

                line = currentLine;
            }

            return line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeNextIteration(String[] nextIteration) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(createPreviousIterationsFile(), true))) {
            writer.write(String.join(",", nextIteration));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File createPreviousIterationsFile() throws IOException {
        File file = new File(PREVIOUS_ITERATIONS_FILE_PATH);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        return file;
    }
}
