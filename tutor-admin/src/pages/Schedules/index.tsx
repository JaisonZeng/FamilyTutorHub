import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import {
  PageContainer,
  ProTable,
  ModalForm,
  ProFormSelect,
  ProFormDateTimePicker,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag } from 'antd';
import { useRef, useState, useEffect } from 'react';
import dayjs from 'dayjs';
import {
  searchSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  getStudents,
  getCourses,
} from '@/services/tutor';

const STATUS_MAP = {
  scheduled: { text: '已排课', color: 'processing' },
  completed: { text: '已完成', color: 'success' },
  cancelled: { text: '已取消', color: 'error' },
};

const Schedules: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [currentRow, setCurrentRow] = useState<API.Schedule>();
  const [students, setStudents] = useState<API.Student[]>([]);
  const [courses, setCourses] = useState<API.Course[]>([]);

  useEffect(() => {
    getStudents().then(setStudents);
    getCourses().then(setCourses);
  }, []);

  const handleDelete = async (id: number) => {
    await deleteSchedule(id);
    message.success('删除成功');
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.Schedule>[] = [
    {
      title: '学生',
      dataIndex: 'student_id',
      render: (_, record) => record.student?.name || '-',
      valueType: 'select',
      fieldProps: {
        options: students.map((s) => ({ label: s.name, value: s.id })),
      },
    },
    {
      title: '科目',
      dataIndex: 'course_id',
      render: (_, record) => record.course?.name || '-',
      valueType: 'select',
      fieldProps: {
        options: courses.map((c) => ({ label: c.name, value: c.id })),
      },
    },
    {
      title: '开始时间',
      dataIndex: 'start_time',
      valueType: 'dateTime',
      search: false,
      render: (_, record) => dayjs(record.start_time).format('MM-DD HH:mm'),
    },
    {
      title: '结束时间',
      dataIndex: 'end_time',
      valueType: 'dateTime',
      search: false,
      render: (_, record) => dayjs(record.end_time).format('MM-DD HH:mm'),
    },
    {
      title: '日期范围',
      dataIndex: 'dateRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          start_date: value?.[0],
          end_date: value?.[1],
        }),
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: {
        scheduled: { text: '已排课', status: 'Processing' },
        completed: { text: '已完成', status: 'Success' },
        cancelled: { text: '已取消', status: 'Error' },
      },
      render: (_, record) => {
        const status = STATUS_MAP[record.status] || STATUS_MAP.scheduled;
        return <Tag color={status.color}>{status.text}</Tag>;
      },
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
      <ProTable<API.Schedule>
        headerTitle="排课列表"
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
            <PlusOutlined /> 新建排课
          </Button>,
        ]}
        request={async (params) => {
          const { student_id, course_id, status, start_date, end_date } = params;
          const data = await searchSchedules({
            student_id,
            course_id,
            status,
            start_date,
            end_date,
          });
          return { data: data || [], success: true };
        }}
        columns={columns}
      />

      <ModalForm
        title={currentRow ? '编辑排课' : '新建排课'}
        open={modalOpen}
        onOpenChange={setModalOpen}
        initialValues={
          currentRow
            ? {
                ...currentRow,
                start_time: dayjs(currentRow.start_time),
                end_time: dayjs(currentRow.end_time),
              }
            : { status: 'scheduled' }
        }
        modalProps={{ destroyOnClose: true }}
        onFinish={async (values) => {
          const data = {
            student_id: values.student_id,
            course_id: values.course_id,
            start_time: dayjs(values.start_time).toISOString(),
            end_time: dayjs(values.end_time).toISOString(),
            status: values.status,
          };
          if (currentRow) {
            await updateSchedule(currentRow.id, data);
            message.success('更新成功');
          } else {
            await createSchedule(data);
            message.success('添加成功');
          }
          setModalOpen(false);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormSelect
          name="student_id"
          label="学生"
          rules={[{ required: true, message: '请选择学生' }]}
          options={students.map((s) => ({ label: s.name, value: s.id }))}
        />
        <ProFormSelect
          name="course_id"
          label="科目"
          rules={[{ required: true, message: '请选择科目' }]}
          options={courses.map((c) => ({ label: c.name, value: c.id }))}
        />
        <ProFormDateTimePicker
          name="start_time"
          label="开始时间"
          rules={[{ required: true, message: '请选择开始时间' }]}
          fieldProps={{ format: 'YYYY-MM-DD HH:mm' }}
        />
        <ProFormDateTimePicker
          name="end_time"
          label="结束时间"
          rules={[{ required: true, message: '请选择结束时间' }]}
          fieldProps={{ format: 'YYYY-MM-DD HH:mm' }}
        />
        <ProFormSelect
          name="status"
          label="状态"
          options={[
            { label: '已排课', value: 'scheduled' },
            { label: '已完成', value: 'completed' },
            { label: '已取消', value: 'cancelled' },
          ]}
        />
      </ModalForm>
    </PageContainer>
  );
};

export default Schedules;
