import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  Upload, 
  X, 
  AlertCircle, 
  CheckCircle2, 
  Building, 
  Layers, 
  DoorClosed
} from 'lucide-react';
import { complaintService } from '../../api/complaintService';
import { useToast } from '../../context/ToastContext';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { COMPLAINT_CATEGORIES, COMPLAINT_PRIORITIES } from '../../utils/constants';

export const SubmitComplaintPage = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [formData, setFormData] = useState({
    title: '',
    category: 'ELECTRICAL',
    priority: 'MEDIUM',
    building: '',
    floor: '',
    room: '',
    description: '',
  });

  const [files, setFiles] = useState([]);
  const [filePreviews, setFilePreviews] = useState([]);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState(null);

  const validate = () => {
    const errs = {};

    if (!formData.title.trim()) {
      errs.title = 'Title is required';
    } else if (formData.title.trim().length < 5) {
      errs.title = 'Title must be at least 5 characters';
    } else if (formData.title.trim().length > 150) {
      errs.title = 'Title must not exceed 150 characters';
    }

    if (!formData.category) {
      errs.category = 'Category is required';
    }

    if (!formData.building.trim()) {
      errs.building = 'Building name is required';
    } else if (formData.building.trim().length > 100) {
      errs.building = 'Building must not exceed 100 characters';
    }

    if (!formData.floor.trim()) {
      errs.floor = 'Floor is required';
    } else if (formData.floor.trim().length > 50) {
      errs.floor = 'Floor must not exceed 50 characters';
    }

    if (!formData.room.trim()) {
      errs.room = 'Room / Lab / Area is required';
    } else if (formData.room.trim().length > 50) {
      errs.room = 'Room must not exceed 50 characters';
    }

    if (!formData.description.trim()) {
      errs.description = 'Description is required';
    } else if (formData.description.trim().length < 10) {
      errs.description = 'Description must be at least 10 characters';
    } else if (formData.description.trim().length > 2000) {
      errs.description = 'Description must not exceed 2000 characters';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
    if (apiError) setApiError(null);
  };

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files || []);
    if (!selectedFiles.length) return;

    // Validate size (max 5MB each) and image format
    const validFiles = [];
    const newPreviews = [];

    for (const file of selectedFiles) {
      if (file.size > 5 * 1024 * 1024) {
        addToast({
          title: 'File Too Large',
          message: `${file.name} exceeds the 5MB size limit.`,
          type: 'danger',
        });
        continue;
      }

      if (!file.type.startsWith('image/')) {
        addToast({
          title: 'Invalid File Format',
          message: `${file.name} is not an image (JPEG, PNG, WebP supported).`,
          type: 'danger',
        });
        continue;
      }

      validFiles.push(file);
      newPreviews.push(URL.createObjectURL(file));
    }

    setFiles((prev) => [...prev, ...validFiles]);
    setFilePreviews((prev) => [...prev, ...newPreviews]);
  };

  const removeFile = (index) => {
    if (filePreviews[index]) {
      URL.revokeObjectURL(filePreviews[index]);
    }
    setFiles((prev) => prev.filter((_, i) => i !== index));
    setFilePreviews((prev) => prev.filter((_, i) => i !== index));
  };

  React.useEffect(() => {
    return () => {
      filePreviews.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [filePreviews]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setApiError(null);

    try {
      const payload = {
        title: formData.title.trim(),
        category: formData.category,
        priority: formData.priority,
        building: formData.building.trim(),
        floor: formData.floor.trim(),
        room: formData.room.trim(),
        description: formData.description.trim(),
      };

      const result = await complaintService.submitComplaint(payload, files);

      addToast({
        title: 'Complaint Registered',
        message: `Reference #${result.referenceNumber} has been logged.`,
        type: 'success',
      });

      navigate(`/complaints/${result.id}`);
    } catch (err) {
      const msg = err.message || 'Failed to submit complaint. Please check your connection and inputs.';
      setApiError(msg);
      addToast({
        title: 'Submission Failed',
        message: msg,
        type: 'danger',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '880px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Navigation Back */}
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
          <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
            Report Campus Maintenance Issue
          </h2>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
            Provide accurate defect details and location so facility personnel can diagnose and resolve the issue.
          </p>
        </div>
      </div>

      {apiError && (
        <div
          role="alert"
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            padding: '14px 16px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-danger-bg)',
            border: '1px solid var(--color-danger-border)',
            color: 'var(--color-danger-text)',
            fontSize: 'var(--font-size-sm)',
          }}
        >
          <AlertCircle size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
          <span>{apiError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* General Information Card */}
          <Card title="1. Issue Overview">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <Input
                label="Complaint Title"
                name="title"
                value={formData.title}
                onChange={handleChange}
                placeholder="e.g. Broken ceiling projector in Room 302"
                required
                error={errors.title}
                helperText="Summarize the core maintenance issue (5-150 characters)"
              />

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                    Category <span style={{ color: 'var(--color-danger)' }}>*</span>
                  </label>
                  <select
                    name="category"
                    value={formData.category}
                    onChange={handleChange}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-card)',
                      color: 'var(--color-text-primary)',
                      fontSize: 'var(--font-size-sm)',
                    }}
                  >
                    {COMPLAINT_CATEGORIES.map((cat) => (
                      <option key={cat.value} value={cat.value}>
                        {cat.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                    Priority Level <span style={{ color: 'var(--color-danger)' }}>*</span>
                  </label>
                  <select
                    name="priority"
                    value={formData.priority}
                    onChange={handleChange}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-card)',
                      color: 'var(--color-text-primary)',
                      fontSize: 'var(--font-size-sm)',
                    }}
                  >
                    {COMPLAINT_PRIORITIES.map((pri) => (
                      <option key={pri.value} value={pri.value}>
                        {pri.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          </Card>

          {/* Location Information Card */}
          <Card title="2. Campus Location Details">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
              <Input
                label="Building / Facility"
                name="building"
                value={formData.building}
                onChange={handleChange}
                placeholder="e.g. Science & Tech Block"
                required
                error={errors.building}
                icon={<Building size={16} />}
              />

              <Input
                label="Floor / Level"
                name="floor"
                value={formData.floor}
                onChange={handleChange}
                placeholder="e.g. 3rd Floor"
                required
                error={errors.floor}
                icon={<Layers size={16} />}
              />

              <Input
                label="Room / Lab Number"
                name="room"
                value={formData.room}
                onChange={handleChange}
                placeholder="e.g. Room 302 / Lab 4"
                required
                error={errors.room}
                icon={<DoorClosed size={16} />}
              />
            </div>
          </Card>

          {/* Description & Photo Attachments Card */}
          <Card title="3. Defect Description & Photo Evidence">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                  Detailed Description <span style={{ color: 'var(--color-danger)' }}>*</span>
                </label>
                <textarea
                  name="description"
                  rows={5}
                  value={formData.description}
                  onChange={handleChange}
                  placeholder="Describe the exact issue, hazard level, when it started, or specific components affected (10-2000 characters)..."
                  style={{
                    width: '100%',
                    padding: '12px 14px',
                    borderRadius: 'var(--radius-md)',
                    border: `1px solid ${errors.description ? 'var(--color-danger)' : 'var(--color-border)'}`,
                    backgroundColor: 'var(--color-bg-card)',
                    color: 'var(--color-text-primary)',
                    fontSize: 'var(--font-size-sm)',
                    fontFamily: 'inherit',
                    resize: 'vertical',
                    lineHeight: 1.5,
                  }}
                />
                {errors.description && (
                  <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-danger)' }}>
                    {errors.description}
                  </span>
                )}
                <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                  {formData.description.length} / 2000 characters
                </span>
              </div>

              {/* Photo Upload Area */}
              <div>
                <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)', display: 'block', marginBottom: '8px' }}>
                  Photo Attachments (Optional, max 5MB each)
                </label>

                <div
                  style={{
                    border: '2px dashed var(--color-border)',
                    borderRadius: 'var(--radius-lg)',
                    padding: '24px',
                    textAlign: 'center',
                    backgroundColor: 'var(--color-bg-subtle)',
                    cursor: 'pointer',
                  }}
                  onClick={() => document.getElementById('complaint-photo-input')?.click()}
                >
                  <input
                    id="complaint-photo-input"
                    type="file"
                    multiple
                    accept="image/jpeg,image/png,image/webp"
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                  />
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                    <div
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: 'var(--color-primary-light)',
                        color: 'var(--color-primary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <Upload size={20} />
                    </div>
                    <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                      Click to upload photos or drag and drop
                    </div>
                    <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                      PNG, JPG, or WebP (verified via secure magic-byte inspection on backend)
                    </div>
                  </div>
                </div>

                {/* Previews */}
                {filePreviews.length > 0 && (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', marginTop: '16px' }}>
                    {filePreviews.map((previewUrl, idx) => (
                      <div
                        key={idx}
                        style={{
                          position: 'relative',
                          width: '90px',
                          height: '90px',
                          borderRadius: 'var(--radius-md)',
                          overflow: 'hidden',
                          border: '1px solid var(--color-border)',
                        }}
                      >
                        <img
                          src={previewUrl}
                          alt={`Upload preview ${idx + 1}`}
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        />
                        <button
                          type="button"
                          onClick={() => removeFile(idx)}
                          aria-label="Remove image"
                          style={{
                            position: 'absolute',
                            top: '4px',
                            right: '4px',
                            width: '22px',
                            height: '22px',
                            borderRadius: 'var(--radius-full)',
                            backgroundColor: 'rgba(15, 23, 42, 0.75)',
                            color: '#FFFFFF',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                          }}
                        >
                          <X size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </Card>

          {/* Form Actions */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={submitting}
              icon={<CheckCircle2 size={18} />}
            >
              Submit Maintenance Report
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
};
