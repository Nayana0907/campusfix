import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { StatusBadge } from '../components/common/StatusBadge';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { EmptyState } from '../components/common/EmptyState';

describe('Reusable Design System Components', () => {
  it('StatusBadge correctly maps student, admin, and maintenance roles', () => {
    const studentHtml = renderToStaticMarkup(<StatusBadge value="STUDENT" />);
    expect(studentHtml).toContain('Student');

    const adminHtml = renderToStaticMarkup(<StatusBadge value="ADMIN" />);
    expect(adminHtml).toContain('Administrator');

    const staffHtml = renderToStaticMarkup(<StatusBadge value="MAINTENANCE_STAFF" />);
    expect(staffHtml).toContain('Staff');
  });

  it('StatusBadge correctly maps complaint statuses', () => {
    const inProgressHtml = renderToStaticMarkup(<StatusBadge value="IN_PROGRESS" />);
    expect(inProgressHtml).toContain('In Progress');

    const resolvedHtml = renderToStaticMarkup(<StatusBadge value="RESOLVED" />);
    expect(resolvedHtml).toContain('Resolved');
  });

  it('Card renders title, action, and children content', () => {
    const cardHtml = renderToStaticMarkup(
      <Card title="System Metrics" action={<button>Action</button>}>
        <p>Main content inside card</p>
      </Card>
    );

    expect(cardHtml).toContain('System Metrics');
    expect(cardHtml).toContain('Action');
    expect(cardHtml).toContain('Main content inside card');
  });

  it('Button renders different variants and disabled states', () => {
    const primaryHtml = renderToStaticMarkup(<Button variant="primary">Submit</Button>);
    expect(primaryHtml).toContain('Submit');
    expect(primaryHtml).toContain('btn-primary');

    const dangerHtml = renderToStaticMarkup(<Button variant="danger">Delete</Button>);
    expect(dangerHtml).toContain('btn-danger');

    const loadingHtml = renderToStaticMarkup(<Button isLoading>Saving</Button>);
    expect(loadingHtml).toContain('disabled');
    expect(loadingHtml).toContain('aria-busy="true"');
  });

  it('Card with onClick renders accessible button role and tabIndex', () => {
    const cardHtml = renderToStaticMarkup(
      <Card title="Clickable Metric" onClick={() => {}}>
        <p>Metric Value</p>
      </Card>
    );

    expect(cardHtml).toContain('role="button"');
    expect(cardHtml).toContain('tabindex="0"');
  });

  it('EmptyState renders icon, title, description and action', () => {
    const emptyHtml = renderToStaticMarkup(
      <EmptyState
        title="No complaints yet"
        description="Submit your first ticket"
        actionLabel="Create Ticket"
      />
    );

    expect(emptyHtml).toContain('No complaints yet');
    expect(emptyHtml).toContain('Submit your first ticket');
    expect(emptyHtml).toContain('Create Ticket');
  });

  it('Input renders accessible label, required asterisk, and error message', () => {
    const inputHtml = renderToStaticMarkup(
      <Input
        label="Campus Email"
        name="email"
        required
        error="Invalid email format"
        value=""
        onChange={() => {}}
      />
    );

    expect(inputHtml).toContain('Campus Email');
    expect(inputHtml).toContain('*');
    expect(inputHtml).toContain('Invalid email format');
    expect(inputHtml).toContain('aria-invalid="true"');
  });
});
