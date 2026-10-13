import api from './axios';

export const feedbackService = {
  /**
   * Submit post-resolution feedback for a complaint.
   * @param {number|string} complaintId
   * @param {Object} data - { rating: 1-5, comment: string }
   * @returns {Promise<Object>} FeedbackResponse
   */
  async submitFeedback(complaintId, data) {
    const response = await api.post(`/api/complaints/${complaintId}/feedback`, data);
    return response.data;
  },

  /**
   * Fetch existing feedback for a complaint.
   * @param {number|string} complaintId
   * @returns {Promise<Object>} FeedbackResponse
   */
  async getFeedback(complaintId) {
    const response = await api.get(`/api/complaints/${complaintId}/feedback`);
    return response.data;
  },
};
