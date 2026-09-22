import React, { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Divider,
  Form,
  Input,
  Tabs,
  Tag,
  message,
} from 'antd';
import {
  Lock,
  Mail,
  User,
  ShieldCheck,
  KeyRound,
  ArrowRight,
  CheckCircle2,
  HardDrive,
  LogOut,
  Sparkles,
} from 'lucide-react';
import logoSvg from '@/assets/images/logo.svg';
import illustrationSvg from '@/assets/images/illustration.svg';
import { useAuth } from '@/context/AuthContext';
import type { LoginRequest, RegisterRequest, VerifyOtpRequest } from '@/types';

interface AuthScreenProps {
  onSuccess?: () => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({ onSuccess }) => {
  const {
    user,
    isAuthenticated,
    actionLoading,
    error,
    login,
    register,
    verifyOtp,
    logout,
    clearError,
  } = useAuth();

  const [activeTab, setActiveTab] = useState<'signin' | 'register'>('signin');
  const [otpStep, setOtpStep] = useState(false);
  const [pendingEmail, setPendingEmail] = useState('');

  const [loginForm] = Form.useForm();
  const [registerForm] = Form.useForm();
  const [otpForm] = Form.useForm();

  const handleLogin = async () => {
    try {
      clearError();
      const values = await loginForm.validateFields();
      await login({
        email: values.email,
        password: values.password,
      } as LoginRequest);
      message.success('Authenticated successfully');
      if (onSuccess) onSuccess();
    } catch {
      // Handled by context error
    }
  };

  const handleRegister = async () => {
    try {
      clearError();
      const values = await registerForm.validateFields();
      await register({
        email: values.email,
        password: values.password,
        firstName: values.firstName,
        lastName: values.lastName,
      } as RegisterRequest);

      setPendingEmail(values.email);
      message.success('Account registered successfully. If required, verify with OTP or proceed to sign in.');
      setActiveTab('signin');
    } catch {
      // Handled by context error
    }
  };

  const handleVerifyOtp = async () => {
    try {
      clearError();
      const values = await otpForm.validateFields();
      await verifyOtp({
        email: pendingEmail,
        otp: values.otp,
      } as VerifyOtpRequest);
      message.success('Identity verified. Welcome to DocOps Platform.');
      setOtpStep(false);
      if (onSuccess) onSuccess();
    } catch {
      // Handled by context error
    }
  };

  return (
    <div className="midone-auth-wrapper">
      {/* Brand Left Panel */}
      <div className="midone-auth-brand-side">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div className="midone-brand-icon">
            <img src={logoSvg} alt="DocOps Logo" style={{ width: 22, height: 22 }} />
          </div>
          <span style={{ fontSize: 20, fontWeight: 700, letterSpacing: -0.5 }}>
            DocOps Platform
          </span>
        </div>

        <div style={{ textAlign: 'center', margin: 'auto 0' }}>
          <img
            src={illustrationSvg}
            alt="Midone Vector"
            style={{ width: '80%', maxWidth: 360, margin: '0 auto 24px', display: 'block' }}
          />
          <h1 style={{ fontSize: 28, fontWeight: 800, color: '#ffffff', lineHeight: 1.25 }}>
            Enterprise Document Knowledge & Operations Platform
          </h1>
          <p style={{ color: '#bfdbfe', fontSize: 14, maxWidth: 440, margin: '12px auto 0' }}>
            Multi-tenant cryptographic document storage, fine-grained RBAC access policies, and tamper-evident audit trails.
          </p>

          <div
            style={{
              display: 'flex',
              justifyContent: 'center',
              gap: 20,
              marginTop: 28,
              fontSize: 12,
              color: '#93c5fd',
            }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <ShieldCheck size={16} style={{ color: '#6ee7b7' }} />
              AES-GCM-256
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <HardDrive size={16} style={{ color: '#6ee7b7' }} />
              Zero-Trust ACL
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <KeyRound size={16} style={{ color: '#6ee7b7' }} />
              2FA Challenge
            </span>
          </div>
        </div>

        <div style={{ fontSize: 11, color: '#93c5fd' }}>
          Midone Enterprise Design System • Powered by React 19 & Ant Design
        </div>
      </div>

      {/* Right Form Panel */}
      <div className="midone-auth-form-side">
        <div style={{ width: '100%', maxWidth: 380 }}>
          {isAuthenticated && user ? (
            <Card
              style={{
                borderRadius: 14,
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
                textAlign: 'center',
                padding: '20px 10px',
              }}
            >
              <div
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: '50%',
                  background: '#eff6ff',
                  color: '#2563eb',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 16px',
                }}
              >
                <CheckCircle2 size={32} />
              </div>
              <h3 style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>
                Active Session Established
              </h3>
              <p style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                Logged in as <strong>{user.fullName || user.email}</strong>
              </p>
              <div style={{ fontSize: 12, color: '#94a3b8' }}>{user.email}</div>

              <div style={{ display: 'flex', justifyContent: 'center', gap: 6, margin: '14px 0' }}>
                {user.roles.map((r) => (
                  <Tag color="blue" key={r} style={{ borderRadius: 6 }}>
                    {r.replace('ROLE_', '')}
                  </Tag>
                ))}
              </div>

              <Divider style={{ margin: '16px 0' }} />

              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {onSuccess && (
                  <Button
                    type="primary"
                    block
                    icon={<ArrowRight size={16} />}
                    onClick={onSuccess}
                    style={{ borderRadius: 8, fontWeight: 600 }}
                  >
                    Enter Workspace
                  </Button>
                )}

                <Button
                  danger
                  block
                  icon={<LogOut size={16} />}
                  onClick={logout}
                  style={{ borderRadius: 8 }}
                >
                  Sign Out of Platform
                </Button>
              </div>
            </Card>
          ) : otpStep ? (
            <Card
              style={{
                borderRadius: 14,
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
              }}
            >
              <div style={{ textAlign: 'center', marginBottom: 20 }}>
                <div
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: 12,
                    background: '#fef3c7',
                    color: '#d97706',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 12px',
                  }}
                >
                  <KeyRound size={24} />
                </div>
                <h3 style={{ fontSize: 18, fontWeight: 700, color: '#0f172a' }}>
                  Two-Factor Authentication
                </h3>
                <p style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                  Enter the 6-digit code sent to <strong>{pendingEmail}</strong>
                </p>
              </div>

              {error && (
                <Alert
                  message={error}
                  type="error"
                  showIcon
                  style={{ marginBottom: 16, borderRadius: 8 }}
                />
              )}

              <Form form={otpForm} layout="vertical" onFinish={handleVerifyOtp}>
                <Form.Item
                  name="otp"
                  label="Verification Code (OTP)"
                  rules={[
                    { required: true, message: 'Please enter verification code' },
                    { len: 6, message: 'OTP must be exactly 6 characters' },
                  ]}
                >
                  <Input
                    placeholder="123456"
                    maxLength={6}
                    style={{
                      textAlign: 'center',
                      fontSize: 20,
                      letterSpacing: 8,
                      fontWeight: 700,
                      borderRadius: 8,
                    }}
                  />
                </Form.Item>

                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  loading={actionLoading}
                  style={{
                    borderRadius: 8,
                    fontWeight: 600,
                    height: 40,
                    boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)',
                  }}
                >
                  Verify Identity & Sign In
                </Button>

