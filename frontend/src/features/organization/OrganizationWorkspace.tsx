import React, { useState } from 'react';
import {
  Button,
  Card,
  Checkbox,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  Building2,
  Users,
  ShieldCheck,
  Plus,
  Edit,
  UserCheck,
  KeyRound,
  CheckCircle,
} from 'lucide-react';
import { useDepartment } from '@/context/DepartmentContext';
import { useAuth } from '@/context/AuthContext';
import { formatDate } from '@/utils/formatters';
import type {
  DepartmentResponseDto,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
} from '@/types';

export const OrganizationWorkspace: React.FC = () => {
  const {
    departments,
    loading,
    actionLoading,
    createDepartment,
    updateDepartment,
    assignDepartment,
    assignRoles,
  } = useDepartment();

  const { isAdmin, isManager } = useAuth();

  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingDept, setEditingDept] = useState<DepartmentResponseDto | null>(null);

  // Forms
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [assignDeptForm] = Form.useForm();
  const [assignRoleForm] = Form.useForm();

  // Create Department
  const handleCreateDepartment = async () => {
    try {
      const values = await createForm.validateFields();
      await createDepartment(values as CreateDepartmentRequest);
      message.success('Department created successfully');
      createForm.resetFields();
      setIsCreateModalOpen(false);
    } catch {
      message.error('Failed to create department');
    }
  };

  // Edit Department
  const handleOpenEdit = (dept: DepartmentResponseDto) => {
    setEditingDept(dept);
    editForm.setFieldsValue({
      code: dept.code,
      name: dept.name,
      description: dept.description,
    });
    setIsEditModalOpen(true);
  };

  const handleUpdateDepartment = async () => {
    if (!editingDept) return;
    try {
      const values = await editForm.validateFields();
      await updateDepartment(editingDept.id, values as UpdateDepartmentRequest);
      message.success('Department updated successfully');
      setIsEditModalOpen(false);
    } catch {
      message.error('Failed to update department');
    }
  };

  // Assign user to department
  const handleAssignDept = async () => {
    try {
      const values = await assignDeptForm.validateFields();
      await assignDepartment(values.userId, {
        departmentId: values.departmentId || null,
        isInternal: !!values.isInternal,
      });
      message.success(`User ${values.userId} department assigned`);
      assignDeptForm.resetFields();
    } catch {
      message.error('Failed to assign user department');
    }
  };

  // Assign roles
  const handleAssignRoles = async () => {
    try {
      const values = await assignRoleForm.validateFields();
      await assignRoles(values.userId, {
        roleIds: values.roleIds,
      });
      message.success(`Roles assigned to user ${values.userId}`);
      assignRoleForm.resetFields();
    } catch {
      message.error('Failed to provision roles');
    }
  };

  const departmentColumns: ColumnsType<DepartmentResponseDto> = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      render: (code: string) => (
        <Tag color="blue" style={{ fontWeight: 700, borderRadius: 6 }}>
          {code}
        </Tag>
      ),
    },
    {
      title: 'Department Name',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <strong style={{ color: '#0f172a' }}>{name}</strong>,
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      render: (desc: string | null) => (
        <span style={{ color: '#64748b', fontSize: 13 }}>{desc || '—'}</span>
      ),
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => (
        <span style={{ color: '#94a3b8', fontSize: 12 }}>{formatDate(date)}</span>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 90,
      render: (_, record) => (
        <Tooltip title="Edit Department Details">
          <Button
            shape="circle"
            icon={<Edit size={14} />}
            type="text"
            onClick={() => handleOpenEdit(record)}
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
            Organizational Structure & Governance
          </h2>
          <p style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>
            Manage organizational divisions, allocate corporate personnel, and provision cryptographic RBAC roles.
          </p>
        </div>

        <Button
          type="primary"
          icon={<Plus size={16} />}
          onClick={() => {
            createForm.resetFields();
            setIsCreateModalOpen(true);
          }}
          style={{
            borderRadius: 8,
            fontWeight: 600,
            boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)',
          }}
        >
          New Department
        </Button>
      </div>

      {/* Departments Directory Table */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15 }}>
            <Building2 size={18} style={{ color: '#2563eb' }} />
            <span>Departments Directory ({departments.length})</span>
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
          columns={departmentColumns}
          dataSource={departments}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 5 }}
        />
      </Card>

      {/* User Management Row */}
      <Row gutter={[24, 24]}>
        {/* Assign User to Department */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15 }}>
                <UserCheck size={18} style={{ color: '#10b981' }} />
                <span>Assign User to Department</span>
              </div>
            }
            style={{
              borderRadius: 12,
              border: '1px solid #e2e8f0',
              height: '100%',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Form form={assignDeptForm} layout="vertical" onFinish={handleAssignDept}>
              <Form.Item
                name="userId"
                label="Target User ID / Username"
                rules={[{ required: true, message: 'Please input user ID' }]}
              >
                <Input placeholder="e.g. usr-eng-02 or user@company.com" />
              </Form.Item>

              <Form.Item
                name="departmentId"
                label="Department Unit"
                rules={[{ required: true, message: 'Select a department' }]}
              >
                <Select
                  placeholder="Select organizational department"
                  options={departments.map((d) => ({
                    value: d.id,
                    label: `${d.name} (${d.code})`,
                  }))}
                />
              </Form.Item>

              <Form.Item name="isInternal" valuePropName="checked" initialValue={true}>
                <Checkbox>Mark user as Internal Corporate Personnel</Checkbox>
              </Form.Item>

              <Button
                type="primary"
                htmlType="submit"
                loading={actionLoading}
                block
                style={{ borderRadius: 8, fontWeight: 600 }}
              >
                Confirm Department Assignment
              </Button>
            </Form>
          </Card>
        </Col>

        {/* Provision Roles */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15 }}>
                <KeyRound size={18} style={{ color: '#8b5cf6' }} />
                <span>Provision System Roles (RBAC)</span>
              </div>
            }
            style={{
              borderRadius: 12,
              border: '1px solid #e2e8f0',
              height: '100%',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <Form form={assignRoleForm} layout="vertical" onFinish={handleAssignRoles}>
              <Form.Item
                name="userId"
                label="Target User ID / Username"
                rules={[{ required: true, message: 'Please input user ID' }]}
              >
                <Input placeholder="e.g. usr-admin-01 or auditor@company.com" />
              </Form.Item>

              <Form.Item
                name="roleIds"
                label="Assigned System Roles"
                rules={[{ required: true, message: 'Select at least one role' }]}
              >
                <Select
                  mode="multiple"
                  placeholder="Select roles to provision"
                  options={[
                    { value: 'ROLE_ADMIN', label: 'ROLE_ADMIN (Super Administrator)' },
                    { value: 'ROLE_MANAGER', label: 'ROLE_MANAGER (Department Manager)' },
                    { value: 'ROLE_STAFF', label: 'ROLE_STAFF (Corporate Staff Member)' },
                    { value: 'LEGAL_AUDITOR', label: 'LEGAL_AUDITOR (Security & Compliance Auditor)' },
                  ]}
                />
              </Form.Item>

              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: 8,
                  padding: 10,
                  fontSize: 12,
                  color: '#64748b',
                  marginBottom: 16,
                }}
              >
                Privilege elevations are subject to dual-key authorization and instant audit logging.
              </div>

              <Button
                type="primary"
                htmlType="submit"
                loading={actionLoading}
                block
                style={{
                  borderRadius: 8,
                  fontWeight: 600,
                  background: '#8b5cf6',
                  borderColor: '#8b5cf6',
                }}
              >
                Apply Role Entitlements
              </Button>
            </Form>
          </Card>
        </Col>
      </Row>

      {/* Create Department Modal */}
      <Modal
        title="Create New Department"
        open={isCreateModalOpen}
        onCancel={() => setIsCreateModalOpen(false)}
        onOk={handleCreateDepartment}
        confirmLoading={actionLoading}
        okText="Create Department"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="code"
            label="Department Code"
            rules={[{ required: true, message: 'Code is required' }]}
          >
            <Input placeholder="e.g. FIN, HR, SEC, ENG" style={{ textTransform: 'uppercase' }} />
          </Form.Item>

          <Form.Item
            name="name"
            label="Department Name"
            rules={[{ required: true, message: 'Name is required' }]}
          >
            <Input placeholder="e.g. Finance & Treasury" />
          </Form.Item>

          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} placeholder="Department charter and purpose" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Department Modal */}
      <Modal
        title={`Edit Department: ${editingDept?.name}`}
        open={isEditModalOpen}
        onCancel={() => setIsEditModalOpen(false)}
        onOk={handleUpdateDepartment}
        confirmLoading={actionLoading}
        okText="Save Changes"
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="code"
            label="Department Code"
            rules={[{ required: true, message: 'Code is required' }]}
          >
            <Input style={{ textTransform: 'uppercase' }} />
          </Form.Item>

          <Form.Item
            name="name"
            label="Department Name"
            rules={[{ required: true, message: 'Name is required' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
