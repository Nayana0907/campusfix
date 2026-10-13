import api from './axios';

export const complaintService = {
  /**
   * Submit a new complaint.
   * If images are provided, uses multipart/form-data. Otherwise sends JSON.
   * @param {Object} data - { title, description, category, priority, building, floor, room }
   * @param {File[]} [files] - Optional array of image files
   * @returns {Promise<Object>} ComplaintResponse
   */
  async submitComplaint(data, files = []) {
    if (files && files.length > 0) {
      const formData = new FormData();
      // Spring Boot @RequestPart("complaint") expects JSON blob
      const complaintBlob = new Blob([JSON.stringify(data)], { type: 'application/json' });
      formData.append('complaint', complaintBlob);

      files.forEach((file) => {
        formData.append('images', file);
      });

      const response = await api.post('/api/complaints', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    }

    const response = await api.post('/api/complaints', data);
    return response.data;
  },

  /**
   * Get complaints reported by current student.
   * @param {Object} [params] - { search, category, priority, status, page, size, sortBy, direction }
   * @returns {Promise<Object>} PagedResponse<ComplaintResponse>
   */
  async getMyComplaints(params = {}) {
    const response = await api.get('/api/complaints/mine', { params });
    return response.data;
  },

  /**
   * Get complaint details by ID.
   * @param {number|string} id
   * @returns {Promise<Object>} ComplaintResponse
   */
  async getComplaintById(id) {
    const response = await api.get(`/api/complaints/${id}`);
    return response.data;
  },

  /**
   * Get complaint by reference number (e.g. CMP-20261012-ABC123).
   * @param {string} ref
   * @returns {Promise<Object>} ComplaintResponse
   */
  async getComplaintByRef(ref) {
    const response = await api.get(`/api/complaints/ref/${encodeURIComponent(ref)}`);
    return response.data;
  },

  /**
   * Get chronological audit history for a complaint.
   * @param {number|string} id
   * @returns {Promise<Array>} List<ComplaintHistoryResponse>
   */
  async getComplaintHistory(id) {
    const response = await api.get(`/api/complaints/${id}/history`);
    return response.data;
  },

  /**
   * Attach additional images to an existing complaint.
   * @param {number|string} id
   * @param {File[]} files
   * @returns {Promise<Array>} List<ComplaintImageResponse>
   */
  async addImages(id, files) {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('images', file);
    });

    const response = await api.post(`/api/complaints/${id}/images`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};
