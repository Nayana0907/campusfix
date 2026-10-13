import { describe, it, expect, vi, beforeEach } from 'vitest';
import { adminService } from '../api/adminService';
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

describe('adminService API client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches all complaints with filters', async () => {
    const mockData = { content: [], totalElements: 0 };
    request.get.mockResolvedValueOnce({ data: mockData });

    const params = { page: 0, size: 10, status: 'ASSIGNED', priority: 'HIGH' };
    const result = await adminService.getAllComplaints(params);

    expect(request.get).toHaveBeenCalledWith('/api/admin/complaints', { params });
    expect(result).toEqual(mockData);
  });

  it('assigns staff with staffId payload via PATCH /api/admin/complaints/{id}/assignment', async () => {
    request.patch.mockResolvedValueOnce({ data: { id: 55, status: 'ASSIGNED' } });

    const result = await adminService.assignStaff(55, 12);

    expect(request.patch).toHaveBeenCalledWith('/api/admin/complaints/55/assignment', { staffId: 12 });
    expect(result.status).toBe('ASSIGNED');
  });

  it('fetches summary analytics metrics', async () => {
    const mockSummary = {
      totalComplaints: 120,
      submittedComplaints: 15,
      assignedComplaints: 25,
      inProgressComplaints: 30,
      resolvedComplaints: 40,
      closedComplaints: 10,
      overdueComplaints: 5,
    };
    request.get.mockResolvedValueOnce({ data: mockSummary });

    const result = await adminService.getSummary();

    expect(request.get).toHaveBeenCalledWith('/api/admin/analytics/summary', { params: {} });
    expect(result.totalComplaints).toBe(120);
    expect(result.overdueComplaints).toBe(5);
  });

  it('fetches monthly trends with date filters', async () => {
    const mockTrends = { dataPoints: [] };
    request.get.mockResolvedValueOnce({ data: mockTrends });

    const result = await adminService.getMonthlyTrends('2026-01-01', '2026-10-12');

    expect(request.get).toHaveBeenCalledWith('/api/admin/analytics/monthly', {
      params: { from: '2026-01-01', to: '2026-10-12' },
    });
    expect(result).toEqual(mockTrends);
  });

  it('fetches location analytics and staff performance', async () => {
    request.get.mockResolvedValueOnce({ data: { buildingCounts: [{ building: 'Engineering', count: 12 }] } });
    request.get.mockResolvedValueOnce({ data: { items: [{ staffId: 5, staffName: 'John', resolvedCount: 8 }] } });

    const locResult = await adminService.getLocationAnalytics();
    const staffResult = await adminService.getStaffPerformance();

    expect(request.get).toHaveBeenCalledWith('/api/admin/analytics/locations', { params: {} });
    expect(request.get).toHaveBeenCalledWith('/api/admin/analytics/staff-performance', { params: {} });
    expect(locResult.buildingCounts[0].building).toBe('Engineering');
    expect(staffResult.items[0].staffName).toBe('John');
  });
});