                <Button
                  type="link"
                  block
                  onClick={() => setOtpStep(false)}
                  style={{ marginTop: 8 }}
                >
                  Back to Sign In
                </Button>
              </Form>
            </Card>
          ) : (
            <Card
              style={{
                borderRadius: 14,
                border: '1px solid #e2e8f0',
                boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
              }}
            >
              <div style={{ marginBottom: 20 }}>
                <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0f172a' }}>
                  {activeTab === 'signin' ? 'Sign In to DocOps' : 'Create Account'}
                </h2>
                <p style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>
                  {activeTab === 'signin'
                    ? 'Enter your platform credentials to access secure vaults'
                    : 'Register for cryptographic document knowledge access'}
                </p>
              </div>

              {error && (
                <Alert
                  message={error}
                  type="error"
                  showIcon
                  style={{ marginBottom: 16, borderRadius: 8 }}
                />
              )}

              <Tabs
                activeKey={activeTab}
                onChange={(k) => {
                  clearError();
                  setActiveTab(k as 'signin' | 'register');
                }}
                items={[
                  { key: 'signin', label: 'Sign In' },
                  { key: 'register', label: 'Register New User' },
                ]}
              />

              {activeTab === 'signin' ? (
                <Form form={loginForm} layout="vertical" onFinish={handleLogin} style={{ marginTop: 16 }}>
                  <Form.Item
                    name="email"
                    label="Corporate Email"
                    rules={[
                      { required: true, message: 'Please input email' },
                      { type: 'email', message: 'Enter a valid email' },
                    ]}
                  >
                    <Input
                      prefix={<Mail size={16} style={{ color: '#94a3b8' }} />}
                      placeholder="admin@docops.internal"
                      style={{ borderRadius: 8 }}
                    />
                  </Form.Item>

                  <Form.Item
                    name="password"
                    label="Password"
                    rules={[{ required: true, message: 'Please input password' }]}
                  >
                    <Input.Password
                      prefix={<Lock size={16} style={{ color: '#94a3b8' }} />}
                      placeholder="••••••••"
                      style={{ borderRadius: 8 }}
                    />
                  </Form.Item>

                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    loading={actionLoading}
                    style={{
                      borderRadius: 8,
                      fontWeight: 600,
                      height: 42,
                      marginTop: 8,
                      boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)',
                    }}
                  >
                    Sign In
                  </Button>
                </Form>
              ) : (
                <Form form={registerForm} layout="vertical" onFinish={handleRegister} style={{ marginTop: 16 }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                    <Form.Item
                      name="firstName"
                      label="First Name"
                      rules={[{ required: true, message: 'First name required' }]}
                    >
                      <Input placeholder="John" style={{ borderRadius: 8 }} />
                    </Form.Item>

                    <Form.Item name="lastName" label="Last Name">
                      <Input placeholder="Doe" style={{ borderRadius: 8 }} />
                    </Form.Item>
                  </div>

                  <Form.Item
                    name="email"
                    label="Email Address"
                    rules={[
                      { required: true, message: 'Email is required' },
                      { type: 'email', message: 'Enter a valid email' },
                    ]}
                  >
                    <Input
                      prefix={<Mail size={16} style={{ color: '#94a3b8' }} />}
                      placeholder="john@company.com"
                      style={{ borderRadius: 8 }}
                    />
                  </Form.Item>

                  <Form.Item
                    name="password"
                    label="Password"
                    rules={[
                      { required: true, message: 'Password is required' },
                      { min: 6, message: 'Password must be at least 6 characters' },
                    ]}
                  >
                    <Input.Password
                      prefix={<Lock size={16} style={{ color: '#94a3b8' }} />}
                      placeholder="••••••••"
                      style={{ borderRadius: 8 }}
                    />
                  </Form.Item>

                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    loading={actionLoading}
                    style={{
                      borderRadius: 8,
                      fontWeight: 600,
                      height: 42,
                      marginTop: 8,
                      boxShadow: '0 4px 14px rgba(37, 99, 235, 0.3)',
                    }}
                  >
                    Register Account
                  </Button>
                </Form>
              )}
            </Card>
          )}
        </div>
      </div>
    </div>
  );
};
