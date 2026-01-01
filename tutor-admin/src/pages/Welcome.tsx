import { PageContainer } from '@ant-design/pro-components';
import { Card, Col, Row, Statistic, List, Tag, Empty } from 'antd';
import { UserOutlined, BookOutlined, CalendarOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import { getStudents, getCourses, searchSchedules } from '@/services/tutor';

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  scheduled: { text: '待上课', color: 'processing' },
  completed: { text: '已完成', color: 'success' },
  cancelled: { text: '已取消', color: 'error' },
};

// 根据时间计算课程状态和背景色（浅色版本）
const getScheduleStyle = (schedule: API.Schedule) => {
  if (schedule.status === 'cancelled') {
    return { text: '已取消', color: 'error', bgColor: 'rgba(255, 77, 79, 0.1)' };
  }
  
  const now = dayjs();
  const start = dayjs(schedule.start_time);
  const end = dayjs(schedule.end_time);
  
  // 已结束 - 浅灰色背景
  if (now.isAfter(end)) {
    return { text: '已结束', color: 'default', bgColor: 'rgba(140, 140, 140, 0.1)' };
  }
  // 进行中 - 浅黄色背景
  if (now.isAfter(start) && now.isBefore(end)) {
    return { text: '进行中', color: 'warning', bgColor: 'rgba(250, 173, 20, 0.15)' };
  }
  // 未开始 - 浅蓝色背景
  return { text: '待上课', color: 'processing', bgColor: 'rgba(24, 144, 255, 0.1)' };
};

const Welcome: React.FC = () => {
  const [studentCount, setStudentCount] = useState(0);
  const [courseCount, setCourseCount] = useState(0);
  const [todaySchedules, setTodaySchedules] = useState<API.Schedule[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [students, courses, schedules] = await Promise.all([
          getStudents(),
          getCourses(),
          searchSchedules({
            start_date: dayjs().format('YYYY-MM-DD'),
            end_date: dayjs().format('YYYY-MM-DD'),
          }),
        ]);
        setStudentCount(students?.length || 0);
        setCourseCount(courses?.length || 0);
        setTodaySchedules(schedules || []);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  return (
    <PageContainer>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="学生总数"
              value={studentCount}
              prefix={<UserOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="科目总数"
              value={courseCount}
              prefix={<BookOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="今日课程"
              value={todaySchedules.length}
              prefix={<CalendarOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
      </Row>

      <Card
        title={
          <span>
            <ClockCircleOutlined style={{ marginRight: 8 }} />
            今日课程安排 ({dayjs().format('M月D日 dddd')})
          </span>
        }
        style={{ marginTop: 16 }}
        loading={loading}
      >
        {todaySchedules.length === 0 ? (
          <Empty description="今天没有课程安排" />
        ) : (
          <List
            dataSource={todaySchedules.sort((a, b) => 
              dayjs(a.start_time).valueOf() - dayjs(b.start_time).valueOf()
            )}
            renderItem={(item) => {
              const style = getScheduleStyle(item);
              return (
                <List.Item style={{ backgroundColor: style.bgColor, padding: '12px 16px', marginBottom: 4, borderRadius: 6 }}>
                  <List.Item.Meta
                    title={
                      <span>
                        <Tag style={{ marginRight: '2px' }} color="blue">{dayjs(item.start_time).format('HH:mm')}</Tag>
                        {item.student?.name} - {item.course?.name}
                      </span>
                    }
                    description={`${dayjs(item.start_time).format('HH:mm')} ~ ${dayjs(item.end_time).format('HH:mm')}`}
                  />
                  <Tag color={style.color}>{style.text}</Tag>
                </List.Item>
              );
            }}
          />
        )}
      </Card>
    </PageContainer>
  );
};

export default Welcome;
