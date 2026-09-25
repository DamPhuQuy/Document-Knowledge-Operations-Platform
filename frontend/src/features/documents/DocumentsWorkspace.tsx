import React, { useState, useMemo } from 'react';
import {
  Button,
  Card,
  Col,
  Dropdown,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Segmented,
  Select,
  Table,
  Tag,
  Tooltip,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import {
  Files,
  HardDrive,
  ShieldAlert,
  Building,
  UploadCloud,
  Search,
  Grid,
  List,
  MoreVertical,
  History,
  Lock,
  Trash2,
  FileText,
  FileSpreadsheet,
  FileCode,
  FileArchive,
  File as GenericFileIcon,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { useDocument } from '@/context/DocumentContext';
import { useDepartment } from '@/context/DepartmentContext';
import { useAuth } from '@/context/AuthContext';
import { formatBytes, formatDate, getAccessBadgeConfig } from '@/utils/formatters';
import type {
  DocumentResponseDto,
  AccessLevel,
  UploadDocumentParams,
  UploadVersionParams,
  UpdateDocumentPermissionsRequest,
} from '@/types';

const { Dragger } = Upload;

export const DocumentsWorkspace: React.FC = () => {
  const {
    documents,
    isLoading,
    documentCount,
    totalStorageBytes,
    highSecurityCount,
    uniqueDepartmentsCount,
    uploadDocument,
    uploadVersion,
    updatePermissions,
    deleteDocument,
  } = useDocument();

  const { departments } = useDepartment();
  const { isAdmin, isManager } = useAuth();

  // View & Filter states
  const [viewMode, setViewMode] = useState<'grid' | 'table'>('grid');
  const [searchTerm, setSearchTerm] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState<string>('ALL');
  const [accessLevelFilter, setAccessLevelFilter] = useState<string>('ALL');

  // Modal states
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isVersionModalOpen, setIsVersionModalOpen] = useState(false);
  const [isAclModalOpen, setIsAclModalOpen] = useState(false);
  const [selectedDoc, setSelectedDoc] = useState<DocumentResponseDto | null>(null);

  // Forms
  const [uploadForm] = Form.useForm();
  const [versionForm] = Form.useForm();
  const [aclForm] = Form.useForm();

  // Upload file list
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [versionFileList, setVersionFileList] = useState<UploadFile[]>([]);
  const [submitting, setSubmitting] = useState(false);

  // Department name lookup map
  const deptMap = useMemo(() => {
    const map = new Map<string, string>();
    departments.forEach((d) => map.set(d.id, d.name));
    return map;
  }, [departments]);

  // Filtered documents
  const filteredDocuments = useMemo(() => {
    return documents.filter((doc: DocumentResponseDto) => {
      const matchesSearch =
        (doc.title && doc.title.toLowerCase().includes(searchTerm.toLowerCase())) ||
        doc.originalFileName.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesDept =
        departmentFilter === 'ALL' ||
        (departmentFilter === 'UNASSIGNED' ? !doc.departmentId : doc.departmentId === departmentFilter);

      const matchesAccess =
        accessLevelFilter === 'ALL' || doc.accessLevel === accessLevelFilter;

      return matchesSearch && matchesDept && matchesAccess;
    });
  }, [documents, searchTerm, departmentFilter, accessLevelFilter]);

  // File icon helper
  const getFileIcon = (contentType: string, fileName: string) => {
    const lower = (fileName || '').toLowerCase();
    if (contentType.includes('pdf') || lower.endsWith('.pdf')) {
      return { icon: <FileText size={22} style={{ color: '#ef4444' }} />, bg: '#fee2e2' };
    }
    if (
      contentType.includes('sheet') ||
      contentType.includes('excel') ||
      lower.endsWith('.xlsx') ||
      lower.endsWith('.csv')
    ) {
      return { icon: <FileSpreadsheet size={22} style={{ color: '#10b981' }} />, bg: '#d1fae5' };
    }
    if (
      contentType.includes('json') ||
      contentType.includes('javascript') ||
      lower.endsWith('.ts') ||
      lower.endsWith('.json')
    ) {
      return { icon: <FileCode size={22} style={{ color: '#f59e0b' }} />, bg: '#fef3c7' };
    }
    if (contentType.includes('zip') || contentType.includes('tar') || lower.endsWith('.zip')) {
      return { icon: <FileArchive size={22} style={{ color: '#8b5cf6' }} />, bg: '#ede9fe' };
    }
    return { icon: <GenericFileIcon size={22} style={{ color: '#3b82f6' }} />, bg: '#dbeafe' };
  };

  // Actions
  const handleOpenUpload = () => {
    uploadForm.resetFields();
    setFileList([]);
    setIsUploadModalOpen(true);
  };

  const handleOpenVersionModal = (doc: DocumentResponseDto) => {
    setSelectedDoc(doc);
    versionForm.resetFields();
    setVersionFileList([]);
    setIsVersionModalOpen(true);
  };

  const handleOpenAclModal = (doc: DocumentResponseDto) => {
    setSelectedDoc(doc);
    aclForm.setFieldsValue({
      accessLevel: doc.accessLevel,
    });
    setIsAclModalOpen(true);
  };

  const handleDeleteDoc = (doc: DocumentResponseDto) => {
    Modal.confirm({
      title: 'Confirm Document Purge',
      content: `Are you sure you want to delete "${doc.title || doc.originalFileName}"? This action cannot be undone.`,
      okText: 'Delete Permanently',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          await deleteDocument(doc.id);
          message.success('Document deleted successfully');
        } catch {
          message.error('Failed to delete document');
        }
      },
    });
  };

  const handleUploadSubmit = async () => {
    try {
      const values = await uploadForm.validateFields();
      const rawFile = (fileList[0]?.originFileObj || fileList[0]) as unknown as File;
      if (!fileList.length || !rawFile) {
        message.warning('Please select a file to upload');
        return;
      }
      setSubmitting(true);
      const payload: UploadDocumentParams = {
        file: rawFile,
        title: values.title,
        description: values.description,
        accessLevel: values.accessLevel,
        departmentId: values.departmentId,
      };
      await uploadDocument(payload);
      message.success('Document uploaded and encrypted successfully');
      setIsUploadModalOpen(false);
    } catch (err: any) {
      if (err?.errorFields) {
        return;
      }
      message.error(err?.message || 'Failed to upload document');
    } finally {
      setSubmitting(false);
    }
  };

  const handleVersionSubmit = async () => {
    if (!selectedDoc) return;
    try {
      const values = await versionForm.validateFields();
      const rawVersionFile = (versionFileList[0]?.originFileObj || versionFileList[0]) as unknown as File;
      if (!versionFileList.length || !rawVersionFile) {
        message.warning('Please select a new version file');
        return;
      }
      setSubmitting(true);
      const payload: UploadVersionParams = {
        file: rawVersionFile,
        changeSummary: values.changeSummary,
      };
      await uploadVersion(selectedDoc.id, payload);
      message.success('New version committed to platform');
      setIsVersionModalOpen(false);
    } catch (err: any) {
      if (err?.errorFields) {
        return;
      }
      message.error(err?.message || 'Failed to commit version');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAclSubmit = async () => {
    if (!selectedDoc) return;
    try {
      const values = await aclForm.validateFields();
      setSubmitting(true);
      const payload: UpdateDocumentPermissionsRequest = {
        accessLevel: values.accessLevel,
      };
      await updatePermissions(selectedDoc.id, payload);
      message.success('Access control policies updated');
      setIsAclModalOpen(false);
    } catch {
      message.error('Failed to update access control');
    } finally {
      setSubmitting(false);
    }
  };

  // Table columns definition
  const columns: ColumnsType<DocumentResponseDto> = [
    {
      title: 'Document',
      key: 'name',
      render: (_, record) => {
        const iconInfo = getFileIcon(record.contentType, record.originalFileName);
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 8,
                background: iconInfo.bg,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {iconInfo.icon}
            </div>
            <div>
              <div style={{ fontWeight: 600, color: '#0f172a' }}>
                {record.title || record.originalFileName}
              </div>
              <div style={{ fontSize: 12, color: '#94a3b8' }}>{record.originalFileName}</div>
            </div>
          </div>
        );
      },
    },
    {
      title: 'Department',
      key: 'department',
      render: (_, record) => {
        const deptName = record.departmentId ? deptMap.get(record.departmentId) : null;
        return deptName ? (
          <Tag color="blue" style={{ borderRadius: 6, fontWeight: 500 }}>
            {deptName}
          </Tag>
        ) : (
          <span style={{ color: '#94a3b8', fontSize: 12 }}>Unassigned</span>
        );
      },
    },
    {
      title: 'Access Level',
      key: 'accessLevel',
      render: (_, record) => {
        const badge = getAccessBadgeConfig(record.accessLevel);
        return (
          <Tag color={badge.color} style={{ borderRadius: 6, fontWeight: 600 }}>
            {record.accessLevel}
          </Tag>
        );
      },
    },
    {
      title: 'Size',
      key: 'size',
      render: (_, record) => (
        <span style={{ fontSize: 13, color: '#475569', fontWeight: 500 }}>
          {formatBytes(record.fileSizeBytes)}
        </span>
      ),
    },
    {
      title: 'Last Modified',
      key: 'updatedAt',
      render: (_, record) => (
        <span style={{ fontSize: 12, color: '#64748b' }}>
          {formatDate(record.updatedAt || record.createdAt)}
        </span>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 90,
      render: (_, record) => (
        <Dropdown
          menu={{
            items: [
              {
                key: 'version',
                icon: <History size={14} />,
                label: 'Commit New Version',
                onClick: () => handleOpenVersionModal(record),
              },
              {
                key: 'acl',
                icon: <Lock size={14} />,
                label: 'Access Policies (ACL)',
                onClick: () => handleOpenAclModal(record),
              },
              {
                type: 'divider',
              },
              {
                key: 'delete',
                icon: <Trash2 size={14} />,
                label: 'Delete Document',
                danger: true,
                onClick: () => handleDeleteDoc(record),
              },
            ],
          }}
          trigger={['click']}
        >
          <Button shape="circle" icon={<MoreVertical size={16} />} type="text" />
        </Dropdown>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* KPI Section */}
      <Row gutter={[20, 20]}>
        <Col xs={24} sm={12} lg={6}>
          <div className="midone-kpi-card">
            <div className="midone-kpi-header">
              <span className="midone-kpi-label">TOTAL DOCUMENTS</span>
              <div className="midone-kpi-icon-wrap" style={{ background: '#eff6ff', color: '#2563eb' }}>
                <Files size={22} />
              </div>
            </div>
            <div className="midone-kpi-value">{documentCount}</div>
            <div style={{ fontSize: 12, color: '#10b981', display: 'flex', alignItems: 'center', gap: 4, marginTop: 6 }}>
              <CheckCircle2 size={13} />
              <span>Full cryptographic tracking</span>
            </div>
          </div>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <div className="midone-kpi-card">
            <div className="midone-kpi-header">
              <span className="midone-kpi-label">ENCRYPTED STORAGE</span>
              <div className="midone-kpi-icon-wrap" style={{ background: '#ecfdf5', color: '#10b981' }}>
                <HardDrive size={22} />
              </div>
            </div>
            <div className="midone-kpi-value">{formatBytes(totalStorageBytes)}</div>
            <div style={{ fontSize: 12, color: '#64748b', display: 'flex', alignItems: 'center', gap: 4, marginTop: 6 }}>
              <span>AES-GCM-256 at Rest</span>
            </div>
          </div>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <div className="midone-kpi-card">
            <div className="midone-kpi-header">
              <span className="midone-kpi-label">HIGH SECURITY FILES</span>
              <div className="midone-kpi-icon-wrap" style={{ background: '#fef2f2', color: '#ef4444' }}>
                <ShieldAlert size={22} />
              </div>
            </div>
            <div className="midone-kpi-value">{highSecurityCount}</div>
            <div style={{ fontSize: 12, color: '#ef4444', display: 'flex', alignItems: 'center', gap: 4, marginTop: 6 }}>
              <span>Confidential / Restricted</span>
            </div>
          </div>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <div className="midone-kpi-card">
            <div className="midone-kpi-header">
              <span className="midone-kpi-label">DEPARTMENTS LINKED</span>
              <div className="midone-kpi-icon-wrap" style={{ background: '#f5f3ff', color: '#8b5cf6' }}>
                <Building size={22} />
              </div>
            </div>
            <div className="midone-kpi-value">{uniqueDepartmentsCount}</div>
            <div style={{ fontSize: 12, color: '#8b5cf6', display: 'flex', alignItems: 'center', gap: 4, marginTop: 6 }}>
              <span>Org Units Enforced</span>
            </div>
          </div>
        </Col>
      </Row>

      {/* Filter & Action Toolbar */}
      <Card
        style={{
          borderRadius: 12,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
        styles={{ body: { padding: '16px 20px' } }}
      >
        <Row gutter={[16, 16]} align="middle" justify="space-between">
          <Col xs={24} lg={12}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
              <Input
                placeholder="Search documents by title or filename..."
                prefix={<Search size={16} style={{ color: '#94a3b8' }} />}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ width: 280, borderRadius: 8 }}
                allowClear
              />

              <Select
                value={departmentFilter}
                onChange={setDepartmentFilter}
                style={{ width: 170 }}
                options={[
                  { value: 'ALL', label: 'All Departments' },
                  ...departments.map((d) => ({ value: d.id, label: d.name })),
                  { value: 'UNASSIGNED', label: 'Unassigned' },
                ]}
              />

              <Select
                value={accessLevelFilter}
                onChange={setAccessLevelFilter}
                style={{ width: 150 }}
                options={[
                  { value: 'ALL', label: 'All Access Levels' },
                  { value: 'PUBLIC', label: 'Public' },
                  { value: 'INTERNAL', label: 'Internal' },
                  { value: 'CONFIDENTIAL', label: 'Confidential' },
                  { value: 'RESTRICTED', label: 'Restricted' },
                ]}
              />
            </div>
          </Col>

          <Col xs={24} lg={12} style={{ display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
            <Segmented
              value={viewMode}
              onChange={(val) => setViewMode(val as 'grid' | 'table')}
              options={[
                { value: 'grid', icon: <Grid size={16} /> },
                { value: 'table', icon: <List size={16} /> },
              ]}
              style={{ padding: 3 }}
            />

            <Button
              type="primary"
              icon={<UploadCloud size={16} />}
              onClick={handleOpenUpload}
              style={{
                borderRadius: 8,
                fontWeight: 600,
                boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)',
              }}
            >
              Upload Document
            </Button>
          </Col>
        </Row>
      </Card>

      {/* Main Content: Grid or Table */}
      {isLoading ? (
        <Card style={{ borderRadius: 12, textAlign: 'center', padding: 40 }}>
          <div style={{ color: '#64748b' }}>Loading documents vault...</div>
        </Card>
      ) : filteredDocuments.length === 0 ? (
        <Card style={{ borderRadius: 12, textAlign: 'center', padding: 60 }}>
          <Empty
            description={
              <div>
                <p style={{ fontWeight: 600, color: '#0f172a', fontSize: 16 }}>No documents found</p>
                <p style={{ color: '#64748b', fontSize: 13, marginTop: 4 }}>
                  Upload your first corporate knowledge asset to initiate cryptographic tracking.
                </p>
              </div>
            }
          >
            <Button type="primary" icon={<UploadCloud size={16} />} onClick={handleOpenUpload}>
              Upload Document
            </Button>
          </Empty>
        </Card>
      ) : viewMode === 'table' ? (
        <Card
          style={{
            borderRadius: 12,
            border: '1px solid #e2e8f0',
            overflow: 'hidden',
          }}
          styles={{ body: { padding: 0 } }}
        >
          <Table
            columns={columns}
            dataSource={filteredDocuments}
            rowKey="id"
            pagination={{ pageSize: 10, showSizeChanger: true }}
          />
        </Card>
      ) : (
        <Row gutter={[20, 20]}>
          {filteredDocuments.map((doc: DocumentResponseDto) => {
            const iconInfo = getFileIcon(doc.contentType, doc.originalFileName);
            const badge = getAccessBadgeConfig(doc.accessLevel);
            const deptName = doc.departmentId ? deptMap.get(doc.departmentId) : null;

            return (
              <Col xs={24} sm={12} md={8} xl={6} key={doc.id}>
                <div className="midone-doc-card">
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div className="midone-doc-icon-box" style={{ background: iconInfo.bg }}>
                        {iconInfo.icon}
                      </div>

                      <Dropdown
                        menu={{
                          items: [
                            {
                              key: 'version',
                              icon: <History size={14} />,
                              label: 'Commit New Version',
                              onClick: () => handleOpenVersionModal(doc),
                            },
                            {
                              key: 'acl',
                              icon: <Lock size={14} />,
                              label: 'Access Policies (ACL)',
                              onClick: () => handleOpenAclModal(doc),
                            },
                            {
                              type: 'divider',
                            },
                            {
                              key: 'delete',
                              icon: <Trash2 size={14} />,
                              label: 'Delete Document',
                              danger: true,
                              onClick: () => handleDeleteDoc(doc),
                            },
                          ],
                        }}
                        trigger={['click']}
                      >
                        <Button shape="circle" icon={<MoreVertical size={16} />} type="text" />
                      </Dropdown>
                    </div>

                    <div style={{ marginTop: 14 }}>
                      <Tooltip title={doc.title || doc.originalFileName}>
                        <div
                          style={{
                            fontWeight: 700,
                            color: '#0f172a',
                            fontSize: 14.5,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}
                        >
                          {doc.title || doc.originalFileName}
                        </div>
                      </Tooltip>
                      <div
                        style={{
                          fontSize: 12,
                          color: '#94a3b8',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          marginTop: 2,
                        }}
                      >
                        {doc.originalFileName}
                      </div>
                    </div>

                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 12 }}>
                      <Tag color={badge.color} style={{ borderRadius: 6, fontWeight: 600, margin: 0, fontSize: 11 }}>
                        {doc.accessLevel}
                      </Tag>
                      {deptName && (
                        <Tag color="blue" style={{ borderRadius: 6, margin: 0, fontSize: 11 }}>
                          {deptName}
                        </Tag>
                      )}
                    </div>
                  </div>

                  <div
                    style={{
                      borderTop: '1px solid #f1f5f9',
                      paddingTop: 12,
                      marginTop: 16,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      fontSize: 12,
                      color: '#64748b',
                    }}
                  >
                    <span>{formatBytes(doc.fileSizeBytes)}</span>
                    <span>{formatDate(doc.updatedAt || doc.createdAt)}</span>
                  </div>
                </div>
              </Col>
            );
          })}
        </Row>
      )}

      {/* Upload Document Modal */}
      <Modal
        title="Upload Corporate Document"
        open={isUploadModalOpen}
        onCancel={() => setIsUploadModalOpen(false)}
        onOk={handleUploadSubmit}
        confirmLoading={submitting}
        okText="Upload & Encrypt"
        destroyOnClose
      >
        <Form form={uploadForm} layout="vertical" initialValues={{ accessLevel: 'INTERNAL' }}>
          <Form.Item
            name="title"
            label="Document Title"
            rules={[{ required: true, message: 'Please specify document title' }]}
          >
            <Input placeholder="e.g. Q3 Security Architecture Whitepaper" />
          </Form.Item>

          <Form.Item name="description" label="Description / Knowledge Summary">
            <Input.TextArea rows={2} placeholder="Optional contextual summary" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="accessLevel"
                label="Access Tier"
                rules={[{ required: true }]}
              >
                <Select
                  options={[
                    { value: 'PUBLIC', label: 'Public (Open to All)' },
                    { value: 'INTERNAL', label: 'Internal (Employees Only)' },
                    { value: 'CONFIDENTIAL', label: 'Confidential (Designated Depts)' },
                    { value: 'RESTRICTED', label: 'Restricted (High Executive Only)' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="departmentId" label="Owning Department">
                <Select
                  allowClear
                  placeholder="Select Department"
                  options={departments.map((d) => ({ value: d.id, label: d.name }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item label="Upload File Asset" required>
            <Dragger
              fileList={fileList}
              beforeUpload={(file) => {
                setFileList([file]);
                return false;
              }}
              onRemove={() => setFileList([])}
              maxCount={1}
            >
              <p className="ant-upload-drag-icon">
                <UploadCloud size={32} style={{ color: '#2563eb' }} />
              </p>
              <p style={{ fontWeight: 600, color: '#0f172a' }}>
                Click or drag file to this area to upload
              </p>
              <p style={{ fontSize: 12, color: '#64748b' }}>
                Support for PDF, DOCX, XLSX, JSON, CSV and ZIP. Payload will be encrypted with SHA-256 integrity checks.
              </p>
            </Dragger>
          </Form.Item>
        </Form>
      </Modal>

      {/* Upload Version Modal */}
      <Modal
        title={`Commit New Version: ${selectedDoc?.title || selectedDoc?.originalFileName}`}
        open={isVersionModalOpen}
        onCancel={() => setIsVersionModalOpen(false)}
        onOk={handleVersionSubmit}
        confirmLoading={submitting}
        okText="Commit Version"
        destroyOnClose
      >
        <Form form={versionForm} layout="vertical">
          <Form.Item
            name="changeSummary"
            label="Change Summary"
            rules={[{ required: true, message: 'Please describe the changes in this version' }]}
          >
            <Input.TextArea rows={3} placeholder="e.g. Updated Section 4 with new IAM policies and audit keys" />
          </Form.Item>

          <Form.Item label="New File Version" required>
            <Dragger
              fileList={versionFileList}
              beforeUpload={(file) => {
                setVersionFileList([file]);
                return false;
              }}
              onRemove={() => setVersionFileList([])}
              maxCount={1}
            >
              <p className="ant-upload-drag-icon">
                <History size={32} style={{ color: '#2563eb' }} />
              </p>
              <p style={{ fontWeight: 600, color: '#0f172a' }}>
                Click or drag updated version file here
              </p>
            </Dragger>
          </Form.Item>
        </Form>
      </Modal>

      {/* ACL Permissions Modal */}
      <Modal
        title={`Access Policies (ACL): ${selectedDoc?.title || selectedDoc?.originalFileName}`}
        open={isAclModalOpen}
        onCancel={() => setIsAclModalOpen(false)}
        onOk={handleAclSubmit}
        confirmLoading={submitting}
        okText="Save Policies"
        destroyOnClose
      >
        <Form form={aclForm} layout="vertical">
          <Form.Item
            name="accessLevel"
            label="Global Access Classification"
            rules={[{ required: true }]}
          >
            <Select
              options={[
                { value: 'PUBLIC', label: 'PUBLIC - Open to everyone without restriction' },
                { value: 'INTERNAL', label: 'INTERNAL - Restricted to authenticated platform personnel' },
                { value: 'CONFIDENTIAL', label: 'CONFIDENTIAL - Requires explicit role or department grant' },
                { value: 'RESTRICTED', label: 'RESTRICTED - Security vault level (Admin/Executive only)' },
              ]}
            />
          </Form.Item>

          <div
            style={{
              background: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: 8,
              padding: 12,
              fontSize: 13,
              color: '#475569',
            }}
          >
            <div style={{ fontWeight: 600, color: '#0f172a', marginBottom: 4 }}>
              Cryptographic Enclave Security
            </div>
            Access control modifications are tracked in the immutable security audit log with SHA-256 fingerprinting.
          </div>
        </Form>
      </Modal>
    </div>
  );
};
