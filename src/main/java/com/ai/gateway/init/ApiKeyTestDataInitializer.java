package com.ai.gateway.init;

import com.ai.gateway.entity.ApiKey;
import com.ai.gateway.mapper.ApiKeyMapper;
import com.ai.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyTestDataInitializer {

    private final ApiKeyMapper apiKeyMapper;
    private final TokenService tokenService;

    @Value("${app.init-test-data:false}")
    private boolean initTestData;

    @PostConstruct
    public void init() {
        if (!initTestData) {
            return;
        }

        Long count = apiKeyMapper.selectCount(null);
        if (count != null && count > 0) {
            log.info("api_key 表已有 {} 条数据，跳过初始化", count);
            return;
        }

        log.info("开始初始化 api_key 测试数据（AES加密）...");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Object[]> keys = Arrays.asList(
            new Object[]{1L,  "sk-adm-00000000000000000000000000000001", 1L,  "超管默认Key",     1, 9999, "2026-01-15 08:01:00"},
            new Object[]{2L,  "sk-adm-00000000000000000000000000000002", 2L,  "管理员默认Key",    1, 5000, "2026-01-15 08:06:00"},
            new Object[]{3L,  "sk-usr-00000000000000000000000000000003", 3L,  "张三工作Key",      1, 500,  "2026-02-01 10:01:00"},
            new Object[]{4L,  "sk-usr-00000000000000000000000000000004", 3L,  "张三家庭Key",      1, 200,  "2026-02-10 10:00:00"},
            new Object[]{5L,  "sk-usr-00000000000000000000000000000005", 4L,  "李四Key",          1, 500,  "2026-02-02 11:01:00"},
            new Object[]{6L,  "sk-usr-00000000000000000000000000000006", 5L,  "王五Key",          1, 500,  "2026-02-03 12:01:00"},
            new Object[]{7L,  "sk-usr-00000000000000000000000000000007", 6L,  "赵六Key",          1, 100,  "2026-03-01 09:01:00"},
            new Object[]{8L,  "sk-usr-00000000000000000000000000000008", 7L,  "孙七Key",          0, 100,  "2026-03-05 10:01:00"},
            new Object[]{9L,  "sk-usr-00000000000000000000000000000009", 8L,  "周八Key",          1, 100,  "2026-03-10 14:01:00"},
            new Object[]{10L, "sk-usr-00000000000000000000000000000010", 9L,  "吴九Key",          1, 100,  "2026-03-15 16:01:00"},
            new Object[]{11L, "sk-usr-00000000000000000000000000000011", 10L, "郑十Key",          1, 100,  "2026-03-20 08:01:00"},
            new Object[]{12L, "sk-usr-00000000000000000000000000000012", 11L, "AliceKey",         1, 50,   "2026-04-01 09:01:00"},
            new Object[]{13L, "sk-usr-00000000000000000000000000000013", 12L, "BobKey",           1, 50,   "2026-04-02 10:01:00"},
            new Object[]{14L, "sk-usr-00000000000000000000000000000014", 13L, "CharlieKey",       1, 50,   "2026-04-03 11:01:00"},
            new Object[]{15L, "sk-usr-00000000000000000000000000000015", 14L, "DavidKey",         1, 50,   "2026-04-04 12:01:00"},
            new Object[]{16L, "sk-usr-00000000000000000000000000000016", 15L, "EveKey",           1, 50,   "2026-04-05 13:01:00"},
            new Object[]{17L, "sk-usr-00000000000000000000000000000017", 16L, "low1Key",          1, 30,   "2026-04-10 08:01:00"},
            new Object[]{18L, "sk-usr-00000000000000000000000000000018", 17L, "low2Key",          1, 30,   "2026-04-11 09:01:00"},
            new Object[]{19L, "sk-usr-00000000000000000000000000000019", 18L, "low3Key",          1, 30,   "2026-04-12 10:01:00"},
            new Object[]{20L, "sk-usr-00000000000000000000000000000020", 19L, "low4Key",          1, 30,   "2026-04-13 11:01:00"},
            new Object[]{21L, "sk-usr-00000000000000000000000000000021", 20L, "new1Key",          1, 50,   "2026-05-09 08:01:00"},
            new Object[]{22L, "sk-usr-00000000000000000000000000000022", 21L, "new2Key",          1, 50,   "2026-05-10 09:01:00"},
            new Object[]{23L, "sk-usr-00000000000000000000000000000023", 22L, "new3Key",          1, 50,   "2026-05-11 10:01:00"},
            new Object[]{24L, "sk-usr-00000000000000000000000000000024", 23L, "new4Key",          1, 50,   "2026-05-12 11:01:00"},
            new Object[]{25L, "sk-usr-00000000000000000000000000000025", 24L, "new5Key",          1, 50,   "2026-05-13 12:01:00"},
            new Object[]{26L, "sk-usr-00000000000000000000000000000026", 25L, "new6Key",          1, 50,   "2026-05-14 13:01:00"},
            new Object[]{27L, "sk-usr-00000000000000000000000000000027", 26L, "new7Key",          1, 50,   "2026-05-15 08:01:00"},
            new Object[]{28L, "sk-usr-00000000000000000000000000000028", 27L, "new8Key",          1, 50,   "2026-05-15 09:01:00"},
            new Object[]{29L, "sk-usr-00000000000000000000000000000029", 30L, "cus001Key",        1, 50,   "2026-04-15 08:01:00"},
            new Object[]{30L, "sk-usr-00000000000000000000000000000030", 31L, "cus002Key",        1, 50,   "2026-04-16 09:01:00"},
            new Object[]{31L, "sk-usr-00000000000000000000000000000031", 35L, "cus006Key",        1, 100,  "2026-04-20 13:01:00"},
            new Object[]{32L, "sk-usr-00000000000000000000000000000032", 36L, "cus007Key",        1, 500,  "2026-04-21 14:01:00"},
            new Object[]{33L, "sk-usr-00000000000000000000000000000033", 38L, "cus009Key",        1, 500,  "2026-04-23 16:01:00"},
            new Object[]{34L, "sk-usr-00000000000000000000000000000034", 45L, "cus016Key",        1, 500,  "2026-04-30 13:01:00"},
            new Object[]{35L, "sk-usr-00000000000000000000000000000035", 50L, "cus021Key",        1, 500,  "2026-05-05 08:01:00"}
        );

        for (Object[] row : keys) {
            try {
                Long id = (Long) row[0];
                String rawKey = (String) row[1];
                Long userId = (Long) row[2];
                String name = (String) row[3];
                Integer status = (Integer) row[4];
                Integer rateLimit = (Integer) row[5];
                LocalDateTime createTime = LocalDateTime.parse((String) row[6], fmt);

                ApiKey entity = new ApiKey();
                entity.setId(id);
                entity.setApiKey(tokenService.encryptApiKey(rawKey));
                entity.setUserId(userId);
                entity.setName(name);
                entity.setStatus(status);
                entity.setRateLimit(rateLimit);
                entity.setCreateTime(createTime);
                apiKeyMapper.insert(entity);
            } catch (Exception e) {
                log.error("插入 api_key 失败: id={}", row[0], e);
            }
        }
        log.info("api_key 测试数据初始化完成，共 {} 条", keys.size());
    }
}
