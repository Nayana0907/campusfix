import api from './axios';

export const adminService = {
  /**
   * Get all campus complaints with comprehensive filters.
   * @param {Object} [params] - { search, category, priority, status, building, floor, room, reporterId, staffId, startDate, endDate, page, size, sortBy, direction }
   * @returns {Promise<Object>} PagedResponse<ComplaintResponse>
   */
  async getAllComplaints(params = {}) {
    const response = await api.get('/api/admin/complaints', { params });
    return response.data;
  },

  /**
   * Get list of active maintenance staff for assignment dropdowns.
   * @returns {Promise<Array>} List<UserResponse>
   */
  async getStaffUsers() {
    const response = await api.get('/api/admin/complaints/staff');
    return response.data;
  },

  /**
   * Assign or reassign maintenance staff.
   * @param {number|string} complaintId
   * @param {number|string} staffId
   * @returns {Promise<Object>} ComplaintResponse
   */
  async assignStaff(complaintId, staffId) {
    const response = await api.patch(`/api/admin/complaints/${complaintId}/assignment`, {
      staffId: Number(staffId),
    });
    return response.data;
  },

  /**
   * Update complaint priority.
   * @param {number|string} complaintId
   * @param {string} priority - LOW, MEDIUM, HIGH, URGENT
   * @returns {Promise<Object>} ComplaintResponse
   */
  async updatePriority(complaintId, priority) {
    const response = await api.patch(`/api/admin/complaints/${complaintId}/priority`, {
      priority,
    });
    return response.data;
  },

  /**
   * Close a resolved complaint.
   * @param {number|string} complaintId
   * @returns {Promise<Object>} ComplaintResponse
   */
  async closeComplaint(complaintId) {
    const response = await api.patch(`/api/admin/complaints/${complaintId}/close`);
    return response.data;
  },

  /**
   * Reopen a closed or resolved complaint with mandatory reason.
   * @param {number|string} complaintId
   * @param {string} reason
   * @returns {Promise<Object>} ComplaintResponse
   */
  async reopenComplaint(complaintId, reason) {
    const response = await api.patch(`/api/admin/complaints/${complaintId}/reopen`, {
      reason,
    });
    return response.data;
  },

  /**
   * Admin verification override following timeout or dispute.
   * @param {number|string} complaintId
   * @param {string} decision - VERIFIED or REJECTED
   * @param {string} adminOverrideReason
   * @returns {Promise<Object>} VerificationResponse
   */
  async overrideVerification(complaintId, decision, adminOverrideReason) {
    const response = await api.post(`/api/admin/complaints/${complaintId}/verification/override`, {
      verificationStatus: decision,
      adminOverrideReason,
    });
    return response.data;
  },

  // ------------------------------------------------------------------------
  // Analytics Endpoints
  // ------------------------------------------------------------------------

  /**
   * Overview summary cards statistics.
   * @param {string} [from] - YYYY-MM-DD
   * @param {string} [to] - YYYY-MM-DD
   * @returns {Promise<Object>} SummaryAnalyticsResponse
   */
  async getSummary(from, to) {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await api.get('/api/admin/analytics/summary', { params });
    return response.data;
  },

  /**
   * Monthly submission, resolution, and closure trends.
   * @param {string} [from]
   * @param {string} [to]
   * @returns {Promise<Object>} MonthlyTrendsResponse
   */
  async getMonthlyTrends(from, to) {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await api.get('/api/admin/analytics/monthly', { params });
    return response.data;
  },

  /**
   * Category and priority distribution.
   * @param {string} [from]
   * @param {string} [to]
   * @returns {Promise<Object>} CategoryPriorityAnalyticsResponse
   */
  async getCategoriesAndPriorities(from, to) {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await api.get('/api/admin/analytics/categories', { params });
    return response.data;
  },

  /**
   * Location hotspots and recurring issues.
   * @param {Object} [params] - { from, to, building, category, priority, page, size, sort }
   * @returns {Promise<Object>} LocationAnalyticsResponse
   */
  async getLocationAnalytics(params = {}) {
    const response = await api.get('/api/admin/analytics/locations', { params });
    return response.data;
  },

  /**
   * Resolution & verification cycle duration and SLA performance.
   * @param {string} [from]
   * @param {string} [to]
   * @returns {Promise<Object>} ResolutionPerformanceResponse
   */
  async getResolutionPerformance(from, to) {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await api.get('/api/admin/analytics/resolution-performance', { params });
    return response.data;
  },

  /**
   * Maintenance staff performance and workload stats.
   * @param {Object} [params] - { from, to, staffId, page, size, sort }
   * @returns {Promise<Object>} StaffPerformanceResponse
   */
  async getStaffPerformance(params = {}) {
    const response = await api.get('/api/admin/analytics/staff-performance', { params });
    return response.data;
  },
};
