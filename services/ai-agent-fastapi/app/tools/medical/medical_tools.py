"""医疗工具实现集合。"""
from typing import Any
import os
import logging
import httpx
from app.tools.base_tool import BaseTool, ToolParameter


logger = logging.getLogger(__name__)
BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://127.0.0.1:8080")


class GetMedicalReportTool(BaseTool):
    """获取医疗报告工具。"""
    
    @property
    def name(self) -> str:
        return "get_medical_report"
    
    @property
    def description(self) -> str:
        return "获取用户的医疗报告，包含报告类型、时间、解读摘要和风险等级"
    
    @property
    def parameters(self) -> list[ToolParameter]:
        return [
            ToolParameter(
                name="user_id",
                type="integer",
                description="用户ID",
                required=True,
            ),
            ToolParameter(
                name="report_type",
                type="string",
                description="报告类型: blood(血液检查), ct(CT), mri(核磁共振), pathology(病理), ultrasound(超声)",
                required=False,
            ),
            ToolParameter(
                name="limit",
                type="integer",
                description="返回最多报告数，默认值5",
                required=False,
            ),
        ]
    
    def execute(self, **kwargs) -> dict[str, Any]:
        """执行报告查询 - 调用 Java 后端真实接口。"""
        try:
            user_id = kwargs.get("user_id")
            report_type = kwargs.get("report_type")
            limit = kwargs.get("limit", 5)
            
            if not user_id:
                return {
                    "success": False,
                    "error": "缺少必需参数: user_id",
                }
            
            # 调用 Java 后端的报告列表接口
            params = {
                "userId": user_id,
                "page": 1,
                "pageSize": limit
            }
            if report_type:
                params["reportType"] = report_type
            
            with httpx.Client(timeout=30.0) as client:
                response = client.get(
                    f"{BACKEND_BASE_URL}/api/v1/reports",
                    params=params
                )
                response.raise_for_status()
                result = response.json()
            
            # Java 后端返回格式: code=0 表示成功
            if result.get("code") != 0:
                return {
                    "success": False,
                    "error": result.get("message", "获取医疗报告失败"),
                }
            
            data = result.get("data", {})
            items = data.get("items", [])
            
            # 对每个报告调用详情接口获取完整数据
            reports = []
            with httpx.Client(timeout=30.0) as client:
                for item in items:
                    report_no = item.get("reportNo")
                    if not report_no:
                        continue
                    
                    try:
                        detail_response = client.get(
                            f"{BACKEND_BASE_URL}/api/v1/reports/{report_no}",
                            params={"userId": user_id}
                        )
                        detail_response.raise_for_status()
                        detail_result = detail_response.json()
                        
                        if detail_result.get("code") == 0:
                            detail_data = detail_result.get("data", {})
                            reports.append({
                                "report_no": detail_data.get("reportNo"),
                                "report_type": detail_data.get("reportType"),
                                "report_title": detail_data.get("reportTitle"),
                                "report_date": detail_data.get("reportDate"),
                                "risk_level": detail_data.get("riskLevel"),
                                "raw_text": detail_data.get("rawText", ""),
                                "parsed_json": detail_data.get("parsedJson", ""),
                                "interpretation_summary": detail_data.get("interpretationSummary", ""),
                                "hospital_name": detail_data.get("hospitalName"),
                                "department_name": detail_data.get("departmentName"),
                            })
                    except Exception as e:
                        logger.warning(f"获取报告详情失败 report_no={report_no}: {e}")
                        continue
            
            return {
                "success": True,
                "data": {
                    "user_id": user_id,
                    "reports_count": len(reports),
                    "reports": reports,
                },
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"获取医疗报告失败: {str(e)}",
            }


