import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  Wrench, 
  CheckCircle, 
  ShieldCheck, 
  RotateCcw,
  History,
  Check,
  X,
  AlertCircle,
  Star
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { complaintService } from '../../api/complaintService';
import { verificationService } from '../../api/verificationService';
import { adminService } from '../../api/adminService';
import { staffService } from '../../api/staffService';
import { feedbackService } from '../../api/feedbackService';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';
import { CardSkeleton } from '../../components/common/Skeleton';
import { formatDate } from '../../utils/constants';

const resolveImageUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  return `${baseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
};

export const ComplaintDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, role } = useAuth();
  const { addToast } = useToast();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [complaint, setComplaint] = useState(null);
  const [history, setHistory] = useState([]);
  const [actionLoading, setActionLoading] = useState(false);

  // Modals state
  const [verifyModal, setVerifyModal] = useState({ open: false, decision: 'VERIFIED', comment: '' });
  const [reopenModal, setReopenModal] = useState({ open: false, reason: '' });
  const [assignModal, setAssignModal] = useState({ open: false, staffId: '', staffList: [] });
  const [priorityModal, setPriorityModal] = useState({ open: false, priority: 'MEDIUM' });
  const [resolveModal, setResolveModal] = useState({ open: false, notes: '', afterImage: null });
  const [previewImage, setPreviewImage] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [feedbackForm, setFeedbackForm] = useState({ rating: 5, comments: '' });
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  const loadComplaint = useCallback(async () => {
    setError(null);
    try {
      const data = await complaintService.getComplaintById(id);
      setComplaint(data);

      // Load chronological audit history
      try {
        const historyData = await complaintService.getComplaintHistory(id);
        setHistory(historyData || []);
      } catch {
        // Non-critical if history fails
      }

      // Load feedback if complaint is resolved or closed
      if (data.status === 'RESOLVED' || data.status === 'CLOSED') {
        try {
          const feedbackData = await feedbackService.getFeedback(id);
          setFeedback(feedbackData);
        } catch {
          setFeedback(null);
        }
      }
    } catch (err) {
      setError(err.message || 'Unable to load complaint details');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadComplaint();
  }, [loadComplaint]);

  const handleFeedbackSubmit = async (e) => {
    e.preventDefault();
    setSubmittingFeedback(true);
    try {
      const savedFb = await feedbackService.submitFeedback(id, {
        rating: Number(feedbackForm.rating),
        comments: feedbackForm.comments.trim(),
      });
      setFeedback(savedFb);
      addToast({
        title: 'Feedback Recorded',
        message: 'Thank you for rating our campus maintenance service!',
        type: 'success',
      });
    } catch (err) {
      addToast({
        title: 'Feedback Error',
        message: err.message || 'Failed to submit feedback',
        type: 'danger',
      });
    } finally {
      setSubmittingFeedback(false);
    }
  };

  // -------------------------------------------------------------------------
  // Student Verification Actions
  // -------------------------------------------------------------------------
  const handleVerifySubmit = async (e) => {
    e.preventDefault();
    if (verifyModal.decision === 'REJECTED' && (!verifyModal.comment || verifyModal.comment.trim().length < 5)) {
      addToast({
        title: 'Comment Required',
        message: 'Please provide a clear reason (min 5 characters) explaining why the repair is inadequate.',
        type: 'danger',
      });
      return;
    }

    setActionLoading(true);
    try {
      await verificationService.verifyResolution(
        id,
        verifyModal.decision,
        verifyModal.comment.trim()
      );

      addToast({
        title: verifyModal.decision === 'VERIFIED' ? 'Repair Confirmed' : 'Resolution Rejected',
        message: verifyModal.decision === 'VERIFIED'
          ? 'Complaint has been verified and marked as CLOSED.'
          : 'Complaint has been reopened for further maintenance.',
        type: verifyModal.decision === 'VERIFIED' ? 'success' : 'warning',
      });

      setVerifyModal({ open: false, decision: 'VERIFIED', comment: '' });
      await loadComplaint();
    } catch (err) {
      addToast({
        title: 'Action Failed',
        message: err.message || 'Failed to record verification decision.',
        type: 'danger',
      });
    } finally {
      setActionLoading(false);
    }
  };

  // -------------------------------------------------------------------------
  // Admin Management Actions
  // -------------------------------------------------------------------------
  const openAssignModal = async () => {
    try {
      // Fetch maintenance staff list from dedicated active staff endpoint
      const staffList = await adminService.getStaffUsers();
      setAssignModal({
        open: true,
        staffId: complaint.assignedStaff?.id || '',
        staffList: Array.isArray(staffList) ? staffList : [],
      });
    } catch {
      setAssignModal({ open: true, staffId: '', staffList: [] });
    }
  };

  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    if (!assignModal.staffId) {
      addToast({ title: 'Select Staff', message: 'Please choose a maintenance staff member.', type: 'danger' });
      return;
    }

    setActionLoading(true);
    try {
      await adminService.assignStaff(id, assignModal.staffId);
      addToast({ title: 'Staff Assigned', message: 'Work order allocated successfully.', type: 'success' });
      setAssignModal({ open: false, staffId: '', staffList: [] });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Assignment Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  const handlePrioritySubmit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      await adminService.updatePriority(id, priorityModal.priority);
      addToast({ title: 'Priority Updated', message: `Complaint priority set to ${priorityModal.priority}.`, type: 'success' });
      setPriorityModal({ open: false, priority: 'MEDIUM' });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Update Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleAdminClose = async () => {
    if (!window.confirm('Are you sure you want to close this complaint?')) return;
    setActionLoading(true);
    try {
      await adminService.closeComplaint(id);
      addToast({ title: 'Complaint Closed', message: 'Ticket marked as CLOSED.', type: 'success' });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Close Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleReopenSubmit = async (e) => {
    e.preventDefault();
    if (!reopenModal.reason || reopenModal.reason.trim().length < 5) {
      addToast({ title: 'Reason Required', message: 'Please provide a valid reopen reason (min 5 characters).', type: 'danger' });
      return;
    }

    setActionLoading(true);
    try {
      await adminService.reopenComplaint(id, reopenModal.reason.trim());
      addToast({ title: 'Complaint Reopened', message: 'Ticket status reverted to REOPENED.', type: 'warning' });
      setReopenModal({ open: false, reason: '' });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Reopen Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  // -------------------------------------------------------------------------
  // Staff Work Order Actions
  // -------------------------------------------------------------------------
  const handleStaffAccept = async () => {
    setActionLoading(true);
    try {
      await staffService.acceptAssignment(id);
      addToast({ title: 'Assignment Accepted', message: 'Status updated to ACCEPTED.', type: 'success' });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Accept Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleStaffStart = async () => {
    setActionLoading(true);
    try {
      await staffService.startWork(id);
      addToast({ title: 'Work Started', message: 'Complaint is now IN_PROGRESS.', type: 'success' });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Start Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleStaffResolveSubmit = async (e) => {
    e.preventDefault();
    if (!resolveModal.notes || resolveModal.notes.trim().length < 5) {
      addToast({ title: 'Notes Required', message: 'Please specify repair notes (min 5 characters).', type: 'danger' });
      return;
    }

    setActionLoading(true);
    try {
      await verificationService.submitResolution(id, resolveModal.notes.trim(), resolveModal.afterImage);
      addToast({ title: 'Resolution Submitted', message: 'Work marked resolved and submitted for verification.', type: 'success' });
      setResolveModal({ open: false, notes: '', afterImage: null });
      await loadComplaint();
    } catch (err) {
      addToast({ title: 'Resolution Failed', message: err.message, type: 'danger' });
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <CardSkeleton />
        <CardSkeleton />
      </div>
    );
  }

  if (error || !complaint) {
    return (
      <div style={{ maxWidth: '600px', margin: '40px auto', textAlign: 'center' }}>
        <div style={{ padding: '24px', backgroundColor: 'var(--color-bg-card)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--color-border)' }}>
          <AlertCircle size={48} color="var(--color-danger)" style={{ marginBottom: '16px' }} />
          <h2 style={{ fontSize: 'var(--font-size-xl)', fontWeight: 700, marginBottom: '8px' }}>
            Complaint Not Located
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginBottom: '24px' }}>
            {error || 'The requested ticket could not be found or you lack permission to view it.'}
          </p>
          <Button variant="primary" onClick={() => navigate('/complaints')}>
            Return to Registry
          </Button>
        </div>
      </div>
    );
  }

  const isReporter = user?.id === complaint.reporter?.id;
  const isAssignedStaff = user?.id === complaint.assignedStaff?.id;
  const isAdmin = role === 'ADMIN';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Top Breadcrumb & Actions Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Button
            variant="outline"
            size="sm"
            icon={<ArrowLeft size={16} />}
            onClick={() => navigate(-1)}
          >
            Back
          </Button>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 700, color: 'var(--color-primary)' }}>
                {complaint.referenceNumber}
              </span>
              <StatusBadge value={complaint.status} size="sm" />
              <StatusBadge value={complaint.priority} size="sm" />
            </div>
            <h1 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)', marginTop: '4px' }}>
              {complaint.title}
            </h1>
          </div>
        </div>

        {/* Role Control Actions Header */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {isAdmin && (
            <>
              <Button
                variant="outline"
                size="sm"
                icon={<Wrench size={14} />}
                onClick={openAssignModal}
              >
                {complaint.assignedStaff ? 'Reassign Staff' : 'Assign Staff'}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPriorityModal({ open: true, priority: complaint.priority })}
              >
                Priority
              </Button>
              {complaint.status === 'RESOLVED' && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleAdminClose}
                  isLoading={actionLoading}
                >
                  Close Ticket
                </Button>
              )}
              {(complaint.status === 'RESOLVED' || complaint.status === 'CLOSED') && (
                <Button
                  variant="danger"
                  size="sm"
                  icon={<RotateCcw size={14} />}
                  onClick={() => setReopenModal({ open: true, reason: '' })}
                >
                  Reopen
                </Button>
              )}
            </>
          )}

          {/* Maintenance Staff Lifecycle Buttons */}
          {isAssignedStaff && (
            <>
              {complaint.status === 'ASSIGNED' && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleStaffAccept}
                  isLoading={actionLoading}
                >
                  Accept Assignment
                </Button>
              )}
              {(complaint.status === 'ACCEPTED' || complaint.status === 'REOPENED') && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleStaffStart}
                  isLoading={actionLoading}
                >
                  Start Work (In Progress)
                </Button>
              )}
              {complaint.status === 'IN_PROGRESS' && (
                <Button
                  variant="primary"
                  size="sm"
                  icon={<CheckCircle size={16} />}
                  onClick={() => setResolveModal({ open: true, notes: '', afterImage: null })}
                >
                  Complete & Resolve
                </Button>
              )}
            </>
          )}
        </div>
      </div>

      {/* Resolution Verification Callout Banner (For Student Reporter) */}
      {(complaint.status === 'RESOLVED' || complaint.verificationStatus === 'PENDING') && (isReporter || isAdmin) && (
        <Card
          hoverable={false}
          style={{
            backgroundColor: 'var(--color-warning-bg)',
            borderColor: 'var(--color-warning-border)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
            <div style={{ display: 'flex', gap: '14px' }}>
              <div
                style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: 'var(--color-warning)',
                  color: '#FFFFFF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <ShieldCheck size={22} />
              </div>
              <div>
                <h3 style={{ fontSize: 'var(--font-size-base)', fontWeight: 700, color: 'var(--color-warning-text)' }}>
                  Verification Required: Was this issue fixed?
                </h3>
                <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '4px', maxWidth: '650px' }}>
                  Maintenance staff has finished repairing this issue. Please check the premises and confirm resolution.
                </p>
                {complaint.resolutionNotes && (
                  <div style={{ marginTop: '8px', padding: '10px 14px', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-bg-card)', border: '1px solid var(--color-border)', fontSize: 'var(--font-size-sm)' }}>
                    <strong>Staff Notes:</strong> {complaint.resolutionNotes}
                  </div>
                )}
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <Button
                variant="outline"
                size="sm"
                icon={<X size={16} />}
                onClick={() => setVerifyModal({ open: true, decision: 'REJECTED', comment: '' })}
              >
                Reject & Reopen
              </Button>
              <Button
                variant="primary"
                size="sm"
                icon={<Check size={16} />}
                onClick={() => setVerifyModal({ open: true, decision: 'VERIFIED', comment: '' })}
              >
                Confirm Fixed (Close)
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Student Feedback & Rating Section */}
      {(complaint.status === 'RESOLVED' || complaint.status === 'CLOSED') && (
        <Card title="Student Resolution Feedback">
          {feedback ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-secondary)' }}>
                  Satisfaction Rating:
                </span>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  {[1, 2, 3, 4, 5].map((s) => (
                    <Star
                      key={s}
                      size={18}
                      fill={s <= feedback.rating ? '#F59E0B' : 'transparent'}
                      color={s <= feedback.rating ? '#F59E0B' : 'var(--color-text-muted)'}
                    />
                  ))}
                </div>
                <span style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', color: '#F59E0B' }}>
                  {feedback.rating} / 5
                </span>
              </div>
              {(feedback.comment || feedback.comments) && (
                <div style={{ padding: '12px 14px', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-bg-subtle)', border: '1px solid var(--color-border)', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>
                  "{feedback.comment || feedback.comments}"
                </div>
              )}
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                Submitted by {feedback.studentName || 'Student'} on {formatDate(feedback.createdAt)}
              </div>
            </div>
          ) : isReporter ? (
            <form onSubmit={handleFeedbackSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <p style={{ margin: 0, fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                Your maintenance request has been addressed. Please rate your experience to help improve campus facility operations.
              </p>
              <div>
                <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 600, textTransform: 'uppercase', color: 'var(--color-text-secondary)', marginBottom: '8px' }}>
                  Rating (1 to 5 Stars)
                </label>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  {[1, 2, 3, 4, 5].map((s) => (
                    <button
                      type="button"
                      key={s}
                      onClick={() => setFeedbackForm((prev) => ({ ...prev, rating: s }))}
                      style={{
                        background: 'none',
                        border: 'none',
                        cursor: 'pointer',
                        padding: '4px',
                        display: 'flex',
                        alignItems: 'center',
                      }}
                    >
                      <Star
                        size={24}
                        fill={s <= feedbackForm.rating ? '#F59E0B' : 'transparent'}
                        color={s <= feedbackForm.rating ? '#F59E0B' : 'var(--color-text-muted)'}
                      />
                    </button>
                  ))}
                  <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 700, marginLeft: '8px', color: '#F59E0B' }}>
                    {feedbackForm.rating} Stars
                  </span>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 600, textTransform: 'uppercase', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                  Comments (Optional)
                </label>
                <textarea
                  rows={2}
                  value={feedbackForm.comments}
                  onChange={(e) => setFeedbackForm((prev) => ({ ...prev, comments: e.target.value }))}
                  placeholder="Share details about repair quality, speed, or technician professionalism..."
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    fontFamily: 'inherit',
                    fontSize: 'var(--font-size-sm)',
                  }}
                />
              </div>

              <div>
                <Button type="submit" variant="primary" size="sm" isLoading={submittingFeedback}>
                  Submit Rating
                </Button>
              </div>
            </form>
          ) : (
            <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
              No student feedback submitted yet for this resolved ticket.
            </div>
          )}
        </Card>
      )}

      {/* Main Grid: Details (Left) + Sidebar Context (Right) */}
      <div className="content-grid">
        {/* Left Column: Complaint Full Details */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Description & Location */}
          <Card title="Issue Description & Location">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
              <div>
                <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Description
                </div>
                <p style={{ fontSize: 'var(--font-size-base)', color: 'var(--color-text-primary)', lineHeight: 1.6, whiteSpace: 'pre-line' }}>
                  {complaint.description}
                </p>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', paddingTop: '16px', borderTop: '1px solid var(--color-border)' }}>
                <div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>Building</div>
                  <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                    {complaint.building}
                  </div>
                </div>

                <div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>Floor</div>
                  <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                    {complaint.floor}
                  </div>
                </div>

                <div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>Room / Area</div>
                  <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                    {complaint.room}
                  </div>
                </div>
              </div>
            </div>
          </Card>

          {/* Photo Attachments & After Images */}
          <Card title="Photo Evidence">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Initial submitted photos */}
              <div>
                <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase', marginBottom: '8px' }}>
                  Reported Defect Photos ({complaint.images?.length || 0})
                </div>

                {(!complaint.images || complaint.images.length === 0) ? (
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
                    No photos were attached during report creation.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
                    {complaint.images.map((img) => {
                      const imgUrl = resolveImageUrl(img.url);
                      return (
                        <div
                          key={img.id}
                          style={{
                            width: '120px',
                            height: '120px',
                            borderRadius: 'var(--radius-md)',
                            overflow: 'hidden',
                            border: '1px solid var(--color-border)',
                            cursor: 'pointer',
                          }}
                          onClick={() => setPreviewImage(imgUrl)}
                        >
                          <img
                            src={imgUrl}
                            alt={img.originalFilename || 'Complaint attachment'}
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            onError={(e) => {
                              e.currentTarget.style.display = 'none';
                            }}
                          />
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* After-Repair Photo */}
              {complaint.afterImageUrl && (
                <div style={{ paddingTop: '16px', borderTop: '1px solid var(--color-border)' }}>
                  <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-success)', textTransform: 'uppercase', marginBottom: '8px' }}>
                    After-Repair Verification Photo
                  </div>
                  <div
                    style={{
                      width: '140px',
                      height: '140px',
                      borderRadius: 'var(--radius-md)',
                      overflow: 'hidden',
                      border: '2px solid var(--color-success)',
                      cursor: 'pointer',
                    }}
                    onClick={() => setPreviewImage(resolveImageUrl(complaint.afterImageUrl))}
                  >
                    <img
                      src={resolveImageUrl(complaint.afterImageUrl)}
                      alt="After repair evidence"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                      }}
                    />
                  </div>
                </div>
              )}
            </div>
          </Card>

          {/* Audit History Timeline */}
          <Card title="Activity & Audit History">
            {history.length === 0 ? (
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                No history log available for this ticket yet.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', position: 'relative' }}>
                {history.map((entry, idx) => (
                  <div
                    key={entry.id || idx}
                    style={{
                      display: 'flex',
                      gap: '14px',
                      position: 'relative',
                    }}
                  >
                    <div
                      style={{
                        width: '32px',
                        height: '32px',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: 'var(--color-primary-light)',
                        color: 'var(--color-primary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                        marginTop: '2px',
                      }}
                    >
                      <History size={16} />
                    </div>

                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                        <span style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>
                          {entry.action}
                        </span>
                        {entry.newStatus && (
                          <StatusBadge value={entry.newStatus} size="sm" />
                        )}
                        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                          {formatDate(entry.timestamp)}
                        </span>
                      </div>

                      <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                        {entry.description || 'Status updated'}
                      </div>

                      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                        by <strong>{entry.actorName || 'System'}</strong> ({entry.actorRole || 'STAFF'})
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>

        {/* Right Column: Key Details & Personnel */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Status & Category Panel */}
          <Card title="Ticket Metadata">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', fontSize: 'var(--font-size-sm)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Status:</span>
                <StatusBadge value={complaint.status} />
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Priority:</span>
                <StatusBadge value={complaint.priority} />
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Category:</span>
                <span style={{ fontWeight: 600 }}>{complaint.category}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Reported On:</span>
                <span style={{ fontWeight: 500 }}>{formatDate(complaint.createdAt)}</span>
              </div>

              {complaint.resolvedAt && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ color: 'var(--color-text-secondary)' }}>Resolved On:</span>
                  <span style={{ fontWeight: 500 }}>{formatDate(complaint.resolvedAt)}</span>
                </div>
              )}
            </div>
          </Card>

          {/* Assigned Staff Info */}
          <Card title="Assigned Technician">
            {complaint.assignedStaff ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div
                  style={{
                    width: '42px',
                    height: '42px',
                    borderRadius: 'var(--radius-full)',
                    backgroundColor: 'var(--color-primary)',
                    color: '#FFFFFF',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {complaint.assignedStaff.name?.charAt(0) || 'S'}
                </div>
                <div>
                  <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                    {complaint.assignedStaff.name}
                  </div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                    {complaint.assignedStaff.email}
                  </div>
                  {complaint.assignedStaff.phone && (
                    <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                      Tel: {complaint.assignedStaff.phone}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
                No maintenance staff assigned yet.
              </div>
            )}
          </Card>

          {/* Reporter Info */}
          <Card title="Reported By">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: 'var(--color-bg-subtle)',
                  color: 'var(--color-text-secondary)',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '1px solid var(--color-border)',
                }}
              >
                {complaint.reporter?.name?.charAt(0) || 'U'}
              </div>
              <div>
                <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                  {complaint.reporter?.name || 'Anonymous Student'}
                </div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                  {complaint.reporter?.email}
                </div>
                {complaint.reporter?.phone && (
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                    Tel: {complaint.reporter?.phone}
                  </div>
                )}
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* ------------------------------------------------------------------- */}
      {/* Interactive Modals */}
      {/* ------------------------------------------------------------------- */}

      {/* Verification Modal (Confirm or Reject) */}
      <Modal
        isOpen={verifyModal.open}
        onClose={() => setVerifyModal({ open: false, decision: 'VERIFIED', comment: '' })}
        title={verifyModal.decision === 'VERIFIED' ? 'Confirm Resolution' : 'Reject & Reopen Resolution'}
      >
        <form onSubmit={handleVerifySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
            {verifyModal.decision === 'VERIFIED'
              ? 'By confirming, you verify that maintenance personnel successfully fixed this issue on campus. The ticket will be transitioned to CLOSED.'
              : 'Please explain why the repair is incomplete or defective. The ticket will revert to REOPENED for staff re-work.'}
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              Feedback / Comment {verifyModal.decision === 'REJECTED' && <span style={{ color: 'var(--color-danger)' }}>*</span>}
            </label>
            <textarea
              rows={3}
              value={verifyModal.comment}
              onChange={(e) => setVerifyModal((prev) => ({ ...prev, comment: e.target.value }))}
              placeholder={verifyModal.decision === 'VERIFIED' ? 'Optional feedback...' : 'Explain the remaining defect...'}
              required={verifyModal.decision === 'REJECTED'}
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                fontFamily: 'inherit',
                fontSize: 'var(--font-size-sm)',
              }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button
              type="button"
              variant="outline"
              onClick={() => setVerifyModal({ open: false, decision: 'VERIFIED', comment: '' })}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant={verifyModal.decision === 'VERIFIED' ? 'primary' : 'danger'}
              isLoading={actionLoading}
            >
              {verifyModal.decision === 'VERIFIED' ? 'Confirm & Close' : 'Submit Rejection'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Admin Reopen Modal */}
      <Modal
        isOpen={reopenModal.open}
        onClose={() => setReopenModal({ open: false, reason: '' })}
        title="Reopen Complaint Ticket"
      >
        <form onSubmit={handleReopenSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
            Please state the administrative justification for reopening this complaint (5-1000 characters).
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <textarea
              rows={3}
              value={reopenModal.reason}
              onChange={(e) => setReopenModal((prev) => ({ ...prev, reason: e.target.value }))}
              placeholder="State reason for reopening ticket..."
              required
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                fontFamily: 'inherit',
                fontSize: 'var(--font-size-sm)',
              }}
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button type="button" variant="outline" onClick={() => setReopenModal({ open: false, reason: '' })}>
              Cancel
            </Button>
            <Button type="submit" variant="danger" isLoading={actionLoading}>
              Reopen Ticket
            </Button>
          </div>
        </form>
      </Modal>

      {/* Admin Staff Assignment Modal */}
      <Modal
        isOpen={assignModal.open}
        onClose={() => setAssignModal({ open: false, staffId: '', staffList: [] })}
        title="Assign Maintenance Staff"
      >
        <form onSubmit={handleAssignSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              Select Technician
            </label>
            <select
              value={assignModal.staffId}
              onChange={(e) => setAssignModal((prev) => ({ ...prev, staffId: e.target.value }))}
              required
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-card)',
                fontSize: 'var(--font-size-sm)',
              }}
            >
              <option value="">-- Choose Staff Member --</option>
              {assignModal.staffList.map((st) => {
                const sId = st.id || st.staffId;
                const sName = st.name || st.staffName;
                const sEmail = st.email || st.staffEmail;
                return (
                  <option key={sId} value={sId}>
                    {sName} ({sEmail})
                  </option>
                );
              })}
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button type="button" variant="outline" onClick={() => setAssignModal({ open: false, staffId: '', staffList: [] })}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={actionLoading}>
              Save Assignment
            </Button>
          </div>
        </form>
      </Modal>

      {/* Admin Priority Modal */}
      <Modal
        isOpen={priorityModal.open}
        onClose={() => setPriorityModal({ open: false, priority: 'MEDIUM' })}
        title="Update Priority"
      >
        <form onSubmit={handlePrioritySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>Priority Level</label>
            <select
              value={priorityModal.priority}
              onChange={(e) => setPriorityModal((prev) => ({ ...prev, priority: e.target.value }))}
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                fontSize: 'var(--font-size-sm)',
              }}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button type="button" variant="outline" onClick={() => setPriorityModal({ open: false, priority: 'MEDIUM' })}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={actionLoading}>
              Save Priority
            </Button>
          </div>
        </form>
      </Modal>

      {/* Staff Resolution Modal */}
      <Modal
        isOpen={resolveModal.open}
        onClose={() => setResolveModal({ open: false, notes: '', afterImage: null })}
        title="Complete Work Order & Submit Resolution"
      >
        <form onSubmit={handleStaffResolveSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              Resolution Notes <span style={{ color: 'var(--color-danger)' }}>*</span>
            </label>
            <textarea
              rows={4}
              value={resolveModal.notes}
              onChange={(e) => setResolveModal((prev) => ({ ...prev, notes: e.target.value }))}
              placeholder="Describe what repairs were made, parts replaced, and safety checks performed..."
              required
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                fontSize: 'var(--font-size-sm)',
                fontFamily: 'inherit',
              }}
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              After-Repair Photo (Optional)
            </label>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) setResolveModal((prev) => ({ ...prev, afterImage: file }));
              }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button type="button" variant="outline" onClick={() => setResolveModal({ open: false, notes: '', afterImage: null })}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={actionLoading}>
              Submit Resolution
            </Button>
          </div>
        </form>
      </Modal>

      {/* Fullscreen Photo Lightbox Modal */}
      <Modal
        isOpen={Boolean(previewImage)}
        onClose={() => setPreviewImage(null)}
        title="Attachment Preview"
        maxWidth="800px"
      >
        <div style={{ textAlign: 'center', padding: '10px' }}>
          {previewImage && (
            <img
              src={previewImage}
              alt="Preview"
              style={{ maxWidth: '100%', maxHeight: '70vh', borderRadius: 'var(--radius-md)' }}
            />
          )}
        </div>
      </Modal>
    </div>
  );
};
