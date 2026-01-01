import { PageContainer } from '@ant-design/pro-components';
import { Modal, Form, Select, DatePicker, message, Tag } from 'antd';
import { useState, useEffect, useRef } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';
import dayjs from 'dayjs';
import {
  getSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  getStudents,
  getCourses,
} from '@/services/tutor';
import './index.less';

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  scheduled: { text: '已排课', color: '#1890ff' },
  completed: { text: '已完成', color: '#52c41a' },
  cancelled: { text: '已取消', color: '#ff4d4f' },
};

// 根据时间计算课程实际状态颜色
const getScheduleColor = (schedule: API.Schedule) => {
  // 已取消的课程保持红色
  if (schedule.status === 'cancelled') return '#ff4d4f';
  
  const now = dayjs();
  const start = dayjs(schedule.start_time);
  const end = dayjs(schedule.end_time);
  
  // 已结束 - 灰色
  if (now.isAfter(end)) return '#8c8c8c';
  // 进行中 - 黄色
  if (now.isAfter(start) && now.isBefore(end)) return '#faad14';
  // 未开始 - 蓝色
  return '#1890ff';
};

const Calendar: React.FC = () => {
  const [schedules, setSchedules] = useState<API.Schedule[]>([]);
  const [students, setStudents] = useState<API.Student[]>([]);
  const [courses, setCourses] = useState<API.Course[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedRange, setSelectedRange] = useState<{ start: Date; end: Date } | null>(null);
  const [selectedSchedule, setSelectedSchedule] = useState<API.Schedule | null>(null);
  const [form] = Form.useForm();
  const calendarRef = useRef<FullCalendar>(null);

  const loadData = async () => {
    const [s, st, c] = await Promise.all([getSchedules(), getStudents(), getCourses()]);
    setSchedules(s || []);
    setStudents(st || []);
    setCourses(c || []);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSelect = (arg: DateSelectArg) => {
    setSelectedRange({ start: arg.start, end: arg.end });
    form.resetFields();
    setModalOpen(true);
  };

  const handleEventClick = (arg: EventClickArg) => {
    const schedule = schedules.find((s) => String(s.id) === arg.event.id);
    if (schedule) {
      setSelectedSchedule(schedule);
      setDetailModalOpen(true);
    }
  };

  const handleCreate = async () => {
    const values = await form.validateFields();
    if (!selectedRange) return;

    await createSchedule({
      student_id: values.student_id,
      course_id: values.course_id,
      start_time: selectedRange.start.toISOString(),
      end_time: selectedRange.end.toISOString(),
      status: 'scheduled',
    });
    message.success('排课成功');
    setModalOpen(false);
    loadData();
  };

  const handleUpdateStatus = async (status: string) => {
    if (!selectedSchedule) return;
    await updateSchedule(selectedSchedule.id, { status });
    message.success('状态更新成功');
    setDetailModalOpen(false);
    loadData();
  };

  const handleDelete = async () => {
    if (!selectedSchedule) return;
    await deleteSchedule(selectedSchedule.id);
    message.success('删除成功');
    setDetailModalOpen(false);
    loadData();
  };

  const events = schedules.map((s) => {
    const color = getScheduleColor(s);
    return {
      id: String(s.id),
      title: `${s.student?.name || '学生'} - ${s.course?.name || '课程'}`,
      start: s.start_time,
      end: s.end_time,
      backgroundColor: color,
      borderColor: color,
    };
  });

  const formatTimeRange = (start: string, end: string) => {
    return `${dayjs(start).format('MM-DD HH:mm')} ~ ${dayjs(end).format('HH:mm')}`;
  };

  return (
    <PageContainer>
      <div className="calendar-container">
        <FullCalendar
          ref={calendarRef}
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="timeGridWeek"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay',
          }}
          locale="zh-cn"
          buttonText={{
            today: '今天',
            month: '月',
            week: '周',
            day: '日',
          }}
          events={events}
          selectable
          select={handleSelect}
          eventClick={handleEventClick}
          height="auto"
          slotMinTime="06:00:00"
          slotMaxTime="23:00:00"
          slotDuration="00:30:00"
          allDaySlot={false}
          eventTimeFormat={{
            hour: '2-digit',
            minute: '2-digit',
            hour12: false,
          }}
          slotLabelFormat={{
            hour: '2-digit',
            minute: '2-digit',
            hour12: false,
          }}
        />
      </div>

      {/* 新建排课弹窗 */}
      <Modal
        title="添加排课"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
        okText="保存"
        cancelText="取消"
      >
        <div style={{ marginBottom: 16, padding: 12, background: '#f0f5ff', borderRadius: 6 }}>
          <span style={{ color: '#1890ff' }}>
            时间段：{selectedRange && `${dayjs(selectedRange.start).format('MM-DD HH:mm')} ~ ${dayjs(selectedRange.end).format('HH:mm')}`}
          </span>
        </div>
        <Form form={form} layout="vertical">
          <Form.Item name="student_id" label="学生" rules={[{ required: true, message: '请选择学生' }]}>
            <Select placeholder="请选择学生">
              {students.map((s) => (
                <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="course_id" label="科目" rules={[{ required: true, message: '请选择科目' }]}>
            <Select placeholder="请选择科目">
              {courses.map((c) => (
                <Select.Option key={c.id} value={c.id}>{c.name}</Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* 课程详情弹窗 */}
      <Modal
        title="课程详情"
        open={detailModalOpen}
        onCancel={() => setDetailModalOpen(false)}
        footer={
          selectedSchedule?.status === 'scheduled' ? [
            <a key="complete" style={{ marginRight: 16 }} onClick={() => handleUpdateStatus('completed')}>
              标记完成
            </a>,
            <a key="cancel" style={{ marginRight: 16, color: '#faad14' }} onClick={() => handleUpdateStatus('cancelled')}>
              取消课程
            </a>,
            <a key="delete" style={{ color: '#ff4d4f' }} onClick={handleDelete}>
              删除
            </a>,
          ] : [
            <a key="delete" style={{ color: '#ff4d4f' }} onClick={handleDelete}>
              删除
            </a>,
          ]
        }
      >
        {selectedSchedule && (
          <div>
            <p><strong>学生：</strong>{selectedSchedule.student?.name}</p>
            <p><strong>科目：</strong>{selectedSchedule.course?.name}</p>
            <p><strong>时间：</strong>{formatTimeRange(selectedSchedule.start_time, selectedSchedule.end_time)}</p>
            <p>
              <strong>状态：</strong>
              <Tag color={STATUS_MAP[selectedSchedule.status]?.color}>
                {STATUS_MAP[selectedSchedule.status]?.text}
              </Tag>
            </p>
          </div>
        )}
      </Modal>
    </PageContainer>
  );
};

export default Calendar;
