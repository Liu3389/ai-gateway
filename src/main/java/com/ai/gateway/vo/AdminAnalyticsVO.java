package com.ai.gateway.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminAnalyticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<DailyUsageVO> dailyUsage;
    private List<ModelUsageVO> modelUsage;
    private List<HourlyTrafficVO> hourlyTraffic;
    private List<TopUserVO> topUsers;
    private List<RevenueTrendVO> revenueTrend;
    private List<UserGrowthVO> userGrowth;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsageVO implements Serializable {
        private String date;
        private Long calls;
        private Long tokens;
        private BigDecimal cost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelUsageVO implements Serializable {
        private String model;
        private Long calls;
        private Long tokens;
        private BigDecimal cost;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyTrafficVO implements Serializable {
        private String hour;
        private Long calls;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUserVO implements Serializable {
        private Integer rank;
        private String username;
        private Long userId;
        private Long calls;
        private BigDecimal cost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueTrendVO implements Serializable {
        private String month;
        private BigDecimal revenue;
        private BigDecimal costs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowthVO implements Serializable {
        private String month;
        private Long total;
        private Long active;

        @JsonProperty("new")
        private Long newUsers;
    }
}
