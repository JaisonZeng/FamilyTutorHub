import type { ProLayoutProps } from '@ant-design/pro-components';

const Settings: ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
} = {
  navTheme: 'light',
  colorPrimary: '#2563eb',
  layout: 'side',
  contentWidth: 'Fluid',
  fixedHeader: true,
  fixSiderbar: true,
  colorWeak: false,
  title: '家教管理系统',
  pwa: false,
  logo: '/logo.png',
  iconfontUrl: '',
  token: {},
  siderWidth: 180,
};

export default Settings;
