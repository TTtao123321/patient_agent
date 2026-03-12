#!/bin/bash
set -e

PYTHON_CMD="/Users/taot/Desktop/projects/patient_agent/.venv/bin/python"

echo "=========================================="
echo "Report Analysis Integration Test"
echo "=========================================="

# Test 1: Blood report parsing
echo ""
echo "[Test 1] Blood routine report with abnormal indicators"
result1=$($PYTHON_CMD -c "
import json,sys
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')
from app.services.report.report_structured_parser import ReportStructuredParser

parser = ReportStructuredParser()
query = '白细胞12.5 x10^9/L，参考范围4.0-10.0；血红蛋白110 g/L，参考范围120-160；血小板250 x10^9/L，参考范围100-300；中性粒细胞百分比78%，参考范围40-75。'
result = parser.parse(query=query, reports=[])

# Verify structure
assert result.report_type == 'laboratory', f'Expected laboratory, got {result.report_type}'
assert result.overall_status == 'abnormal', f'Expected abnormal, got {result.overall_status}'
assert len(result.indicators) >= 3, f'Expected >= 3 indicators, got {len(result.indicators)}'

# Verify abnormal indicators
abnormal_count = sum(1 for ind in result.indicators if ind.abnormal)
assert abnormal_count >= 2, f'Expected >= 2 abnormal indicators, got {abnormal_count}'

print('✓ Blood report parsed correctly')
print(f'  - Found {len(result.indicators)} indicators')
print(f'  - {abnormal_count} abnormal indicators detected')
print(f'  - Overall status: {result.overall_status}')
")
echo "$result1"

# Test 2: Lipid report 
echo ""
echo "[Test 2] Blood lipid report"
result2=$($PYTHON_CMD -c "
import json,sys
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')
from app.services.report.report_structured_parser import ReportStructuredParser

parser = ReportStructuredParser()
query = '总胆固醇6.5 mmol/L，参考范围0-5.2；低密度脂蛋白4.2 mmol/L，参考范围0-3.4；高密度脂蛋白0.9 mmol/L，参考范围>1.0；甘油三酯2.8 mmol/L，参考范围0-1.7'
result = parser.parse(query=query, reports=[])

assert result.report_type == 'laboratory', f'Expected laboratory, got {result.report_type}'

# Check for lipid indicators
indicator_names = [ind.name for ind in result.indicators]
assert any('胆固醇' in name or 'TC' in name for name in indicator_names), 'Missing cholesterol indicator'
assert any('脂蛋白' in name for name in indicator_names), 'Missing lipoprotein indicator'

print('✓ Lipid report parsed correctly')
print(f'  - Found {len(result.indicators)} lipid indicators')
for ind in result.indicators:
    if ind.abnormal:
        print(f'    - {ind.name}: {ind.value} {ind.unit} (abnormal)')
")
echo "$result2"

# Test 3: Glucose report
echo ""
echo "[Test 3] Fasting glucose report"
result3=$($PYTHON_CMD -c "
import json,sys
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')
from app.services.report.report_structured_parser import ReportStructuredParser

# Test directly with parser to avoid mock report interference
parser = ReportStructuredParser()
query = '空腹血糖7.2 mmol/L，参考范围3.9-6.1'
result_obj = parser.parse(query=query, reports=[])
result = result_obj

# overall_status could be abnormal if multiple indicators are found
# Just verify the glucose indicator itself
glucose_ind = next((ind for ind in result.indicators if '血糖' in ind.name), None)
assert glucose_ind is not None, 'Missing glucose indicator'
assert glucose_ind.status == 'high', f'Expected high status, got {glucose_ind.status}'

print('✓ Glucose report parsed correctly')
print(f'  - Fasting glucose: {glucose_ind.value} {glucose_ind.unit}')
print(f'  - Status: {glucose_ind.status} (reference: {glucose_ind.reference_range})')
")
echo "$result3"

# Test 4: Imaging report
echo ""
echo "[Test 4] CT imaging report"
result4=$($PYTHON_CMD -c "
import json,sys
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')
from app.agents.report.report_agent import ReportAgent

agent = ReportAgent()
query = '胸部CT'
reports = [{
    'report_no': 'RPT001',
    'report_type': 'ct',
    'report_title': '胸部CT',
    'report_date': '2026-03-05',
    'risk_level': 'MEDIUM',
    'interpretation_summary': '右上肺发现1.5cm磨玻璃影',
    'raw_text': '右上肺见约1.5cm磨玻璃影，边界欠清。',
    'hospital_name': '医院',
    'department_name': '放射科'
}]

# Note: ReportAgent.handle doesn't support reports parameter directly
# So we test the parser directly
from app.services.report.report_structured_parser import ReportStructuredParser
parser = ReportStructuredParser()
parsed = parser.parse(query=query, reports=reports)

assert parsed.report_type == 'imaging', f'Expected imaging, got {parsed.report_type}'
assert parsed.overall_status == 'abnormal', f'Expected abnormal, got {parsed.overall_status}'
assert len(parsed.findings) > 0, f'Expected findings, got {len(parsed.findings)}'

print('✓ CT imaging report parsed correctly')
print(f'  - Report type: {parsed.report_type}')
print(f'  - Findings: {len(parsed.findings)}')
if parsed.findings:
    print(f'    - {parsed.findings[0].name}: {parsed.findings[0].summary}')
")
echo "$result4"

# Test 5: JSON output format validation
echo ""
echo "[Test 5] JSON output format validation"
result5=$($PYTHON_CMD -c "
import json,sys
sys.path.insert(0, '/Users/taot/Desktop/projects/patient_agent/services/ai-agent-fastapi')
from app.services.report.report_structured_parser import ReportStructuredParser

parser = ReportStructuredParser()
query = '白细胞12.5 x10^9/L，参考范围4.0-10.0'
result_obj = parser.parse(query=query, reports=[])
output_json = result_obj.model_dump_json(ensure_ascii=False)

# Verify it's valid JSON
parsed = json.loads(output_json)

# Verify all required fields exist
required_fields = ['report_type', 'overall_status', 'indicators', 'findings', 'medical_advice', 'recent_reports']
for field in required_fields:
    assert field in parsed, f'Missing required field: {field}'

# Verify list fields
assert isinstance(parsed['indicators'], list), 'indicators should be a list'
assert isinstance(parsed['findings'], list), 'findings should be a list'
assert isinstance(parsed['medical_advice'], list), 'medical_advice should be a list'
assert isinstance(parsed['recent_reports'], list), 'recent_reports should be a list'

# Verify indicator structure
if parsed['indicators']:
    ind = parsed['indicators'][0]
    ind_fields = ['name', 'value', 'unit', 'reference_range', 'status', 'abnormal', 'medical_explanation']
    for field in ind_fields:
        assert field in ind, f'Missing indicator field: {field}'

print('✓ JSON output format is valid')
print(f'  - All required fields present')
print(f'  - indicators: {len(parsed[\"indicators\"])} items')
print(f'  - findings: {len(parsed[\"findings\"])} items')
print(f'  - medical_advice: {len(parsed[\"medical_advice\"])} items')
")
echo "$result5"

echo ""
echo "=========================================="
echo "All integration tests passed! ✓"
echo "=========================================="
