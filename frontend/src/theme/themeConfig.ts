import type { ThemeConfig } from 'antd';

export const midoneAntdTheme: ThemeConfig = {
  token: {
    colorPrimary: '#2563eb', // Midone Royal Blue
    colorInfo: '#0284c7',
    colorSuccess: '#10b981',
    colorWarning: '#f59e0b',
    colorError: '#ef4444',
    colorTextBase: '#0f172a',
    colorBgBase: '#ffffff',
    colorBgLayout: '#f8fafc',
    colorBorder: '#e2e8f0',
    colorBorderSecondary: '#f1f5f9',
    borderRadius: 10,
    borderRadiusLG: 14,
    borderRadiusSM: 6,
    fontFamily: "'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    fontSize: 14,
    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.05), 0 2px 4px -2px rgb(0 0 0 / 0.05)',
    boxShadowSecondary: '0 10px 15px -3px rgb(0 0 0 / 0.06), 0 4px 6px -4px rgb(0 0 0 / 0.06)',
  },
  components: {
    Button: {
      borderRadius: 8,
      fontWeight: 500,
      controlHeight: 38,
      primaryShadow: '0 2px 4px 0 rgba(37, 99, 235, 0.2)',
    },
    Card: {
      borderRadiusLG: 14,
      colorBorderSecondary: '#e2e8f0',
      headerHeight: 52,
    },
    Table: {
      borderRadius: 12,
      headerBg: '#f8fafc',
      headerColor: '#475569',
      rowHoverBg: '#f1f5f9',
      borderColor: '#f1f5f9',
    },
    Input: {
      controlHeight: 38,
      borderRadius: 8,
      colorBorder: '#cbd5e1',
    },
    Select: {
      controlHeight: 38,
      borderRadius: 8,
    },
    Modal: {
      borderRadiusLG: 16,
      headerBg: '#ffffff',
    },
    Tag: {
      borderRadiusSM: 6,
      fontSize: 12,
    },
    Badge: {
      fontSize: 11,
    },
    Segmented: {
      borderRadius: 8,
      trackBg: '#f1f5f9',
      itemSelectedBg: '#ffffff',
      itemHoverBg: 'rgba(0,0,0,0.03)',
    },
  },
};
