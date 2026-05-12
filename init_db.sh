#!/bin/bash

# ============================================
# AI Gateway Platform - 数据库初始化脚本
# ============================================
# 功能说明：
#   1. 创建数据库
#   2. 导入建表 SQL 脚本
#   3. 验证表结构和初始数据
# 使用方法：
#   ./init_db.sh
# 注意事项：
#   - 确保 MySQL 服务已启动
#   - 确保可以连接到配置的 MySQL 服务器
#   - 首次运行或需要重置数据库时使用
# ============================================

# 数据库配置（从 application.yml 读取或使用默认值）
VM_IP="10.211.55.10"
MYSQL_USER="root"
MYSQL_PASS="123456"
DB_NAME="ai_gateway"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="$SCRIPT_DIR/src/main/resources/sql/schema.sql"
TEST_DATA_FILE="$SCRIPT_DIR/testdata.sql"

echo "========================================="
echo "AI Gateway Platform - 数据库初始化"
echo "========================================="

# 检查 SQL 文件是否存在
echo ""
echo "1. 检查 SQL 文件..."
if [ ! -f "$SQL_FILE" ]; then
    echo "❌ SQL 文件不存在: $SQL_FILE"
    exit 1
fi
echo "✅ SQL 文件存在: $SQL_FILE"

# 测试 MySQL 连接
echo ""
echo "2. 测试 MySQL 连接..."
if ! mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -e "SELECT 1;" > /dev/null 2>&1; then
    echo "❌ MySQL 连接失败"
    echo "   请检查："
    echo "   1. MySQL 服务是否运行"
    echo "   2. 防火墙是否开放 3306 端口"
    echo "   3. 用户名和密码是否正确"
    echo "   4. MySQL 是否允许远程访问"
    exit 1
fi
echo "✅ MySQL 连接成功"

# 创建数据库
echo ""
echo "3. 创建数据库..."
mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ 数据库 '$DB_NAME' 创建成功"
else
    echo "❌ 数据库创建失败"
    exit 1
fi

# 导入建表脚本
echo ""
echo "4. 导入建表脚本..."
echo "   SQL 文件: $SQL_FILE"
mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS $DB_NAME < "$SQL_FILE" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ 建表脚本导入成功"
else
    echo "❌ 建表脚本导入失败"
    exit 1
fi

# 验证表结构
echo ""
echo "5. 验证表结构..."
TABLE_COUNT=$(mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -N -e "USE $DB_NAME; SHOW TABLES;" 2>/dev/null | wc -l)

if [ "$TABLE_COUNT" -eq 5 ]; then
    echo "✅ 所有表已创建（5个表）"
    echo ""
    echo "表列表："
    mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -e "USE $DB_NAME; SHOW TABLES;" 2>/dev/null | grep -v "Tables_in" | sed 's/^/  - /'
else
    echo "❌ 表数量不正确：$TABLE_COUNT（应该是5个）"
    echo "   建议删除数据库后重新运行此脚本"
    exit 1
fi

# 询问是否插入测试数据
echo ""
read -p "是否插入测试数据？(y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -f "$TEST_DATA_FILE" ]; then
        echo ""
        echo "6. 插入测试数据..."
        mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS $DB_NAME < "$TEST_DATA_FILE" 2>/dev/null
        
        if [ $? -eq 0 ]; then
            echo "✅ 测试数据插入成功"
        else
            echo "⚠️  测试数据插入失败，但不影响使用"
        fi
    else
        echo "⚠️  测试数据文件不存在: $TEST_DATA_FILE"
    fi
fi

# 验证初始数据
echo ""
echo "7. 验证初始数据..."
MODEL_COUNT=$(mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -N -e "USE $DB_NAME; SELECT COUNT(*) FROM model_config;" 2>/dev/null)

if [ "$MODEL_COUNT" -ge 1 ]; then
    echo "✅ 模型配置数据已插入（$MODEL_COUNT 条记录）"
    echo ""
    echo "模型列表："
    mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -e "USE $DB_NAME; SELECT model_name, provider, status FROM model_config;" 2>/dev/null | tail -n +2 | sed 's/^/  - /'
else
    echo "⚠️  模型配置数据为空"
    echo "   如需测试对话功能，请手动插入模型配置"
fi

# 显示用户账号信息
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "========================================="
    echo "测试账号信息（密码统一为 admin123）："
    echo "========================================="
    mysql -h $VM_IP -u $MYSQL_USER -p$MYSQL_PASS -e "USE $DB_NAME; SELECT username, role FROM user WHERE role != 'USER';" 2>/dev/null | tail -n +2 | sed 's/^/  - /'
fi

echo ""
echo "========================================="
echo "✅ 数据库初始化完成！"
echo "========================================="
echo ""
echo "下一步："
echo "  1. 检查环境: ./check_env.sh"
echo "  2. 启动应用: ./start.sh"
echo ""
