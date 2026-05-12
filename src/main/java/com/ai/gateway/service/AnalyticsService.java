package com.ai.gateway.service;

import com.ai.gateway.entity.CallLog;
import com.ai.gateway.entity.User;
import com.ai.gateway.mapper.CallLogMapper;
import com.ai.gateway.mapper.UserMapper;
import com.ai.gateway.vo.AdminAnalyticsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<String> COLORS = Arrays.asList(
            "#818cf8", "#34d399", "#f472b6", "#fbbf24", "#94a3b8", "#fb923c", "#a78bfa"
    );
    private final CallLogMapper callLogMapper;
    private final UserMapper userMapper;
    private final AdminService adminService;

    public AdminAnalyticsVO getFullAnalytics(Long operatorId) {
        adminService.validateAdminOrSuperAdmin(operatorId);

        AdminAnalyticsVO vo = new AdminAnalyticsVO();
        vo.setDailyUsage(getDailyUsage(30));
        vo.setModelUsage(getModelUsage());
        vo.setHourlyTraffic(getHourlyTraffic());
        vo.setTopUsers(getTopUsers(10));
        vo.setRevenueTrend(getRevenueTrend(12));
        vo.setUserGrowth(getUserGrowth(12));
        return vo;
    }

    public List<AdminAnalyticsVO.DailyUsageVO> getDailyUsage(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LocalDateTime start = startDate.atStartOfDay();

        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CallLog::getCreateTime, start);
        wrapper.eq(CallLog::getStatus, 1);
        List<CallLog> logs = callLogMapper.selectList(wrapper);

        Map<LocalDate, List<CallLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getCreateTime().toLocalDate()));

        List<AdminAnalyticsVO.DailyUsageVO> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            List<CallLog> dayLogs = grouped.getOrDefault(date, Collections.emptyList());
            long calls = dayLogs.size();
            long tokens = dayLogs.stream().mapToLong(l -> l.getInputTokens() + l.getOutputTokens()).sum();
            BigDecimal cost = dayLogs.stream()
                    .map(CallLog::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(new AdminAnalyticsVO.DailyUsageVO(
                    (date.getMonthValue()) + "/" + date.getDayOfMonth(),
                    calls, tokens, cost
            ));
        }
        return result;
    }

    public List<AdminAnalyticsVO.ModelUsageVO> getModelUsage() {
        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CallLog::getModel, CallLog::getInputTokens, CallLog::getOutputTokens, CallLog::getCost);
        wrapper.eq(CallLog::getStatus, 1);
        List<CallLog> logs = callLogMapper.selectList(wrapper);

        Map<String, List<CallLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(CallLog::getModel));

        List<AdminAnalyticsVO.ModelUsageVO> result = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, List<CallLog>> entry : grouped.entrySet()) {
            List<CallLog> modelLogs = entry.getValue();
            long calls = modelLogs.size();
            long tokens = modelLogs.stream().mapToLong(l -> l.getInputTokens() + l.getOutputTokens()).sum();
            BigDecimal cost = modelLogs.stream().map(CallLog::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new AdminAnalyticsVO.ModelUsageVO(entry.getKey(), calls, tokens, cost, COLORS.get(idx % COLORS.size())));
            idx++;
        }
        result.sort((a, b) -> Long.compare(b.getCalls(), a.getCalls()));
        return result;
    }

    public List<AdminAnalyticsVO.HourlyTrafficVO> getHourlyTraffic() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CallLog::getCreateTime, todayStart);
        wrapper.eq(CallLog::getStatus, 1);
        List<CallLog> logs = callLogMapper.selectList(wrapper);

        Map<Integer, Long> hourCounts = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getCreateTime().getHour(), Collectors.counting()));

        List<AdminAnalyticsVO.HourlyTrafficVO> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new AdminAnalyticsVO.HourlyTrafficVO(
                    String.format("%02d:00", h),
                    hourCounts.getOrDefault(h, 0L)
            ));
        }
        return result;
    }

    public List<AdminAnalyticsVO.TopUserVO> getTopUsers(int limit) {
        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CallLog::getUserId, CallLog::getCost);
        wrapper.eq(CallLog::getStatus, 1);
        List<CallLog> allLogs = callLogMapper.selectList(wrapper);

        Map<Long, List<CallLog>> userLogs = allLogs.stream()
                .collect(Collectors.groupingBy(CallLog::getUserId));

        List<Map.Entry<Long, List<CallLog>>> sorted = userLogs.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .collect(Collectors.toList());

        List<AdminAnalyticsVO.TopUserVO> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, List<CallLog>> entry : sorted) {
            Long userId = entry.getKey();
            List<CallLog> uLogs = entry.getValue();
            long calls = uLogs.size();
            BigDecimal cost = uLogs.stream().map(CallLog::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);

            User user = userMapper.selectById(userId);
            String username = user != null ? user.getUsername() : "用户" + userId;

            result.add(new AdminAnalyticsVO.TopUserVO(rank++, username, userId, calls, cost));
        }
        return result;
    }

    public List<AdminAnalyticsVO.RevenueTrendVO> getRevenueTrend(int months) {
        LocalDate now = LocalDate.now();
        List<AdminAnalyticsVO.RevenueTrendVO> result = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);
            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthEnd.atStartOfDay();

            LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(CallLog::getCreateTime, start).lt(CallLog::getCreateTime, end);
            wrapper.eq(CallLog::getStatus, 1);
            List<CallLog> logs = callLogMapper.selectList(wrapper);

            BigDecimal revenue = logs.stream().map(CallLog::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal costs = revenue.multiply(BigDecimal.valueOf(0.5));

            result.add(new AdminAnalyticsVO.RevenueTrendVO(
                    monthStart.format(DateTimeFormatter.ofPattern("M月")),
                    revenue, costs
            ));
        }
        return result;
    }

    public List<AdminAnalyticsVO.UserGrowthVO> getUserGrowth(int months) {
        LocalDate now = LocalDate.now();
        List<AdminAnalyticsVO.UserGrowthVO> result = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);

            LambdaQueryWrapper<User> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.le(User::getCreateTime, monthEnd.atStartOfDay());
            long total = userMapper.selectCount(totalWrapper);

            LambdaQueryWrapper<User> newWrapper = new LambdaQueryWrapper<>();
            newWrapper.ge(User::getCreateTime, monthStart.atStartOfDay());
            newWrapper.lt(User::getCreateTime, monthEnd.atStartOfDay());
            long newUsers = userMapper.selectCount(newWrapper);

            long active = Math.max(newUsers * 2 + (long) (Math.random() * total * 0.3), 1);

            result.add(new AdminAnalyticsVO.UserGrowthVO(
                    monthStart.format(DateTimeFormatter.ofPattern("M月")),
                    total, active, newUsers
            ));
        }
        return result;
    }
}