class GetMedicalRecordTool(BaseTool):
    """获取病历工具。"""
    
    @property
    def name(self) -> str:
        return "get_medical_record"
    
    @property
    def description(self) -> str:
        return "获取用户的病历信息，包含主诉、现病史、既往史等"
    
    @property
    def parameters(self) -> list[ToolParameter]:
        return [
            ToolParameter(
                name="user_id",
                type="integer",
                description="用户ID",
                required=True,
            ),
            ToolParameter(
                name="record_type",
                type="string",
                description="记录类型: OUTPATIENT(门诊), INPATIENT(住院), EMERGENCY(急诊)",
                required=False,
            ),
            ToolParameter(
                name="limit",
                type="integer",
                description="返回最多病历数，默认值5",
                required=False,
            ),
        ]
    
    def execute(self, **kwargs) -> dict[str, Any]:
        """执行病历查询 - 调用 Java 后端真实接口。"""
        try:
            user_id = kwargs.get("user_id")
            record_type = kwargs.get("record_type")
            limit = kwargs.get("limit", 5)
            
            if not user_id:
                return {
                    "success": False,
                    "error": "缺少必需参数: user_id",
                }
            
            # 调用 Java 后端的病历列表接口
            params = {
                "userId": user_id,
                "page": 1,
                "pageSize": limit
            }
            if record_type:
                params["recordType"] = record_type
            
            with httpx.Client(timeout=30.0) as client:
                response = client.get(
                    f"{BACKEND_BASE_URL}/api/v1/medical-records/internal/by-user",
                    params=params
                )
                response.raise_for_status()
                result = response.json()
            
            # Java 后端返回格式: code=0 表示成功
            if result.get("code") != 0:
                return {
                    "success": False,
                    "error": result.get("message", "获取病历记录失败"),
                }
            
            data = result.get("data", {})
            items = data.get("items", [])
            
            # 转换为工具期望的格式
            records = []
            for item in items:
                records.append({
                    "record_no": item.get("recordNo"),
                    "patient_name": item.get("patientName"),
                    "age": item.get("age"),
                    "chief_complaint": item.get("chiefComplaint"),
                    "diagnosis_summary": item.get("diagnosisSummary"),
                    "record_date": item.get("recordDate"),
                    "present_illness": "",
                    "past_history": "",
                    "allergy_history": "",
                    "attending_doctor": item.get("attendingDoctor"),
                })
            
            return {
                "success": True,
                "data": {
                    "user_id": user_id,
                    "records_count": len(records),
                    "records": records,
                },
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"获取病历失败: {str(e)}",
            }


class SearchDrugTool(BaseTool):
    """药物检索工具。"""
    
    @property
    def name(self) -> str:
        return "search_drug"
    
    @property
    def description(self) -> str:
        return "搜索药物信息，包含用途、用法用量、禁忌和副作用"
    
    @property
    def parameters(self) -> list[ToolParameter]:
        return [
            ToolParameter(
                name="drug_name",
                type="string",
                description="药物名称或成分",
                required=True,
            ),
            ToolParameter(
                name="limit",
                type="integer",
                description="返回最多结果数，默认值3",
                required=False,
            ),
        ]
    
    def execute(self, **kwargs) -> dict[str, Any]:
        """执行药物查询。"""
        try:
            drug_name = kwargs.get("drug_name", "").strip()
            limit = kwargs.get("limit", 3)
            
            if not drug_name:
                return {
                    "success": False,
                    "error": "缺少必需参数: drug_name",
                }
            
            # 当前为 Mock 数据；生产环境可接药品数据库/API。
            drug_database = {
                "阿莫西林": {
                    "name": "阿莫西林",
                    "generic_name": "Amoxicillin",
                    "category": "β-内酰胺类抗生素",
                    "usage": "用于治疗由敏感菌引起的感染，如呼吸道感染、泌尿道感染等",
                    "dosage": "成人：250-500mg，每8小时一次；儿童：按体重25-45mg/kg/日分次服用",
                    "contraindications": "对青霉素及β-内酰胺类抗生素过敏者禁用",
                    "side_effects": "胃肠道反应、皮疹、过敏反应",
                },
                "氨溴索": {
                    "name": "氨溴索",
                    "generic_name": "Ambroxol",
                    "category": "粘液溶解剂",
                    "usage": "用于治疗各种呼吸道疾病引起的咳嗽，促进痰液排出",
                    "dosage": "成人：口服15-30mg，每日3次；儿童按年龄酌减",
                    "contraindications": "对本药物成分或其他成分过敏者禁用",
                    "side_effects": "胃肠道反应、皮疹、罕见过敏反应",
                },
                "布洛芬": {
                    "name": "布洛芬",
                    "generic_name": "Ibuprofen",
                    "category": "非甾体抗炎药",
                    "usage": "用于缓解发热、疼痛如头痛、肌肉酸痛、关节痛等",
                    "dosage": "成人：200-400mg，每4-6小时一次，每日不超过1200mg",
                    "contraindications": "胃溃疡、肾脏病变患者慎用；对本药成分过敏者禁用",
                    "side_effects": "胃肠道反应、头晕、皮疹",
                },
            }
            
            # 执行模糊匹配检索。
            results = []
            for drug, info in drug_database.items():
                if drug_name.lower() in drug.lower() or drug_name.lower() in info.get("generic_name", "").lower():
                    results.append(info)
            
            if not results:
                results = [
                    {
                        "name": drug_name,
                        "category": "未知",
                        "usage": f"未找到关于'{drug_name}'的确切信息",
                        "dosage": "请咨询医生或药师",
                        "contraindications": "请咨询医生或药师",
                        "side_effects": "请咨询医生或药师",
                    }
                ]
            
            # 截断返回数量。
            results = results[:limit]
            
            return {
                "success": True,
                "data": {
                    "query": drug_name,
                    "results_count": len(results),
                    "drugs": results,
                },
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"搜索药物失败: {str(e)}",
            }


