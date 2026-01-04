import { PageContainer } from '@ant-design/pro-components';
import { Card, Form, Input, Button, message, Space } from 'antd';
import { LockOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { history } from '@umijs/max';
import { changePassword } from '@/services/ant-design-pro/api';

const ChangePassword: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      await changePassword({
        old_password: values.old_password,
        new_password: values.new_password,
      });

      setSuccess(true);
      message.success('密码修改成功！请重新登录');

      // 3秒后退出登录
      setTimeout(() => {
        // 清除本地存储
        localStorage.removeItem('token');
        localStorage.removeItem('token-expire');
        localStorage.removeItem('user-info');

        // 跳转到登录页
        history.push('/user/login');
      }, 3000);
    } catch (error: any) {
      message.error(error?.message || '密码修改失败，请检查原密码是否正确');
      setSuccess(false);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <PageContainer>
        <Card style={{ maxWidth: 400, margin: '100px auto', textAlign: 'center' }}>
          <CheckCircleOutlined style={{ fontSize: 72, color: '#52c41a' }} />
          <h2 style={{ marginTop: 24, marginBottom: 16 }}>密码修改成功！</h2>
          <p>正在跳转到登录页面...</p>
        </Card>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      header={{
        title: '修改密码',
      }}
    >
      <Card style={{ maxWidth: 600, margin: '0 auto' }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          autoComplete="off"
        >
          <Form.Item
            label="原密码"
            name="old_password"
            rules={[
              { required: true, message: '请输入原密码' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入当前密码"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="新密码"
            name="new_password"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少6位' },
              { max: 32, message: '密码最多32位' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入新密码（至少6位）"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="确认新密码"
            name="confirm_password"
            dependencies={['new_password']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('new_password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请再次输入新密码"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                size="large"
                style={{ minWidth: 120 }}
              >
                修改密码
              </Button>
              <Button
                onClick={() => history.back()}
                size="large"
              >
                取消
              </Button>
            </Space>
          </Form.Item>

          <div style={{
            marginTop: 24,
            padding: 16,
            background: '#f0f2f5',
            borderRadius: 8,
            fontSize: 14,
            color: '#666'
          }}>
            <p style={{ margin: 0, marginBottom: 8 }}>
              <strong>密码安全提示：</strong>
            </p>
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>密码长度至少 6 位，建议使用 12 位以上</li>
              <li>包含大小写字母、数字和特殊字符更安全</li>
              <li>不要使用与用户名相同或过于简单的密码</li>
              <li>建议定期更换密码</li>
            </ul>
          </div>
        </Form>
      </Card>
    </PageContainer>
  );
};

export default ChangePassword;
