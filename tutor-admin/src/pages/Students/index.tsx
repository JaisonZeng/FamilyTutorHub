import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable, ModalForm, ProFormText } from '@ant-design/pro-components';
import { Button, message, Popconfirm } from 'antd';
import { useRef, useState } from 'react';
import { getStudents, createStudent, updateStudent, deleteStudent } from '@/services/tutor';

const Students: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [currentRow, setCurrentRow] = useState<API.Student>();

  const handleDelete = async (id: number) => {
    await deleteStudent(id);
    message.success('删除成功');
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.Student>[] = [
    {
      title: '姓名',
      dataIndex: 'name',
      formItemProps: { rules: [{ required: true, message: '请输入姓名' }] },
    },
    {
      title: '年级',
      dataIndex: 'grade',
    },
    {
      title: '家长电话',
      dataIndex: 'parent_phone',
      search: false,
    },
    {
      title: '备注',
      dataIndex: 'notes',
      search: false,
      ellipsis: true,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 150,
      render: (_, record) => [
        <a
          key="edit"
          onClick={() => {
            setCurrentRow(record);
            setModalOpen(true);
          }}
        >
          编辑
        </a>,
        <Popconfirm
          key="delete"
          title="确定删除？"
          onConfirm={() => handleDelete(record.id)}
        >
          <a style={{ color: '#ff4d4f' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.Student>
        headerTitle="学生列表"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          <Button
            type="primary"
            key="add"
            onClick={() => {
              setCurrentRow(undefined);
              setModalOpen(true);
            }}
          >
            <PlusOutlined /> 添加学生
          </Button>,
        ]}
        request={async () => {
          const data = await getStudents();
          return { data: data || [], success: true };
        }}
        columns={columns}
      />

      <ModalForm
        title={currentRow ? '编辑学生' : '添加学生'}
        open={modalOpen}
        onOpenChange={setModalOpen}
        initialValues={currentRow}
        modalProps={{ destroyOnClose: true }}
        onFinish={async (values) => {
          if (currentRow) {
            await updateStudent(currentRow.id, values);
            message.success('更新成功');
          } else {
            await createStudent(values);
            message.success('添加成功');
          }
          setModalOpen(false);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormText name="name" label="姓名" rules={[{ required: true }]} />
        <ProFormText name="grade" label="年级" placeholder="如：初二、高一" />
        <ProFormText name="parent_phone" label="家长电话" />
        <ProFormText name="notes" label="备注" />
      </ModalForm>
    </PageContainer>
  );
};

export default Students;
