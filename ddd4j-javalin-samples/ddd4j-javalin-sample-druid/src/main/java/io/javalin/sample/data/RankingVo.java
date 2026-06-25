package io.javalin.sample.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RankingVo {
    private Long uid; //user id
    private String ct; //国家
    private BigDecimal coin;
}
