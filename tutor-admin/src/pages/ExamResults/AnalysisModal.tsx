import { Modal, Select, Button, Space, message, Spin } from 'antd';
import { useState } from 'react';

interface AnalysisModalProps {
  open: boolean;
  onClose: () => void;
  students: API.Student[];
  courses: API.Course[];
}

const AnalysisModal: React.FC<AnalysisModalProps> = ({ open, onClose, students, courses }) => {
  const [selectedStudent, setSelectedStudent] = useState<number>();
  const [selectedCourse, setSelectedCourse] = useState<number>();
  const [chartUrl, setChartUrl] = useState<string>('');
  const [studentName, setStudentName] = useState('');
  const [courseName, setCourseName] = useState('');
  const [loading, setLoading] = useState(false);

  const handleClose = () => {
    setSelectedStudent(undefined);
    setSelectedCourse(undefined);
    setChartUrl('');
    setStudentName('');
    setCourseName('');
    onClose();
  };

  const handleAnalyze = async () => {
    if (!selectedStudent) {
      message.warning('请选择学生');
      return;
    }

    const student = students.find((s) => s.id === selectedStudent);
    const course = selectedCourse ? courses.find((c) => c.id === selectedCourse) : null;
    setStudentName(student?.name || '');
    setCourseName(course?.name || '全部科目');

    // 构建后端图表 URL
    let url = `/api/analysis/${selectedStudent}/trend.png`;
    if (selectedCourse) {
      // 根据课程名称映射到 subject 参数
      const courseNameLower = course?.name?.toLowerCase() || '';
      if (courseNameLower.includes('数学') || courseNameLower.includes('math')) {
        url += '?subject=math';
      } else if (courseNameLower.includes('英语') || courseNameLower.includes('english')) {
        url += '?subject=english';
      }
    }

    // 添加时间戳防止缓存
    url += (url.includes('?') ? '&' : '?') + `t=${Date.now()}`;
    
    setLoading(true);
    setChartUrl(url);
  };

  const handleSave = () => {
    if (!chartUrl) return;
    
    // 创建下载链接
    const link = document.createElement('a');
    link.download = `${studentName}_成绩曲线_${new Date().toLocaleDateString()}.png`;
    link.href = chartUrl;
    link.click();
    message.success('保存成功');
  };

  const handleImageLoad = () => {
    setLoading(false);
  };

  const handleImageError = () => {
    setLoading(false);
    message.error('加载图表失败');
    setChartUrl('');
  };

  return (
    <Modal
      title="成绩分析"
      open={open}
      onCancel={handleClose}
      width={900}
      footer={null}
    >
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          placeholder="选择学生"
          style={{ width: 150 }}
          value={selectedStudent}
          options={students.map((s) => ({ label: s.name, value: s.id }))}
          onChange={setSelectedStudent}
          allowClear
        />
        <Select
          placeholder="选择科目(可选)"
          style={{ width: 150 }}
          value={selectedCourse}
          options={courses.map((c) => ({ label: c.name, value: c.id }))}
          onChange={setSelectedCourse}
          allowClear
        />
        <Button type="primary" onClick={handleAnalyze}>
          分析
        </Button>
        {chartUrl && !loading && <Button onClick={handleSave}>保存图片</Button>}
      </Space>

      {(chartUrl || loading) && (
        <div style={{ padding: 20, background: '#fff', textAlign: 'center' }}>
          <h3 style={{ marginBottom: 16 }}>
            {studentName} - {courseName} 成绩曲线
          </h3>
          <Spin spinning={loading}>
            {chartUrl && (
              <img
                src={chartUrl}
                alt="成绩趋势图"
                style={{ maxWidth: '100%', height: 'auto' }}
                onLoad={handleImageLoad}
                onError={handleImageError}
              />
            )}
          </Spin>
        </div>
      )}
    </Modal>
  );
};

export default AnalysisModal;
