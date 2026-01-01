import { PlusOutlined, LineChartOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import {
  PageContainer,
  ProTable,
  ModalForm,
  ProFormText,
  ProFormSelect,
  ProFormDigit,
  ProFormDatePicker,
  ProFormTextArea,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag } from 'antd';
import { useRef, useState, useEffect } from 'react';
import {
  getExamResults,
  createExamResult,
  updateExamResult,
  deleteExamResult,
  getStudents,
  getCourses,
} from '@/services/tutor';
import AnalysisModal from './AnalysisModal';

const examTypeMap = {
  midterm: { text: '期中考', color: 'blue' },
  final: { text: '期末考', color: 'purple' },
  quiz: { text: '小测', color: 'green' },
};

const ExamResults: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [analysisOpen, setAnalysisOpen] = useState(false);
  const [currentRow, setCurrentRow] = useState<API.ExamResult>();
  const [students, setStudents] = useState<API.Student[]>([]);
  const [courses, setCourses] = useState<API.Course[]>([]);

  useEffect(() => {
    getStudents().then(setStudents);
    getCourses().then(setCourses);
  }, []);

  const handleDelete = async (id: number) => {
    await deleteExamResult(id);
    message.success('删除成功');
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.ExamResult>[] = [
    {
      title: '学生',
      dataIndex: 'student_id',
      render: (_, record) => record.student?.name,
      valueType: 'select',
      fieldProps: {
        options: students.map((s) => ({ label: s.name, value: s.id })),
      },
    },
    {
      title: '科目',
      dataIndex: 'course_id',
      render: (_, record) => record.course?.name,
      valueType: 'select',
      fieldProps: {
        options: courses.map((c) => ({ label: c.name, value: c.id })),
      },
    },
    {
      title: '考试类型',
      dataIndex: 'exam_type',
      render: (_, record) => {
        const type = examTypeMap[record.exam_type];
        return <Tag color={type?.color}>{type?.text}</Tag>;
      },
      valueType: 'select',
      valueEnum: {
        midterm: { text: '期中考' },
        final: { text: '期末考' },
        quiz: { text: '小测' },
      },
    },
    {
      title: '考试名称',
      dataIndex: 'exam_name',
      search: false,
    },
    {
      title: '成绩',
      dataIndex: 'score',
      search: false,
      render: (_, record) => `${record.score} / ${record.full_score}`,
    },
    {
      title: '考试日期',
      dataIndex: 'exam_date',
      valueType: 'date',
      search: false,
    },
    {
      title: '备注',
      dataIndex: 'comment',
      search: false,
      ellipsis: true,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
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
        <Popconfirm key="delete" title="确定删除？" onConfirm={() => handleDelete(record.id)}>
          <a style={{ color: '#ff4d4f' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.ExamResult>
        headerTitle="成绩列表"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          <Button key="analysis" icon={<LineChartOutlined />} onClick={() => setAnalysisOpen(true)}>
            成绩分析
          </Button>,
          <Button
            type="primary"
            key="add"
            onClick={() => {
              setCurrentRow(undefined);
              setModalOpen(true);
            }}
          >
            <PlusOutlined /> 录入成绩
          </Button>,
        ]}
        request={async (params) => {
          const data = await getExamResults({
            student_id: params.student_id,
            course_id: params.course_id,
            exam_type: params.exam_type,
          });
          return { data: data || [], success: true };
        }}
        columns={columns}
      />

      <ModalForm
        title={currentRow ? '编辑成绩' : '录入成绩'}
        open={modalOpen}
        onOpenChange={setModalOpen}
        initialValues={
          currentRow
            ? {
                ...currentRow,
                exam_date: currentRow.exam_date?.split('T')[0],
              }
            : { full_score: 100 }
        }
        modalProps={{ destroyOnClose: true }}
        grid={true}
        rowProps={{ gutter: 16 }}
        onFinish={async (values) => {
          const data = {
            ...values,
            exam_date: values.exam_date + 'T00:00:00Z',
          };
          if (currentRow) {
            await updateExamResult(currentRow.id, data);
            message.success('更新成功');
          } else {
            await createExamResult(data);
            message.success('录入成功');
          }
          setModalOpen(false);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormSelect
          name="student_id"
          label="学生"
          colProps={{ span: 12 }}
          rules={[{ required: true, message: '请选择学生' }]}
          options={students.map((s) => ({ label: s.name, value: s.id }))}
        />
        <ProFormSelect
          name="course_id"
          label="科目"
          colProps={{ span: 12 }}
          rules={[{ required: true, message: '请选择科目' }]}
          options={courses.map((c) => ({ label: c.name, value: c.id }))}
        />
        <ProFormSelect
          name="exam_type"
          label="考试类型"
          colProps={{ span: 12 }}
          rules={[{ required: true, message: '请选择考试类型' }]}
          options={[
            { label: '期中考', value: 'midterm' },
            { label: '期末考', value: 'final' },
            { label: '小测', value: 'quiz' },
          ]}
        />
        <ProFormText
          name="exam_name"
          label="考试名称"
          colProps={{ span: 12 }}
          placeholder="如：第一次月考、期中考试"
          rules={[{ required: true, message: '请输入考试名称' }]}
        />
        <ProFormDigit name="score" label="成绩" colProps={{ span: 12 }} rules={[{ required: true }]} min={0} />
        <ProFormDigit name="full_score" label="满分" colProps={{ span: 12 }} rules={[{ required: true }]} min={0} />
        <ProFormDatePicker name="exam_date" label="考试日期" colProps={{ span: 12 }} rules={[{ required: true }]} />
        <ProFormTextArea name="comment" label="备注" colProps={{ span: 12 }} />
      </ModalForm>

      <AnalysisModal
        open={analysisOpen}
        onClose={() => setAnalysisOpen(false)}
        students={students}
        courses={courses}
      />
    </PageContainer>
  );
};

export default ExamResults;
