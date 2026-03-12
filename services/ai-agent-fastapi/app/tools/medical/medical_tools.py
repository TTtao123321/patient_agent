"""Medical tools implementation."""
from typing import Any
from app.tools.base_tool import BaseTool, ToolParameter


class GetMedicalReportTool(BaseTool):
    """Tool to retrieve medical reports."""
    
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
        """
        Execute get_medical_report tool.
        
        Args:
            user_id: User ID
            report_type: Optional report type filter
            limit: Optional limit for number of reports
        
        Returns:
            Dictionary with reports data or error
        """
        try:
            user_id = kwargs.get("user_id")
            report_type = kwargs.get("report_type")
            limit = kwargs.get("limit", 5)
            
            if not user_id:
                return {
                    "success": False,
                    "error": "缺少必需参数: user_id",
                }
            
            # Mock data - In production, this would call Java backend API
            mock_reports = [
                {
                    "report_no": "RPT20260310001",
                    "report_type": "blood",
                    "report_title": "血液检查报告",
                    "report_date": "2026-03-10",
                    "risk_level": "MEDIUM",
                    "interpretation_summary": "白细胞轻度升高，血红蛋白偏低，提示可能存在感染和轻度贫血倾向",
                    "raw_text": "白细胞 12.5 x10^9/L，参考范围 4.0-10.0；血红蛋白 110 g/L，参考范围 120-160；血小板 250 x10^9/L，参考范围 100-300；中性粒细胞百分比 78%，参考范围 40-75。",
                    "hospital_name": "协和医院",
                    "department_name": "检验科",
                },
                {
                    "report_no": "RPT20260305002",
                    "report_type": "ct",
                    "report_title": "胸部CT报告",
                    "report_date": "2026-03-05",
                    "risk_level": "MEDIUM",
                    "interpretation_summary": "右上肺发现1.5cm磨玻璃影，建议3个月复查",
                    "raw_text": "胸部CT提示：右上肺见约1.5cm磨玻璃影，边界欠清。双肺纹理稍增多，未见明显胸腔积液。",
                    "hospital_name": "协和医院",
                    "department_name": "放射科",
                },
                {
                    "report_no": "RPT20260301003",
                    "report_type": "blood",
                    "report_title": "血脂检查报告",
                    "report_date": "2026-03-01",
                    "risk_level": "HIGH",
                    "interpretation_summary": "总胆固醇升高，低密度脂蛋白升高，提示血脂异常风险",
                    "raw_text": "总胆固醇 6.5 mmol/L，参考范围 0-5.2；低密度脂蛋白 4.2 mmol/L，参考范围 0-3.4；高密度脂蛋白 0.9 mmol/L，参考范围 >1.0；甘油三酯 2.8 mmol/L，参考范围 0-1.7。",
                    "hospital_name": "协和医院",
                    "department_name": "检验科",
                },
                {
                    "report_no": "RPT20260228004",
                    "report_type": "blood",
                    "report_title": "空腹血糖检查报告",
                    "report_date": "2026-02-28",
                    "risk_level": "MEDIUM",
                    "interpretation_summary": "空腹血糖升高，提示糖代谢异常倾向",
                    "raw_text": "空腹血糖 7.2 mmol/L，参考范围 3.9-6.1；血糖负荷后1小时 11.8 mmol/L，参考范围 <7.8；血糖负荷后2小时 8.9 mmol/L，参考范围 <7.8。",
                    "hospital_name": "协和医院",
                    "department_name": "检验科",
                },
            ]
            
            # Filter by report type if specified
            if report_type:
                mock_reports = [r for r in mock_reports if r["report_type"] == report_type]
            
            # Apply limit
            mock_reports = mock_reports[:limit]
            
            return {
                "success": True,
                "data": {
                    "user_id": user_id,
                    "reports_count": len(mock_reports),
                    "reports": mock_reports,
                },
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"获取医疗报告失败: {str(e)}",
            }


class GetMedicalRecordTool(BaseTool):
    """Tool to retrieve medical records."""
    
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
                name="limit",
                type="integer",
                description="返回最多病历数，默认值3",
                required=False,
            ),
        ]
    
    def execute(self, **kwargs) -> dict[str, Any]:
        """
        Execute get_medical_record tool.
        
        Args:
            user_id: User ID
            limit: Optional limit for number of records
        
        Returns:
            Dictionary with medical records or error
        """
        try:
            user_id = kwargs.get("user_id")
            limit = kwargs.get("limit", 3)
            
            if not user_id:
                return {
                    "success": False,
                    "error": "缺少必需参数: user_id",
                }
            
            # Mock data - In production, this would call Java backend API
            mock_records = [
                {
                    "record_no": "MR20260310001",
                    "patient_name": "李明",
                    "age": 45,
                    "record_date": "2026-03-10",
                    "chief_complaint": "咳嗽2周",
                    "present_illness": "患者2周前因受凉出现干咳，伴喉咙痒，无发热，无浓痰",
                    "past_history": "高血压病史10年，目前控制良好；2年前得过肺炎",
                    "allergy_history": "青霉素过敏",
                    "attending_doctor": "王医生",
                },
                {
                    "record_no": "MR20260220001",
                    "patient_name": "李明",
                    "age": 45,
                    "record_date": "2026-02-20",
                    "chief_complaint": "头晕",
                    "present_illness": "患者血压升高至160/100mmHg，伴头晕",
                    "past_history": "高血压病史10年",
                    "allergy_history": "青霉素过敏",
                    "attending_doctor": "张医生",
                },
            ]
            
            # Apply limit
            mock_records = mock_records[:limit]
            
            return {
                "success": True,
                "data": {
                    "user_id": user_id,
                    "records_count": len(mock_records),
                    "records": mock_records,
                },
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"获取病历失败: {str(e)}",
            }


class SearchDrugTool(BaseTool):
    """Tool to search for drug information."""
    
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
        """
        Execute search_drug tool.
        
        Args:
            drug_name: Drug name to search
            limit: Optional limit for number of results
        
        Returns:
            Dictionary with drug information or error
        """
        try:
            drug_name = kwargs.get("drug_name", "").strip()
            limit = kwargs.get("limit", 3)
            
            if not drug_name:
                return {
                    "success": False,
                    "error": "缺少必需参数: drug_name",
                }
            
            # Mock data - In production, this would call a drug database API
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
            
            # Search for drug
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
            
            # Apply limit
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
    """Tool to search for medical departments."""
    
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
        """
        Execute search_department tool.
        
        Args:
            department_name: Department name to search
            limit: Optional limit for number of results
        
        Returns:
            Dictionary with department information or error
        """
        try:
            dept_name = kwargs.get("department_name", "").strip()
            limit = kwargs.get("limit", 3)
            
            if not dept_name:
                return {
                    "success": False,
                    "error": "缺少必需参数: department_name",
                }
            
            # Mock data - In production, this would call a department database
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
            
            # Search for department
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
            
            # Apply limit
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
