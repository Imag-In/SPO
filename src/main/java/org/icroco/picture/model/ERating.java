package org.icroco.picture.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ERating {
    ABSENT(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final        short               code;
    private final static Map<Short, ERating> shortToRating = Arrays.stream(ERating.values())
                                                                   .collect(Collectors.toMap(ERating::getCode,
                                                                                             Function.identity()));

    private final static Map<String, ERating> starToRating = Map.ofEntries(Map.entry("*", ERating.ONE),
                                                                           Map.entry("**", ERating.TWO),
                                                                           Map.entry("***", ERating.THREE),
                                                                           Map.entry("****", ERating.FOUR),
                                                                           Map.entry("*****", ERating.FIVE));

    ERating(int code) {
        this.code = (short) code;
    }

    public static ERating fromCode(int code) {
        return shortToRating.getOrDefault((short) code, ERating.ABSENT);
    }

    public static ERating fromStars(String code) {
        return starToRating.getOrDefault(code, ERating.ABSENT);
    }
}
