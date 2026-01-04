import { LogoutOutlined, LockOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import type { MenuProps } from 'antd';
import { Spin } from 'antd';
import { createStyles } from 'antd-style';
import React from 'react';
import { flushSync } from 'react-dom';
import { logout } from '@/services/tutor';
import HeaderDropdown from '../HeaderDropdown';

export type GlobalHeaderRightProps = {
  menu?: boolean;
  children?: React.ReactNode;
};

export const AvatarName = () => {
  const { initialState } = useModel('@@initialState');
  const { currentUser } = initialState || {};
  return <span className="anticon">{currentUser?.name}</span>;
};

const useStyles = createStyles(({ token }) => {
  return {
    action: {
      display: 'flex',
      height: '48px',
      marginLeft: 'auto',
      overflow: 'hidden',
      alignItems: 'center',
      padding: '0 8px',
      cursor: 'pointer',
      borderRadius: token.borderRadius,
      '&:hover': {
        backgroundColor: token.colorBgTextHover,
      },
    },
  };
});

export const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({ children }) => {
  const { styles } = useStyles();
  const { initialState, setInitialState } = useModel('@@initialState');

  const loginOut = async () => {
    await logout();
    localStorage.removeItem('token');
    flushSync(() => {
      setInitialState((s) => ({ ...s, currentUser: undefined }));
    });
    history.replace('/user/login');
  };

  const onMenuClick: MenuProps['onClick'] = (event) => {
    if (event.key === 'logout') {
      loginOut();
    } else if (event.key === 'changePassword') {
      history.push('/change-password');
    }
  };

  const loading = (
    <span className={styles.action}>
      <Spin size="small" style={{ marginLeft: 8, marginRight: 8 }} />
    </span>
  );

  if (!initialState) {
    return loading;
  }

  const { currentUser } = initialState;

  if (!currentUser || !currentUser.name) {
    return loading;
  }

  const menuItems = [
    {
      key: 'changePassword',
      icon: <LockOutlined />,
      label: '修改密码',
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
    },
  ];

  return (
    <HeaderDropdown
      menu={{
        selectedKeys: [],
        onClick: onMenuClick,
        items: menuItems,
      }}
      trigger={['click']}
    >
      {children}
    </HeaderDropdown>
  );
};
