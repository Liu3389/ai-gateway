#!/bin/bash

# 管理员功能测试脚本
# 使用前请确保应用已启动且数据库已迁移

BASE_URL="http://localhost:8080"

echo "========================================="
echo "管理员后台系统功能测试"
echo "========================================="
echo ""

# 1. 超级管理员登录
echo "1. 超级管理员登录..."
SUPER_ADMIN_LOGIN=$(curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}')

echo $SUPER_ADMIN_LOGIN | python3 -m json.tool
SUPER_ADMIN_ID=$(echo $SUPER_ADMIN_LOGIN | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['userInfo']['id'])")
echo "超级管理员ID: $SUPER_ADMIN_ID"
echo ""

# 2. 普通用户注册
echo "2. 注册普通用户..."
REGISTER_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123","email":"test@example.com"}')

echo $REGISTER_RESPONSE | python3 -m json.tool
TEST_USER_ID=$(echo $REGISTER_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['id'])")
echo "测试用户ID: $TEST_USER_ID"
echo ""

# 3. 分配管理员角色
echo "3. 为测试用户分配管理员角色..."
ASSIGN_RESPONSE=$(curl -s -X POST ${BASE_URL}/admin/assign-role \
  -H "X-User-Id: ${SUPER_ADMIN_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${TEST_USER_ID},\"role\":\"ADMIN\"}")

echo $ASSIGN_RESPONSE | python3 -m json.tool
echo ""

# 4. 设置完全免费策略
echo "4. 为测试用户设置完全免费策略..."
FREE_STRATEGY_RESPONSE=$(curl -s -X POST ${BASE_URL}/admin/set-free-strategy \
  -H "X-User-Id: ${SUPER_ADMIN_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${TEST_USER_ID},\"freeApiStrategy\":\"UNLIMITED\"}")

echo $FREE_STRATEGY_RESPONSE | python3 -m json.tool
echo ""

# 5. 查看所有用户
echo "5. 查看所有用户..."
USERS_RESPONSE=$(curl -s -X GET ${BASE_URL}/admin/users \
  -H "X-User-Id: ${SUPER_ADMIN_ID}")

echo $USERS_RESPONSE | python3 -m json.tool
echo ""

# 6. 查看特定用户详情
echo "6. 查看测试用户详情..."
USER_DETAIL_RESPONSE=$(curl -s -X GET ${BASE_URL}/admin/user/${TEST_USER_ID} \
  -H "X-User-Id: ${SUPER_ADMIN_ID}")

echo $USER_DETAIL_RESPONSE | python3 -m json.tool
echo ""

# 7. 获取所有管理员列表
echo "7. 获取所有管理员列表..."
ADMINS_RESPONSE=$(curl -s -X GET ${BASE_URL}/admin/admins \
  -H "X-User-Id: ${SUPER_ADMIN_ID}")

echo $ADMINS_RESPONSE | python3 -m json.tool
echo ""

# 8. 取消管理员角色
echo "8. 取消测试用户的管理员角色..."
REVOKE_RESPONSE=$(curl -s -X POST ${BASE_URL}/admin/revoke-role \
  -H "X-User-Id: ${SUPER_ADMIN_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${TEST_USER_ID}}")

echo $REVOKE_RESPONSE | python3 -m json.tool
echo ""

# 9. 禁用用户
echo "9. 禁用测试用户..."
DISABLE_RESPONSE=$(curl -s -X POST ${BASE_URL}/admin/user/status \
  -H "X-User-Id: ${SUPER_ADMIN_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${TEST_USER_ID},\"status\":0}")

echo $DISABLE_RESPONSE | python3 -m json.tool
echo ""

# 10. 重新启用用户
echo "10. 重新启用测试用户..."
ENABLE_RESPONSE=$(curl -s -X POST ${BASE_URL}/admin/user/status \
  -H "X-User-Id: ${SUPER_ADMIN_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":${TEST_USER_ID},\"status\":1}")

echo $ENABLE_RESPONSE | python3 -m json.tool
echo ""

echo "========================================="
echo "测试完成！"
echo "========================================="
