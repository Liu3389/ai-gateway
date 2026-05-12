package com.ai.gateway.controller;

import com.ai.gateway.common.Result;
import com.ai.gateway.service.AnalyticsService;
import com.ai.gateway.vo.AdminAnalyticsVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    private Long getOperatorId(HttpServletRequest request) {
        return (Long) request.getAttribute("currentUserId");
    }

    @GetMapping("/full")
    public Result<AdminAnalyticsVO> getFullAnalytics(HttpServletRequest request) {
        return Result.success(analyticsService.getFullAnalytics(getOperatorId(request)));
    }

    @GetMapping("/daily-usage")
    public Result<List<AdminAnalyticsVO.DailyUsageVO>> getDailyUsage(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest request) {
        return Result.success(analyticsService.getDailyUsage(days));
    }

    @GetMapping("/model-usage")
    public Result<List<AdminAnalyticsVO.ModelUsageVO>> getModelUsage(HttpServletRequest request) {
        return Result.success(analyticsService.getModelUsage());
    }

    @GetMapping("/hourly-traffic")
    public Result<List<AdminAnalyticsVO.HourlyTrafficVO>> getHourlyTraffic(HttpServletRequest request) {
        return Result.success(analyticsService.getHourlyTraffic());
    }

    @GetMapping("/top-users")
    public Result<List<AdminAnalyticsVO.TopUserVO>> getTopUsers(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        return Result.success(analyticsService.getTopUsers(limit));
    }

    @GetMapping("/revenue-trend")
    public Result<List<AdminAnalyticsVO.RevenueTrendVO>> getRevenueTrend(
            @RequestParam(defaultValue = "12") int months,
            HttpServletRequest request) {
        return Result.success(analyticsService.getRevenueTrend(months));
    }

    @GetMapping("/user-growth")
    public Result<List<AdminAnalyticsVO.UserGrowthVO>> getUserGrowth(
            @RequestParam(defaultValue = "12") int months,
            HttpServletRequest request) {
        return Result.success(analyticsService.getUserGrowth(months));
    }
}
