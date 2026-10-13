import api from './axios';

export const verificationService = {
  /**
   * Submit resolution with notes and optional after-repair photo.
   * @param {number|string} complaintId
   * @param {string} resolutionNotes
   * @param {File} [afterImage] - Optional after-repair photo file
   * @returns {Promise<Object>} ResolutionResponse
   */
  async submitResolution(complaintId, resolutionNotes, afterImage = null) {
    if (afterImage) {
      const formData = new FormData();
      formData.append('resolutionNotes', resolutionNotes);
      formData.append('afterImage', afterImage);

      const response = await api.post(`/api/complaints/${complaintId}/resolution`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    }

    const response = await api.post(`/api/complaints/${complaintId}/resolution`, {
      resolutionNotes,
    });
    return response.data;
  },

  /**
   * Reporter verifies resolution (VERIFIED -> CLOSED, or REJECTED -> REOPENED).
   * @param {number|string} complaintId
   * @param {string} decision - 'VERIFIED' or 'REJECTED'
   * @param {string} [comment] - Optional/mandatory feedback comment
   * @returns {Promise<Object>} VerificationResponse
   */
  async verifyResolution(complaintId, decision, comment = '') {
    const response = await api.post(`/api/complaints/${complaintId}/verification`, {
      decision,
      comment,
    });
    return response.data;
  },

  /**
   * Get active verification cycle state for a complaint.
   * @param {number|string} complaintId
   * @returns {Promise<Object>} VerificationResponse
   */
  async getCurrentVerification(complaintId) {
    const response = await api.get(`/api/complaints/${complaintId}/verification`);
    return response.data;
  },

  /**
   * Get full verification cycle history.
   * @param {number|string} complaintId
   * @returns {Promise<Array>} List<VerificationHistoryResponse>
   */
  async getVerificationHistory(complaintId) {
    const response = await api.get(`/api/complaints/${complaintId}/verification-history`);
    return response.data;
  },

  /**
   * Reopen a complaint directly.
   * @param {number|string} complaintId
   * @param {string} reason
   * @returns {Promise<Object>} ComplaintResponse
   */
  async reopenComplaint(complaintId, reason) {
    const response = await api.post(`/api/complaints/${complaintId}/reopen`, {
      reason,
    });
    return response.data;
  },
};
