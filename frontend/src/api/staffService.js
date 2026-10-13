import api from './axios';

export const staffService = {
  /**
   * Get complaints assigned to current maintenance staff.
   * @param {Object} [params] - { search, category, priority, status, page, size, sortBy, direction }
   * @returns {Promise<Object>} PagedResponse<ComplaintResponse>
   */
  async getAssignedComplaints(params = {}) {
    const response = await api.get('/api/staff/complaints', { params });
    return response.data;
  },

  /**
   * Accept an assigned complaint (ASSIGNED -> ACCEPTED).
   * @param {number|string} id
   * @returns {Promise<Object>} ComplaintResponse
   */
  async acceptAssignment(id) {
    const response = await api.patch(`/api/staff/complaints/${id}/accept`);
    return response.data;
  },

  /**
   * Start work on an accepted or reopened complaint (ACCEPTED / REOPENED -> IN_PROGRESS).
   * @param {number|string} id
   * @returns {Promise<Object>} ComplaintResponse
   */
  async startWork(id) {
    const response = await api.patch(`/api/staff/complaints/${id}/start`);
    return response.data;
  },

  /**
   * Submit resolution notes and mark resolved (IN_PROGRESS -> RESOLVED).
   * @param {number|string} id
   * @param {string} resolutionNotes
   * @returns {Promise<Object>} ComplaintResponse
   */
  async resolveComplaint(id, resolutionNotes) {
    const response = await api.patch(`/api/staff/complaints/${id}/resolve`, {
      resolutionNotes,
    });
    return response.data;
  },
};
