import React, { useState } from 'react';
import {
  Button,
  Card,
  Col,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ShieldCheck,
  Search,
  RefreshCw,
  Eye,
  CheckCircle2,
  XCircle,
  FileCode,
  Lock,
} from 'lucide-react';
import { useAudit } from '@/context/AuditContext';
import { formatDate } from '@/utils/formatters';
import type { AuditLogResponseDto, AuditStatus } from '@/types';

export const AuditTrailWorkspace: React.FC = () => {
  const {
    logs,
    totalElements,
    page,
    pageSize,
    loading,
    filter,
    setFilter,
    fetchAuditLogs,
  } = useAudit();

  const [inspectModalOpen, setIsInspectModalOpen] = useState(false);
  const [selectedLog, setSelectedLog] = useState<AuditLogResponseDto | null>(null);

  const handleOpenInspect = (log: AuditLogResponseDto) => {
    setSelectedLog(log);
    setIsInspectModalOpen(true);
  };

  const handleActionSearch = (value: string) => {
    setFilter((prev) => ({ ...prev, action: value || undefined, page: 0 }));
    fetchAuditLogs({ action: value || undefined, page: 0 });
  };

  const handleStatusFilter = (value: string) => {
    const status = value === 'ALL' ? undefined : (value as AuditStatus);
    setFilter((prev) => ({ ...prev, status, page: 0 }));
    fetchAuditLogs({ status, page: 0 });
  };

  const columns: ColumnsType<AuditLogResponseDto> = [
    {
      title: 'Timestamp',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (date: string) => (
        <span style={{ fontSize: 12, color: '#64748b' }}>{formatDate(date)}</span>
      ),
    },
    {
      title: 'Actor (User ID)',
      dataIndex: 'userId',
      key: 'userId',
      render: (userId: string | null) => (
        <span style={{ fontWeight: 600, color: '#0f172a' }}>{userId || 'SYSTEM_DAEMON'}</span>
      ),
    },
    {
      title: 'Action Event',
      dataIndex: 'action',
      key: 'action',
      render: (action: string) => (
        <Tag color="cyan" style={{ borderRadius: 6, fontWeight: 600, fontSize: 11 }}>
          {action}
        </Tag>
      ),
    },
    {
      title: 'Resource',
      key: 'resource',
      render: (_, record) => (
        <div>
          <span style={{ fontSize: 12, fontWeight: 500, color: '#334155' }}>
            {record.resourceType}
          </span>
          {record.resourceId && (
            <div style={{ fontSize: 11, color: '#94a3b8' }}>{record.resourceId}</div>
          )}
        </div>
      ),
    },
    {
      title: 'IP Address',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      render: (ip: string | null) => (
        <code style={{ fontSize: 11, color: '#64748b' }}>{ip || '127.0.0.1'}</code>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (status: AuditStatus) => {
        const isSuccess = status === 'SUCCESS';
        return (
          <Tag
            color={isSuccess ? 'success' : 'error'}
            style={{
              borderRadius: 6,
              fontWeight: 600,
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            {isSuccess ? <CheckCircle2 size={12} /> : <XCircle size={12} />}
            {status}
          </Tag>
        );
      },
    },
    {
      title: 'Evidence',
      key: 'inspect',
      width: 90,
      render: (_, record) => (
        <Tooltip title="Inspect Cryptographic Evidence">
          <Button
            shape="circle"
            icon={<Eye size={14} />}
            type="text"
            onClick={() => handleOpenInspect(record)}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header Info */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0f172a' }}>
            Security & Audit Trail Log
          </h2>
          <p style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>
            Cryptographically sealed and immutable audit trail recording all platform operations, uploads, and privilege modifications.
          </p>
        </div>

        <Button
          icon={<RefreshCw size={15} />}
          onClick={() => fetchAuditLogs()}
          loading={loading}
          style={{ borderRadius: 8, fontWeight: 500 }}
        >
          Refresh Logs
        </Button>
      </div>

      {/* Filter Toolbar */}
      <Card
        style={{
          borderRadius: 12,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
        styles={{ body: { padding: '16px 20px' } }}
      >
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={12} md={8}>
            <Input
              placeholder="Filter by action (e.g. DOCUMENT_UPLOAD)..."
              prefix={<Search size={16} style={{ color: '#94a3b8' }} />}
              onPressEnter={(e) => handleActionSearch((e.target as HTMLInputElement).value)}
              allowClear
              style={{ borderRadius: 8 }}
            />
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Select
              defaultValue="ALL"
              onChange={handleStatusFilter}
              style={{ width: '100%' }}
              options={[
                { value: 'ALL', label: 'All Event Statuses' },
                { value: 'SUCCESS', label: 'SUCCESS (Authorized)' },
                { value: 'FAILED', label: 'FAILED (Rejected / Breach)' },
              ]}
            />
          </Col>
        </Row>
      </Card>

      {/* Audit Logs Table */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15 }}>
            <ShieldCheck size={18} style={{ color: '#10b981' }} />
            <span>Immutable Trail Records ({totalElements})</span>
          </div>
        }
        style={{
          borderRadius: 12,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
        styles={{ body: { padding: 0 } }}
      >
        <Table
          columns={columns}
          dataSource={logs}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize: pageSize,
            total: totalElements,
            onChange: (p) => {
              setFilter((prev) => ({ ...prev, page: p - 1 }));
              fetchAuditLogs({ page: p - 1 });
            },
          }}
        />
      </Card>

      {/* Inspect Evidence Modal */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Lock size={18} style={{ color: '#2563eb' }} />
            <span>Audit Evidence Payload</span>
          </div>
        }
        open={inspectModalOpen}
        onCancel={() => setIsInspectModalOpen(false)}
        footer={[
          <Button key="close" type="primary" onClick={() => setIsInspectModalOpen(false)}>
            Close Inspection
          </Button>,
        ]}
        width={650}
      >
        {selectedLog && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div style={{ background: '#f8fafc', padding: 12, borderRadius: 8, border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 8, fontSize: 13 }}>
                <span style={{ color: '#64748b' }}>Log ID:</span>
                <code style={{ color: '#0f172a' }}>{selectedLog.id}</code>

                <span style={{ color: '#64748b' }}>Action:</span>
                <strong>{selectedLog.action}</strong>

                <span style={{ color: '#64748b' }}>Resource:</span>
                <span>
                  {selectedLog.resourceType} ({selectedLog.resourceId || 'N/A'})
                </span>

                <span style={{ color: '#64748b' }}>Client UA:</span>
                <span style={{ fontSize: 11, color: '#64748b' }}>
                  {selectedLog.userAgent || 'DocOps-SDK/2.4'}
                </span>
              </div>
            </div>

            <div>
              <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 6, color: '#0f172a' }}>
                Cryptographic Details (JSON)
              </div>
              <pre
                style={{
                  background: '#0f172a',
                  color: '#38bdf8',
                  padding: 14,
                  borderRadius: 8,
                  fontSize: 12,
                  overflowX: 'auto',
                }}
              >
                {JSON.stringify(selectedLog.details || {}, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
