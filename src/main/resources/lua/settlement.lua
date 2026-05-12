-- ============================================
-- 结算Lua脚本（实际扣费，退还多余预扣金额）
-- ============================================
-- 功能说明：
--   在AI模型调用完成后，根据实际使用的token数量计算真实费用
--   与预扣金额进行比较，退还多扣的部分到用户余额
--   确保计费的准确性和公平性
--
-- 业务流程：
--   1. 请求开始时，系统会预扣一定金额（通常是预估费用的120%）
--   2. 请求完成后，根据实际token使用量计算真实费用
--   3. 调用此脚本进行结算：
--      - 如果预扣 > 实际消费：退还差额到用户余额
--      - 如果预扣 < 实际消费：理论上不会发生（因为预扣比例>1）
--   4. 删除预扣记录，完成整个计费流程
--
-- 原子性保证：
--   - 使用Lua脚本确保余额查询、计算、更新的原子性
--   - 避免并发请求导致的余额计算错误
--   - 防止超额消费和余额负数问题
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
--            存储请求开始时预扣的金额，用于结算时对比
--   ARGV[1]: 用户ID，用于标识哪个用户的余额需要更新
--   ARGV[2]: 实际消费金额（字符串格式），根据实际token使用量计算得出
--            计算公式：(prompt_tokens + completion_tokens) * price_per_1k_tokens / 1000
--   ARGV[3]: 请求ID，唯一标识一次API调用，用于查找对应的预扣记录
--
-- 返回值说明：
--   返回一个数组：{成功标志, 新余额, 退还金额}
--   - 成功标志：1表示结算成功，0表示结算失败（预扣记录不存在）
--   - 新余额：结算后用户的最新余额（字符串格式，保留6位小数）
--   - 退还金额：预扣金额与实际消费的差额（字符串格式）
-- ============================================

-- 获取传入的参数
local balanceKey = KEYS[1]           -- 用户余额Key
local preDeductKey = KEYS[2]         -- 预扣记录Key
local userId = ARGV[1]               -- 用户ID
local actualCostStr = ARGV[2]        -- 实际消费金额（字符串格式）
local requestId = ARGV[3]            -- 请求ID

-- 第一步：获取预扣金额
-- 从Redis中读取请求开始时预扣的金额
local preDeductAmountStr = redis.call('GET', preDeductKey)
if preDeductAmountStr == false then
    -- 异常情况：预扣记录不存在
    -- 可能原因：
    --   1. 请求ID错误
    --   2. 预扣记录已过期被删除
    --   3. 重复调用结算接口
    -- 返回失败标志，余额和退还金额都为0
    return {0, "0", "0"}
end

-- 第二步：获取当前余额
-- 从Redis中读取用户的当前余额
local currentBalanceStr = redis.call('GET', balanceKey)
if currentBalanceStr == false then
    -- 如果余额Key不存在，视为0元
    -- 这种情况理论上不应该发生，因为用户注册时会初始化余额
    currentBalanceStr = "0"
end

-- 第三步：数据类型转换
-- 将字符串类型的金额转换为数字类型进行计算
local preDeductAmount = tonumber(preDeductAmountStr)  -- 预扣金额
local currentBalance = tonumber(currentBalanceStr)    -- 当前余额
local actualCost = tonumber(actualCostStr)            -- 实际消费金额

-- 第四步：计算差额
-- 差额 = 预扣金额 - 实际消费金额
-- 如果差额为正数，表示需要退还给用户
-- 如果差额为负数，表示预扣不足（理论上不应发生）
local difference = preDeductAmount - actualCost

-- 第五步：更新用户余额
-- 新余额 = 当前余额 + 差额（退还金额）
-- 例如：当前余额10元，预扣0.12元，实际消费0.10元，差额0.02元
--      新余额 = 10 + 0.02 = 10.02元
local newBalance = currentBalance + difference

-- 第六步：精度处理
-- 使用string.format保留6位小数，避免浮点数运算精度问题
-- 例如：0.1 + 0.2 = 0.30000000000000004（浮点数精度问题）
--      string.format("%.6f", 0.1 + 0.2) = "0.300000"（精确结果）
newBalance = string.format("%.6f", newBalance)

-- 第七步：保存新余额到Redis
-- SET命令会覆盖原有的余额值
redis.call('SET', balanceKey, newBalance)

-- 第八步：清理预扣记录
-- 结算完成后，删除预扣记录，释放Redis存储空间
-- DEL命令返回被删除的key数量
redis.call('DEL', preDeductKey)

-- 第九步：返回结算结果
-- 返回数组包含：成功标志、新余额、退还金额
-- tostring(difference)将差额转换为字符串格式
return {1, newBalance, tostring(difference)}
