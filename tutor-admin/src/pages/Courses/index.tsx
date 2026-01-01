import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable, ModalForm, ProFormText, ProFormTextArea } from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag } from 'antd';
import { useRef, useState } from 'react';
import { getCourses, createCourse, updateCourse, deleteCourse } from '@/services/tutor';

const COURSE_COLORS: Record<string, string> = {
  '数学': 'blue',
  '英语': 'green',
  '语文': 'red',
  '物理': 'purple',
  '化学': 'orange',
  '生物': 'cyan',
};

const Courses: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [currentRow, setCurrentRow] = useState<API.Course>();

  const handleDelete = async (id: number) => {
    await deleteCourse(id);
    message.success('删除成功');
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.Course>[] = [
    {
      title: '科目名称',
      dataIndex: 'name',
      render: (_, record) => (
        <Tag color={COURSE_COLORS[record.name] || 'default'}>{record.name}</Tag>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
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
      <ProTable<API.Course>
        headerTitle="科目列表"
        actionRef={actionRef}
        rowKey="id"
        search={false}
        toolBarRender={() => [
          <Button
            type="primary"
            key="add"
            onClick={() => {
              setCurrentRow(undefined);
              setModalOpen(true);
            }}
          >
            <PlusOutlined /> 添加科目
          </Button>,
        ]}
        request={async () => {
          const data = await getCourses();
          return { data: data || [], success: true };
        }}
        columns={columns}
      />

      <ModalForm
        title={currentRow ? '编辑科目' : '添加科目'}
        open={modalOpen}
        onOpenChange={setModalOpen}
        initialValues={currentRow}
        modalProps={{ destroyOnClose: true }}
        onFinish={async (values) => {
          if (currentRow) {
            await updateCourse(currentRow.id, values);
            message.success('更新成功');
          } else {
            await createCourse(values);
            message.success('添加成功');
          }
          setModalOpen(false);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormText
          name="name"
          label="科目名称"
          placeholder="如：数学、英语、物理"
          rules={[{ required: true }]}
        />
        <ProFormTextArea name="description" label="描述" placeholder="科目描述（可选）" />
      </ModalForm>
    </PageContainer>
  );
};

export default Courses;
