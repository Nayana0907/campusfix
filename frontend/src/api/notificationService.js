import api from './axios';

export const notificationService = {
  /**
   * Fetch all notifications for the current user.
   * @returns {Promise<Array>} List<NotificationResponse>
   */
  async getNotifications() {
    const response = await api.get('/api/notifications');
    return response.data;
  },

  /**
   * Mark a specific notification as read.
   * @param {number|string} id
   * @returns {Promise<Object>} NotificationResponse
   */
  async markAsRead(id) {
    const response = await api.patch(`/api/notifications/${id}/read`);
    return response.data;
  },

  /**
   * Mark all notifications as read for current user.
   * @returns {Promise<Object>}
   */
  async markAllAsRead() {
    const response = await api.patch('/api/notifications/read-all');
    return response.data;
  },
};
