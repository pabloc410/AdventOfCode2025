package software.ulpgc.aoc.day02.control;

import java.util.function.LongPredicate;


public class ValidationStrategies {

    public static final LongPredicate PATTERN_A =
            id -> !Long.toString(id).matches("^(\\d+)\\1$"); //true si NO la encuentra

    public static final LongPredicate PATTERN_B =
            id -> !Long.toString(id).matches("^(\\d+)\\1+$");
}
