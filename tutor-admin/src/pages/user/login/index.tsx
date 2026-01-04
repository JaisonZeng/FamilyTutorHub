import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { Helmet, history, useModel } from '@umijs/max';
import { Alert, App } from 'antd';
import { createStyles } from 'antd-style';
import React, { useState } from 'react';
import { flushSync } from 'react-dom';
import { login } from '@/services/tutor';
import Settings from '../../../../config/defaultSettings';

const useStyles = createStyles(() => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    overflow: 'auto',
    background: '#f0f2f5',
  },
}));

const Login: React.FC = () => {
  const [error, setError] = useState<string>('');
  const { initialState, setInitialState } = useModel('@@initialState');
  const { styles } = useStyles();
  const { message } = App.useApp();

  const handleSubmit = async (values: { username: string; password: string }) => {
    try {
      setError('');
      const res = await login(values);

      // 保存 token
      localStorage.setItem('token', res.token);

      message.success('登录成功！');

      // 更新用户信息
      flushSync(() => {
        setInitialState((s) => ({
          ...s,
          currentUser: res.currentUser,
        }));
      });

      // 跳转
      const urlParams = new URL(window.location.href).searchParams;
      history.push(urlParams.get('redirect') || '/');
    } catch (err: any) {
      setError(err.response?.data?.error || '登录失败，请重试');
    }
  };

  return (
    <div className={styles.container}>
      <Helmet>
        <title>登录 - {Settings.title}</title>
      </Helmet>
      <div style={{ flex: '1', padding: '120px 0 24px' }}>
        <LoginForm
          contentStyle={{ minWidth: 280, maxWidth: '75vw' }}
          title={Settings.title}
          subTitle="家教排课管理系统"
          onFinish={handleSubmit}
        >
          {error && (
            <Alert
              style={{ marginBottom: 24 }}
              message={error}
              type="error"
              showIcon
            />
          )}

          <ProFormText
            name="username"
            fieldProps={{
              size: 'large',
              prefix: <UserOutlined />,
            }}
            placeholder="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          />
          <ProFormText.Password
            name="password"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined />,
            }}
            placeholder="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          />
        </LoginForm>
      </div>
    </div>
  );
};

export default Login;
