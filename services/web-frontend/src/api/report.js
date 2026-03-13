import request from '../utils/request';

/**
 * 上传医疗报告文件（multipart/form-data）。
 *
 * @param {object} params
 * @param {number}        params.userId
 * @param {string}        params.reportType   blood / ct / mri / ultrasound / ecg / pathology / other
 * @param {string}        params.reportTitle
 * @param {string}        [params.hospitalName]
 * @param {string}        [params.departmentName]
 * @param {string}        [params.reportDate]   yyyy-MM-dd
 * @param {File|null}     [params.file]
 */
export function uploadReport({ userId, reportType, reportTitle, hospitalName, departmentName, reportDate, file }) {
  const form = new FormData();
  form.append('userId', userId);
  form.append('reportType', reportType);
  form.append('reportTitle', reportTitle);
  if (hospitalName)   form.append('hospitalName',   hospitalName);
  if (departmentName) form.append('departmentName', departmentName);
  if (reportDate)     form.append('reportDate',     reportDate);
  if (file)           form.append('file', file);

  return request({
    url: '/v1/reports/upload',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  });
}

/**
 * 触发 AI 解读已上传的报告。
 *
 * @param {string} reportNo
 * @param {number} userId
 */
export function interpretReport(reportNo, userId) {
  return request({
    url: `/v1/reports/${reportNo}/interpret`,
    method: 'post',
    params: { userId },
    timeout: 120000,
  });
}

/**
 * 分页查询用户报告列表。
 *
 * @param {number} userId
 * @param {number} [page=1]
 * @param {number} [pageSize=20]
 */
export function listReports(userId, page = 1, pageSize = 20) {
  return request({
    url: '/v1/reports',
    method: 'get',
    params: { userId, page, pageSize },
  });
}

/**
 * 查询报告详情。
 *
 * @param {string} reportNo
 * @param {number} userId
 */
export function getReportDetail(reportNo, userId) {
  return request({
    url: `/v1/reports/${reportNo}`,
    method: 'get',
    params: { userId },
  });
}
