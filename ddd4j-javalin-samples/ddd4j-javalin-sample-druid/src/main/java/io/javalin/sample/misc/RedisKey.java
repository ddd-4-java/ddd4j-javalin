package io.javalin.sample.misc;

public class RedisKey {
    public static String countryRank(String country) {
        return String.format("task:rank:country:%s", country);
    }

    public static String bestCountry() {
        return "task:rank:bestCountry";
    }

    public static String localRank() {
        return "task:rank:local";
    }

    public static String globalRank() {
        return "task:rank:global";
    }

    public static String tradingTaskTs() {
        return "task:ts:trading";
    }

    public static String inviteTaskTs() {
        return "task:ts:invite";
    }

    public static String localChampionTaskTs() {
        return "task:ts:localChampion";
    }

    public static String userRank(Long userId) {
        return String.format("task:rank:user:%s", userId);
    }
}
