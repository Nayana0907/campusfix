import { describe, it, expect, vi, beforeEach } from 'vitest';
import { staffService } from '../api/staffService';
import { verificationService } from '../api/verificationService';
import request from '../api/axios';

vi.mock('../api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Staff and Verification API Services', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('staffService', () => {
    it('retrieves assigned work orders', async () => {
      const mockResult = { content: [{ id: 10, title: 'Fix projector' }] };
      request.get.mockResolvedValueOnce({ data: mockResult });

      const params = { page: 0, size: 10, status: 'ASSIGNED' };
      const res = await staffService.getAssignedComplaints(params);

      expect(request.get).toHaveBeenCalledWith('/api/staff/complaints', { params });
      expect(res.content).toHaveLength(1);
    });

    it('accepts assignment via PATCH /api/staff/complaints/{id}/accept', async () => {
      request.patch.mockResolvedValueOnce({ data: { id: 10, status: 'ACCEPTED' } });

      const res = await staffService.acceptAssignment(10);

      expect(request.patch).toHaveBeenCalledWith('/api/staff/complaints/10/accept');
      expect(res.status).toBe('ACCEPTED');
    });

    it('starts work via PATCH /api/staff/complaints/{id}/start', async () => {
      request.patch.mockResolvedValueOnce({ data: { id: 10, status: 'IN_PROGRESS' } });

      const res = await staffService.startWork(10);

      expect(request.patch).toHaveBeenCalledWith('/api/staff/complaints/10/start');
      expect(res.status).toBe('IN_PROGRESS');
    });

    it('marks complaint resolved via PATCH /api/staff/complaints/{id}/resolve', async () => {
      request.patch.mockResolvedValueOnce({ data: { id: 10, status: 'RESOLVED' } });

      const res = await staffService.resolveComplaint(10, 'Wiring replaced safely');

      expect(request.patch).toHaveBeenCalledWith('/api/staff/complaints/10/resolve', {
        resolutionNotes: 'Wiring replaced safely',
      });
      expect(res.status).toBe('RESOLVED');
    });
  });

  describe('verificationService', () => {
    it('verifies resolution with decision VERIFIED via POST /api/complaints/{id}/verification', async () => {
      request.post.mockResolvedValueOnce({
        data: {
          id: 77,
          status: 'CLOSED',
          verificationStatus: 'VERIFIED',
        },
      });

      const res = await verificationService.verifyResolution(77, 'VERIFIED', 'Looks perfectly repaired!');

      expect(request.post).toHaveBeenCalledWith('/api/complaints/77/verification', {
        decision: 'VERIFIED',
        comment: 'Looks perfectly repaired!',
      });
      expect(res.status).toBe('CLOSED');
      expect(res.verificationStatus).toBe('VERIFIED');
    });

    it('rejects resolution with decision REJECTED and student comment', async () => {
      request.post.mockResolvedValueOnce({
        data: {
          id: 77,
          status: 'REOPENED',
          verificationStatus: 'REJECTED',
        },
      });

      const res = await verificationService.verifyResolution(77, 'REJECTED', 'Water is still leaking continuously.');

      expect(request.post).toHaveBeenCalledWith('/api/complaints/77/verification', {
        decision: 'REJECTED',
        comment: 'Water is still leaking continuously.',
      });
      expect(res.status).toBe('REOPENED');
    });

    it('submits resolution notes and afterImage as multipart via POST /api/complaints/{id}/resolution', async () => {
      request.post.mockResolvedValueOnce({ data: { id: 77, status: 'RESOLVED' } });

      const mockImage = new File(['evidence'], 'fixed.jpg', { type: 'image/jpeg' });
      await verificationService.submitResolution(77, 'Replaced the damaged pipe with PVC tube', mockImage);

      expect(request.post).toHaveBeenCalledTimes(1);
      const [url, formData, config] = request.post.mock.calls[0];

      expect(url).toBe('/api/complaints/77/resolution');
      expect(formData).toBeInstanceOf(FormData);
      expect(config.headers['Content-Type']).toBe('multipart/form-data');
    });
  });
});
