#!/bin/bash

BACKEND_URL="http://localhost:8080"
USER_ID="1"

echo "=== 步骤1: 获取报告列表 ==="
curl -s "${BACKEND_URL}/api/v1/reports?userId=${USER_ID}&page=1&pageSize=10" | python3 -m json.tool > reports_list.json

echo "=== 步骤2: 解析 reportNo..."
REPORT_NOS=($(python3 -c "
import json
with open('reports_list.json', 'r') as f:
    data = json.load(f)
items = data.get('data', {}).get('items', [])
for item in items:
    print(item.get('reportNo'))
"))

echo -e "\n=== 步骤3: 获取每个报告的详情 ==="
for REPORT_NO in "${REPORT_NOS[@]}
do
    echo -e "\n--- 报告编号: $REPORT_NO"
    curl -s "${BACKEND_URL}/api/v1/reports/${REPORT_NO}?userId=${USER_ID}" | python3 -m json.tool
done
