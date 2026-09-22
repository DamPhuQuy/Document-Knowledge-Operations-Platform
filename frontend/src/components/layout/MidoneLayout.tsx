import React, { useState } from 'react';
import {
  Avatar,
  Badge,
  Button,
  Drawer,
  Dropdown,
  Popover,
  Tag,
  Tooltip,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  Files,
  Building2,
  ShieldCheck,
  Bell,
  LogOut,
  LogIn,
  Menu as MenuIcon,
  ChevronRight,
  Server,
  KeyRound,
  FileText,
  User as UserIcon,
} from 'lucide-react';
import logoSvg from '@/assets/images/logo.svg';
import { useAuth } from '@/context/AuthContext';
import { useDocument } from '@/context/DocumentContext';
import { config } from '@/config';

interface MidoneLayoutProps {
  activeTab: 'documents' | 'organization' | 'audit-logs' | 'auth';
  setActiveTab: (tab: 'documents' | 'organization' | 'audit-logs' | 'auth') => void;
  children: React.ReactNode;
}

export const MidoneLayout: React.FC<MidoneLayoutProps> = ({
  activeTab,
  setActiveTab,
  children,
}) => {
  const { user, isAuthenticated, logout, isAdmin, isManager, isAuditor } = useAuth();
  const { documentCount } = useDocument();
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  const getBreadcrumbTitle = () => {
    switch (activeTab) {
      case 'documents':
        return 'Document Knowledge Workspace';
      case 'organization':
        return 'Organization & Role Management';
      case 'audit-logs':
        return 'Security & Audit Governance';
      case 'auth':
        return 'Authentication & Identity Verification';
      default:
        return 'Workspace';
    }
  };

  const notificationContent = (
    <div style={{ width: 300 }}>
      <div style={{ padding: '8px 12px', borderBottom: '1px solid #f1f5f9', fontWeight: 600, color: '#0f172a' }}>
        System Notifications
      </div>
      <div style={{ maxHeight: 240, overflowY: 'auto' }}>
        <div style={{ padding: '10px 12px', borderBottom: '1px solid #f8fafc', fontSize: 13 }}>
          <div style={{ fontWeight: 600, color: '#2563eb' }}>Cryptographic Vault Active</div>
          <div style={{ color: '#64748b', fontSize: 12 }}>AES-GCM-256 and SHA-256 integrity checks enabled.</div>
          <div style={{ color: '#94a3b8', fontSize: 11, marginTop: 4 }}>Just now</div>
        </div>
        <div style={{ padding: '10px 12px', borderBottom: '1px solid #f8fafc', fontSize: 13 }}>
          <div style={{ fontWeight: 600, color: '#10b981' }}>Cluster Telemetry Synced</div>
          <div style={{ color: '#64748b', fontSize: 12 }}>Connected to API at {config.apiBaseUrl}</div>
          <div style={{ color: '#94a3b8', fontSize: 11, marginTop: 4 }}>5m ago</div>
        </div>
        <div style={{ padding: '10px 12px', fontSize: 13 }}>
          <div style={{ fontWeight: 600, color: '#0f172a' }}>RBAC Department Enforced</div>
          <div style={{ color: '#64748b', fontSize: 12 }}>Access levels (PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED) active.</div>
          <div style={{ color: '#94a3b8', fontSize: 11, marginTop: 4 }}>1h ago</div>
        </div>
      </div>
    </div>
  );

  const accountPopoverContent = (
    <div style={{ width: 260, padding: 4 }}>
      {isAuthenticated && user ? (
        <>
          <div style={{ padding: '8px 12px', borderBottom: '1px solid #f1f5f9' }}>
            <div style={{ fontWeight: 700, fontSize: 14, color: '#0f172a' }}>
              {user.fullName || user.email}
            </div>
            <div style={{ fontSize: 12, color: '#64748b' }}>{user.email}</div>
            <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
              {user.roles.map((role) => (
                <Tag
                  key={role}
                  color={
                    role === 'ROLE_ADMIN'
                      ? 'red'
                      : role === 'ROLE_MANAGER'
                      ? 'blue'
                      : role === 'LEGAL_AUDITOR'
                      ? 'purple'
                      : 'green'
                  }
                  style={{ fontSize: 10, margin: 0 }}
                >
                  {role.replace('ROLE_', '')}
                </Tag>
              ))}
            </div>
          </div>

          <div style={{ padding: '8px 4px' }}>
            <button
              className="midone-menu-item"
              style={{ color: '#334155', padding: '8px 12px' }}
              onClick={() => setActiveTab('organization')}
            >
              <div className="midone-menu-item-left">
                <Building2 size={16} />
                <span>Organization</span>
              </div>
            </button>
            <button
              className="midone-menu-item"
              style={{ color: '#334155', padding: '8px 12px' }}
              onClick={() => setActiveTab('audit-logs')}
            >
              <div className="midone-menu-item-left">
                <ShieldCheck size={16} />
                <span>Audit Logs</span>
              </div>
            </button>
          </div>

          <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: 6 }}>
            <Button
              type="text"
              danger
              icon={<LogOut size={16} />}
              block
              style={{ textAlign: 'left', display: 'flex', alignItems: 'center', gap: 8 }}
              onClick={logout}
            >
              Sign Out
            </Button>
          </div>
        </>
      ) : (
        <div style={{ padding: 12, textAlign: 'center' }}>
          <div style={{ fontSize: 13, color: '#64748b', marginBottom: 12 }}>
            You are operating in demo mode. Sign in to access full administrative privileges.
          </div>
          <Button
            type="primary"
            icon={<LogIn size={16} />}
            block
            onClick={() => setActiveTab('auth')}
          >
            Sign In / Register
          </Button>
        </div>
      )}
    </div>
  );

  const navigationItems = (
    <>
      <div className="midone-menu-section">WORKSPACE</div>
      <button
        className={`midone-menu-item ${activeTab === 'documents' ? 'active' : ''}`}
        onClick={() => {
          setActiveTab('documents');
          setMobileDrawerOpen(false);
        }}
      >
        <div className="midone-menu-item-left">
          <Files size={18} />
          <span>Documents</span>
        </div>
        <span className="midone-menu-badge">{documentCount}</span>
      </button>

      <div className="midone-menu-section">ADMINISTRATION</div>
      <button
        className={`midone-menu-item ${activeTab === 'organization' ? 'active' : ''}`}
        onClick={() => {
          setActiveTab('organization');
          setMobileDrawerOpen(false);
        }}
      >
        <div className="midone-menu-item-left">
          <Building2 size={18} />
          <span>Organization & Roles</span>
        </div>
        {isAdmin && <Tag color="gold" style={{ fontSize: 10, margin: 0 }}>ADMIN</Tag>}
      </button>

      <button
        className={`midone-menu-item ${activeTab === 'audit-logs' ? 'active' : ''}`}
        onClick={() => {
          setActiveTab('audit-logs');
          setMobileDrawerOpen(false);
        }}
      >
        <div className="midone-menu-item-left">
          <ShieldCheck size={18} />
          <span>Audit Trail</span>
        </div>
        {isAuditor && <Tag color="purple" style={{ fontSize: 10, margin: 0 }}>AUDITOR</Tag>}
      </button>

      <div className="midone-menu-section">SECURITY & ACCESS</div>
      <button
        className={`midone-menu-item ${activeTab === 'auth' ? 'active' : ''}`}
        onClick={() => {
          setActiveTab('auth');
          setMobileDrawerOpen(false);
        }}
      >
        <div className="midone-menu-item-left">
          <KeyRound size={18} />
          <span>{isAuthenticated ? 'Identity & Session' : 'Sign In / Register'}</span>
        </div>
        {isAuthenticated ? (
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981' }} />
        ) : (
          <span className="midone-menu-badge">Guest</span>
        )}
      </button>
    </>
  );

  return (
    <div className="midone-layout">
      {/* Desktop Sidebar */}
      <aside className="midone-sidebar">
        <div className="midone-sidebar-brand">
          <div className="midone-brand-icon">
            <img src={logoSvg} alt="Midone DocOps" style={{ width: 22, height: 22 }} />
          </div>
          <div className="midone-brand-text">
            <span className="midone-brand-title">DocOps</span>
            <span className="midone-brand-sub">Platform v2.4</span>
          </div>
        </div>

        <div className="midone-sidebar-menu">{navigationItems}</div>

        <div className="midone-sidebar-footer">
          <div className="midone-system-pill">
            <div className="midone-pulse-dot" />
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <span style={{ fontSize: 12, fontWeight: 600, color: '#f8fafc' }}>
                Secure Gateway
              </span>
              <span style={{ fontSize: 10, color: '#94a3b8' }}>
                {config.appEnv.toUpperCase()} • TLS 1.3
              </span>
            </div>
          </div>
        </div>
      </aside>

      {/* Mobile Drawer */}
      <Drawer
        placement="left"
        open={mobileDrawerOpen}
        onClose={() => setMobileDrawerOpen(false)}
        styles={{ body: { padding: 0, background: '#0f172a' } }}
        width={260}
      >
        <div className="midone-sidebar-brand" style={{ background: '#0f172a' }}>
          <div className="midone-brand-icon">
            <img src={logoSvg} alt="Midone DocOps" style={{ width: 22, height: 22 }} />
          </div>
          <div className="midone-brand-text">
            <span className="midone-brand-title">DocOps</span>
            <span className="midone-brand-sub">Platform v2.4</span>
          </div>
        </div>
        <div className="midone-sidebar-menu">{navigationItems}</div>
      </Drawer>

      {/* Main Content Area */}
      <main className="midone-main">
        {/* Topbar */}
        <header className="midone-topbar">
          <div className="midone-topbar-left">
            <Button
              type="text"
              icon={<MenuIcon size={20} />}
              className="md:hidden"
              onClick={() => setMobileDrawerOpen(true)}
              style={{ display: 'inline-flex', alignItems: 'center' }}
            />

            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#64748b' }}>
              <span style={{ fontWeight: 600, color: '#0f172a' }}>DocOps</span>
              <ChevronRight size={14} />
              <span>Workspace</span>
              <ChevronRight size={14} />
              <span style={{ color: '#2563eb', fontWeight: 600 }}>{getBreadcrumbTitle()}</span>
            </div>
          </div>

          <div className="midone-topbar-right">
            {/* Telemetry pill */}
            <div className="midone-telemetry-badge">
              <div className="midone-pulse-dot" style={{ width: 6, height: 6 }} />
              <Server size={14} style={{ color: '#2563eb' }} />
              <span>
                API: <strong style={{ color: '#0f172a' }}>{config.apiBaseUrl}</strong>
              </span>
            </div>

            {/* Notification Popover */}
            <Popover content={notificationContent} trigger="click" placement="bottomRight">
              <Badge dot status="processing">
                <Button
                  shape="circle"
                  icon={<Bell size={18} style={{ color: '#475569' }} />}
                  style={{ border: '1px solid #e2e8f0' }}
                />
              </Badge>
            </Popover>

            {/* Account Popover */}
            <Popover content={accountPopoverContent} trigger="click" placement="bottomRight">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  cursor: 'pointer',
                  padding: '4px 8px',
                  borderRadius: 20,
                  transition: 'background 0.15s',
                }}
              >
                <Avatar
                  style={{
                    backgroundColor: isAuthenticated ? '#2563eb' : '#94a3b8',
                    verticalAlign: 'middle',
                    fontWeight: 600,
                  }}
                  icon={!isAuthenticated ? <UserIcon size={16} /> : undefined}
                >
                  {isAuthenticated && user ? (user.fullName || user.email).substring(0, 2).toUpperCase() : ''}
                </Avatar>
                <div style={{ display: 'flex', flexDirection: 'column', textAlign: 'left' }} className="hidden sm:flex">
                  <span style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', lineHeight: 1.2 }}>
                    {isAuthenticated && user ? user.fullName || user.email : 'Guest User'}
                  </span>
                  <span style={{ fontSize: 11, color: '#64748b' }}>
                    {isAuthenticated && user ? user.roles[0]?.replace('ROLE_', '') || 'User' : 'Sign in'}
                  </span>
                </div>
              </div>
            </Popover>
          </div>
        </header>

        {/* Content Body */}
        <div className="midone-content-body">{children}</div>
      </main>
    </div>
  );
};
