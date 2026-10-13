import { describe, it, expect, vi, beforeEach } from 'vitest';
import { complaintService } from '../api/complaintService';
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

describe('complaintService API client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits complaint with multipart Form and correct RequestPart structure', async () => {
    request.post.mockResolvedValueOnce({
      data: {
        id: 101,
        complaintRef: 'CMP-2026-101',
        title: 'Water Leak',
        category: 'PLUMBING',
        priority: 'HIGH',
      },
    });

    const mockFile = new File(['mock content'], 'leak.jpg', { type: 'image/jpeg' });
    const complaintData = {
      title: 'Water Leak',
      description: 'Pipe leaking under the sink in Lab 3',
      category: 'PLUMBING',
      priority: 'HIGH',
      building: 'Science Block',
      floor: 2,
      roomNumber: 'Lab 3',
    };

    const result = await complaintService.submitComplaint(complaintData, [mockFile]);

    expect(request.post).toHaveBeenCalledTimes(1);
    const [url, formData, config] = request.post.mock.calls[0];

    expect(url).toBe('/api/complaints');
    expect(formData).toBeInstanceOf(FormData);
    expect(config.headers['Content-Type']).toBe('multipart/form-data');
    expect(result.id).toBe(101);
    expect(result.complaintRef).toBe('CMP-2026-101');
  });

  it('retrieves student complaints with proper pagination and sorting params', async () => {
    const mockResponse = {
      content: [{ id: 1, title: 'Broken Light' }],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    };
    request.get.mockResolvedValueOnce({ data: mockResponse });

    const params = { page: 0, size: 10, category: 'ELECTRICAL', status: 'SUBMITTED' };
    const result = await complaintService.getMyComplaints(params);

    expect(request.get).toHaveBeenCalledWith('/api/complaints/mine', { params });
    expect(result.content).toHaveLength(1);
  });

  it('fetches complaint detail by ID', async () => {
    request.get.mockResolvedValueOnce({ data: { id: 42, title: 'AC repair' } });

    const result = await complaintService.getComplaintById(42);

    expect(request.get).toHaveBeenCalledWith('/api/complaints/42');
    expect(result.id).toBe(42);
  });

  it('fetches complaint audit timeline history', async () => {
    request.get.mockResolvedValueOnce({
      data: [
        { id: 1, action: 'CREATED', timestamp: '2026-10-12T10:00:00' },
        { id: 2, action: 'ASSIGNED', timestamp: '2026-10-12T11:00:00' },
      ],
    });

    const result = await complaintService.getComplaintHistory(42);

    expect(request.get).toHaveBeenCalledWith('/api/complaints/42/history');
    expect(result).toHaveLength(2);
  });
});
