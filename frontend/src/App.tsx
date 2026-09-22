import React, { useState, useEffect } from 'react';
import { ConfigProvider, App as AntdApp } from 'antd';
import { midoneAntdTheme } from '@/theme/themeConfig';
import {
  AuthProvider,
  DepartmentProvider,
  DocumentProvider,
  AuditProvider,
} from '@/context';
import { MidoneLayout } from '@/components/layout/MidoneLayout';
import { DocumentsWorkspace } from '@/features/documents/DocumentsWorkspace';
import { OrganizationWorkspace } from '@/features/organization/OrganizationWorkspace';
import { AuditTrailWorkspace } from '@/features/audit/AuditTrailWorkspace';
import { AuthScreen } from '@/features/auth/AuthScreen';
import './App.css';

type ActiveTab = 'documents' | 'organization' | 'audit-logs' | 'auth';

const AppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>(() => {
    const hash = window.location.hash.replace('#', '');
    if (['documents', 'organization', 'audit-logs', 'auth'].includes(hash)) {
      return hash as ActiveTab;
    }
    return 'documents';
  });

  useEffect(() => {
    window.location.hash = activeTab;
    const handleHashChange = () => {
      const hash = window.location.hash.replace('#', '');
      if (['documents', 'organization', 'audit-logs', 'auth'].includes(hash)) {
        setActiveTab(hash as ActiveTab);
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [activeTab]);

  // If auth tab is selected and rendered as dedicated split-screen
  if (activeTab === 'auth') {
    return (
      <div style={{ position: 'relative' }}>
        <button
          onClick={() => setActiveTab('documents')}
          style={{
            position: 'absolute',
            top: 20,
            left: 20,
            zIndex: 100,
            background: 'rgba(255, 255, 255, 0.2)',
            color: '#ffffff',
            border: '1px solid rgba(255, 255, 255, 0.4)',
            padding: '8px 16px',
            borderRadius: 20,
            cursor: 'pointer',
            fontSize: 13,
            fontWeight: 600,
            backdropFilter: 'blur(8px)',
          }}
        >
          ← Return to Workspace
        </button>
        <AuthScreen onSuccess={() => setActiveTab('documents')} />
      </div>
    );
  }

  return (
    <MidoneLayout activeTab={activeTab} setActiveTab={setActiveTab}>
      {activeTab === 'documents' && <DocumentsWorkspace />}
      {activeTab === 'organization' && <OrganizationWorkspace />}
      {activeTab === 'audit-logs' && <AuditTrailWorkspace />}
    </MidoneLayout>
  );
};

export const App: React.FC = () => {
  return (
    <ConfigProvider theme={midoneAntdTheme}>
      <AntdApp>
        <AuthProvider>
          <DepartmentProvider>
            <DocumentProvider>
              <AuditProvider>
                <div className="docops-app-container">
                  <AppContent />
                </div>
              </AuditProvider>
            </DocumentProvider>
          </DepartmentProvider>
        </AuthProvider>
      </AntdApp>
    </ConfigProvider>
  );
};

export default App;
