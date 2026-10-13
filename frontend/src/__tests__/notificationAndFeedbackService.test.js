import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api/axios';
import { notificationService } from '../api/notificationService';
import { feedbackService } from '../api/feedbackService';
import { adminService } from '../api/adminService';

vi.mock('../api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('Notification and Feedback API Services', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('notificationService', () => {
    it('fetches notifications list', async () => {
      const mockNotifications = [
        { id: 1, eventType: 'COMPLAINT_CREATED', subject: 'New Ticket', isRead: false },
        { id: 2, eventType: 'VERIFICATION_PENDING', subject: 'Action Required', isRead: true },
      ];
      api.get.mockResolvedValueOnce({ data: mockNotifications });

      const result = await notificationService.getNotifications();
      expect(api.get).toHaveBeenCalledWith('/api/notifications');
      expect(result).toEqual(mockNotifications);
    });

    it('marks a specific notification as read', async () => {
      const updatedNotif = { id: 1, isRead: true };
      api.patch.mockResolvedValueOnce({ data: updatedNotif });

      const result = await notificationService.markAsRead(1);
      expect(api.patch).toHaveBeenCalledWith('/api/notifications/1/read');
      expect(result.isRead).toBe(true);
    });

    it('marks all notifications as read', async () => {
      api.patch.mockResolvedValueOnce({ data: { message: 'All marked read' } });

      const result = await notificationService.markAllAsRead();
      expect(api.patch).toHaveBeenCalledWith('/api/notifications/read-all');
      expect(result.message).toBe('All marked read');
    });
  });

  describe('feedbackService', () => {
    it('submits student resolution feedback', async () => {
      const mockResponse = { id: 10, complaintId: 42, rating: 5, comments: 'Fast fix!' };
      api.post.mockResolvedValueOnce({ data: mockResponse });

      const result = await feedbackService.submitFeedback(42, { rating: 5, comments: 'Fast fix!' });
      expect(api.post).toHaveBeenCalledWith('/api/complaints/42/feedback', { rating: 5, comments: 'Fast fix!' });
      expect(result.rating).toBe(5);
    });

    it('retrieves existing feedback for complaint', async () => {
      const mockResponse = { id: 10, complaintId: 42, rating: 4, comments: 'Satisfactory' };
      api.get.mockResolvedValueOnce({ data: mockResponse });

      const result = await feedbackService.getFeedback(42);
      expect(api.get).toHaveBeenCalledWith('/api/complaints/42/feedback');
      expect(result.comments).toBe('Satisfactory');
    });
  });

  describe('adminService active staff', () => {
    it('fetches active maintenance staff users', async () => {
      const mockStaff = [
        { id: 3, name: 'Bob Tech', email: 'bob@campusfix.edu', role: 'MAINTENANCE_STAFF' },
      ];
      api.get.mockResolvedValueOnce({ data: mockStaff });

      const result = await adminService.getStaffUsers();
      expect(api.get).toHaveBeenCalledWith('/api/admin/complaints/staff');
      expect(result).toHaveLength(1);
      expect(result[0].role).toBe('MAINTENANCE_STAFF');
    });
  });
});
