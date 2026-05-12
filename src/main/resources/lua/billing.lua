-- ============================================
-- 计费Lua脚本（预扣余额+原子性保证）
-- ============================================
-- 功能说明：
--   在AI模型调用开始前，预先扣除用户余额，防止超额消费
--   采用预扣费机制，确保用户有足够的余额支付API调用费用
--   使用Redis原子操作保证并发场景下的数据一致性
--
-- 业务流程：
--   1. 检查是否已经预扣过（幂等性检查，避免重复扣费）
--   2. 获取用户当前余额
--   3. 验证余额是否充足
--   4. 执行预扣操作，更新余额
--   5. 记录预扣信息，用于后续结算或回滚
--
-- 幂等性设计：
--   - 使用requestId作为唯一标识，防止同一请求重复预扣
--   - 如果已存在预扣记录，直接返回成功，不重复扣费
--   - 保证在网络重试等异常场景下的数据一致性
--
-- 精度处理：
--   - 所有金额计算保留6位小数
--   - 使用string.format避免浮点数精度丢失
--   - 确保财务计算的准确性
--
-- 参数说明：
--   KEYS[1]: 用户余额Key，格式为 ai_gateway:user_balance:{userId}
--            存储用户的当前可用余额
--   KEYS[2]: 预扣记录Key，格式为 ai_gateway:billing_pre_deduct:{requestId}
--            存储本次请求的预扣金额，用于幂等性检查和后续结算
--   ARGV[1]: 用户ID，用于标识哪个用户的余额需要扣减
--   ARGV[2]: 预扣金额（字符串格式），根据预估token使用量计算得出
--            计算公式：预估tokens * price_per_1k_tokens / 1000 * pre_deduct_ratio
--            其中pre_deduct_ratio通常为1.2（预扣120%）
--   ARGV[3]: 请求ID（UUID格式），唯一标识一次API调用
--            用于幂等性检查和后续结算流程
--   ARGV[4]: 过期时间（秒），预扣记录的有效期
--            超过此时间未结算，预扣记录会自动删除
--
-- 返回值说明：
--   返回一个数组：{成功标志, 余额信息}
--   - 成功标志：1表示预扣成功，0表示预扣失败
--   - 余额信息：预扣成功时为新余额，失败时为当前余额或错误信息
-- ============================================

-- 获取传入的参数
local balanceKey = KEYS[1]              -- 用户余额Key
local preDeductKey = KEYS[2]            -- 预扣记录Key
local userId = ARGV[1]                  -- 用户ID
local preDeductAmountStr = ARGV[2]      -- 预扣金额（字符串格式）
local requestId = ARGV[3]               -- 请求ID（用于幂等性）
local expireTime = tonumber(ARGV[4])    -- 过期时间（转换为数字类型）

-- 第一步：幂等性检查
-- 检查是否已经为该请求ID创建过预扣记录
-- 如果存在，说明已经预扣过，直接返回成功，避免重复扣费
local existingPreDeduct = redis.call('GET', preDeductKey)
if existingPreDeduct ~= false then
    -- 已存在预扣记录，说明之前已经成功预扣
    -- 返回成功标志和当前余额（不重复扣费）
    return {1, redis.call('GET', balanceKey)}
end

-- 第二步：获取当前余额
-- 从Redis中读取用户的当前可用余额
local currentBalanceStr = redis.call('GET', balanceKey)
if currentBalanceStr == false then
    -- 余额Key不存在，视为用户余额为0
    -- 这种情况可能发生在：
    --   1. 新用户尚未初始化余额
    --   2. Redis数据异常丢失
    -- 返回失败标志和"0"表示余额不足
    return {0, "0"}
end

-- 第三步：数据类型转换
-- 将字符串类型的金额转换为数字类型进行比较和计算
-- tonumber函数会将字符串"0.123"转换为数字0.123
local currentBalance = tonumber(currentBalanceStr)     -- 当前余额
local preDeductAmount = tonumber(preDeductAmountStr)   -- 预扣金额

-- 第四步：余额充足性检查
-- 比较当前余额和预扣金额，判断是否有足够的余额
if currentBalance < preDeductAmount then
    -- 余额不足，无法完成预扣
    -- 返回失败标志和当前余额字符串
    -- 前端可以根据返回的余额提示用户充值
    return {0, currentBalanceStr}
end

-- 第五步：执行预扣操作
-- 计算预扣后的新余额：新余额 = 当前余额 - 预扣金额
local newBalance = currentBalance - preDeductAmount

-- 第六步：精度处理
-- 使用string.format保留6位小数，避免浮点数运算精度问题
-- 例如：10.0 - 0.12 = 9.880000000000001（浮点数精度问题）
--      string.format("%.6f", 10.0 - 0.12) = "9.880000"（精确结果）
newBalance = string.format("%.6f", newBalance)

-- 第七步：更新用户余额
-- 将计算后的新余额写回Redis，覆盖原有余额
-- SET命令会原子性地更新键值
redis.call('SET', balanceKey, newBalance)

-- 第八步：记录预扣信息
-- 在Redis中创建预扣记录，存储预扣金额
-- SET key value EX seconds - 设置键值对并指定过期时间（秒）
-- 预扣记录的作用：
--   1. 幂等性检查：防止同一请求重复预扣
--   2. 结算依据：后续根据预扣金额和实际消费进行结算
--   3. 回滚参考：如果请求失败，可以根据预扣记录回滚余额
redis.call('SET', preDeductKey, preDeductAmountStr, 'EX', expireTime)

-- 第九步：返回预扣结果
-- 返回成功标志（1）和预扣后的新余额
-- 调用方可以根据返回值判断预扣是否成功
return {1, newBalance}