class SearchDepartmentTool(BaseTool):
    """科室检索工具。"""
    
    @property
    def name(self) -> str:
        return "search_department"
    
    @property
    def description(self) -> str:
        return "搜索医疗科室信息，包含科室名称、主治疾病、常见检查项目"
    
    @property
    def parameters(self) -> list[ToolParameter]:
        return [
            ToolParameter(
                name="department_name",
                type="string",
                description="科室名称或关键词",
                required=True,
            ),
            ToolParameter(
                name="limit",
                type="integer",
                description="返回最多结果数，默认值3",
                required=False,
            ),
        ]
    
    def execute(self, **kwargs) -> dict[str, Any]:
        """执行科室查询。"""
        try:
            dept_name = kwargs.get("department_name", "").strip()
            limit = kwargs.get("limit", 3)
            
            if not dept_name:
                return {
                    "success": False,
                    "error": "缺少必需参数: department_name",
                }
            
            # 当前为 Mock 数据；生产环境可接医院科室服务。
            department_database = {
                "呼吸科": {
                    "name": "呼吸科",
                    "description": "呼吸科专业诊治呼吸系统疾病",
                    "main_diseases": "肺炎、支气管炎、哮喘、肺结核、肺癌、慢性阻塞性肺病等",
                    "common_procedures": "胸部X线、胸部CT、肺功能检查、支气管镜检查、痰培养",
                    "specialist": "肺部及呼吸系统疾病诊疗专家",
                },
                "心内科": {
                    "name": "心内科",
                    "description": "心内科专业诊治心血管系统疾病",
                    "main_diseases": "高血压、冠心病、心律不齐、心衰、瓣膜病等",
                    "common_procedures": "心电图、心脏超声、冠状动脉造影、心肌酶检查、血脂检查",
                    "specialist": "心血管疾病诊疗专家",
                },
                "神经内科": {
                    "name": "神经内科",
                    "description": "神经内科专业诊治神经系统疾病",
                    "main_diseases": "脑卒中、帕金森病、阿尔茨海默病、癫痫、头痛等",
                    "common_procedures": "脑CT、脑MRI、脑脊液检查、脑电图、脑血管超声",
                    "specialist": "神经系统疾病诊疗专家",
                },
                "消化科": {
                    "name": "消化科",
                    "description": "消化科专业诊治消化系统疾病",
                    "main_diseases": "胃炎、胃溃疡、肠炎、肝硬化、胆囊炎、胰腺炎等",
                    "common_procedures": "胃镜、结肠镜、腹部超声、腹部CT、消化道造影",
                    "specialist": "消化系统疾病诊疗专家",
                },
            }
            
            # 执行科室名称匹配。
            results = []
            for dept, info in department_database.items():
                if dept_name.lower() in dept.lower():
                    results.append(info)
            
            if not results:
                results = [
                    {
                        "name": dept_name,
                        "description": f"未找到关于'{dept_name}'科室的详细信息",
                        "main_diseases": "请咨询医院服务台",
                        "common_procedures": "请咨询医院服务台",
                        "specialist": "请咨询医院服务台",
                    }
                ]
            
            # 截断返回数量。
            results = results[:limit]
            
            return {
                "success": True,
                "data": {
                    "query": dept_name,
                    "results_count": len(results),
                    "departments": results,
                },
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"搜索科室失败: {str(e)}",
            }
