package com.dont.groovy.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TimeUtils {

    private long milliseconds;
    private String and = "and";
    private final Map<int[], Function<Integer, String>> functions = new LinkedHashMap<>();

    private TimeUtils(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public static TimeUtils of(long milliseconds) {
        return new TimeUtils(Math.max(0, milliseconds));
    }

    public static TimeUtils of() {
        return of(0L);
    }

    public static String format(long millis) {
        return TimeUtils.of(millis)
                .day(i -> i + (i == 1 ? " dia" : " dias"))
                .hour(i -> i + (i == 1 ? " hora" : " horas"))
                .min(i -> i + (i == 1 ? " minuto" : " minutos"))
                .sec(i -> i + (i == 1 ? " segundo" : " segundos"))
                .and("e").get();
    }

    public static String measureTime(Supplier<String> runnable) {
        long before = System.currentTimeMillis();
        String task = runnable.get();
        long passed = System.currentTimeMillis() - before;
        return (task.replace("{time}", passed + "ms"));
    }

    public static long measureTime(Runnable runnable) {
        long before = System.currentTimeMillis();
        runnable.run();
        return System.currentTimeMillis() - before;
    }

    public TimeUtils and(String and) {
        this.and = and;
        return this;
    }

    public TimeUtils millis(long millis) {
        this.milliseconds = millis;
        Map<int[], Function<Integer, String>> clone = new LinkedHashMap<>(functions);
        clone.forEach((k, v) -> {
            switch (k[0]) {
                case 3:
                    sec(v);
                    break;
                case 2:
                    min(v);
                    break;
                case 1:
                    hour(v);
                    break;
                case 0:
                    day(v);
                    break;
            }
        });

        return this;
    }

    public TimeUtils sec(Function<Integer, String> fun) {
        functions.keySet().removeIf(ints -> ints[0] == 3);
        functions.put(new int[]{3, (int) (milliseconds / 1000) % 60}, fun);
        return this;
    }

    public TimeUtils min(Function<Integer, String> fun) {
        functions.keySet().removeIf(ints -> ints[0] == 2);
        functions.put(new int[]{2, (int) ((milliseconds / (1000 * 60)) % 60)}, fun);
        return this;
    }

    public TimeUtils hour(Function<Integer, String> fun) {
        functions.keySet().removeIf(ints -> ints[0] == 1);
        functions.put(new int[]{1, (int) ((milliseconds / (1000 * 60 * 60)) % 24)}, fun);
        return this;
    }

    public TimeUtils day(Function<Integer, String> fun) {
        functions.keySet().removeIf(ints -> ints[0] == 0);
        functions.put(new int[]{0, (int) ((milliseconds / (1000 * 60 * 60 * 24)))}, fun);
        return this;
    }

    public String get() {
        int len = functions.entrySet().stream().filter(e -> e.getKey()[1] > 0).collect(Collectors.toList()).size();
        if (len == 0) return String.format("%.2f", (float) milliseconds) + "ms";

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<int[], Function<Integer, String>> e : functions.entrySet()) {
            Function<Integer, String> v = e.getValue();
            if (e.getKey()[1] == 0) continue; // fix zeros
            String f = v.apply(e.getKey()[1]);
            if (len == 1)
                sb.append(f).append(" ");
            else if (i == len - 1)
                sb.append(" ").append(and).append(" ").append(f);
            else {
                sb.append(f);
                if (i != len - 2) sb.append(",").append(" ");
            }
            i++;
        }

        return sb.toString();
    }

}