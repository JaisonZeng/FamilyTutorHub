import { request } from '@umijs/max';

// 学生管理
export async function getStudents() {
  return request<API.Student[]>('/api/students');
}

export async function createStudent(data: Partial<API.Student>) {
  return request<API.Student>('/api/students', {
    method: 'POST',
    data,
  });
}

export async function updateStudent(id: number, data: Partial<API.Student>) {
  return request<API.Student>(`/api/students/${id}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteStudent(id: number) {
  return request(`/api/students/${id}`, {
    method: 'DELETE',
  });
}

// 科目管理
export async function getCourses() {
  return request<API.Course[]>('/api/courses');
}

export async function createCourse(data: Partial<API.Course>) {
  return request<API.Course>('/api/courses', {
    method: 'POST',
    data,
  });
}

export async function updateCourse(id: number, data: Partial<API.Course>) {
  return request<API.Course>(`/api/courses/${id}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteCourse(id: number) {
  return request(`/api/courses/${id}`, {
    method: 'DELETE',
  });
}

// 排课管理
export async function getSchedules() {
  return request<API.Schedule[]>('/api/schedules');
}

export async function searchSchedules(params: API.ScheduleSearchParams) {
  return request<API.Schedule[]>('/api/schedules/search', {
    params,
  });
}

export async function createSchedule(data: Partial<API.Schedule>) {
  return request<API.Schedule>('/api/schedules', {
    method: 'POST',
    data,
  });
}

export async function updateSchedule(id: number, data: Partial<API.Schedule>) {
  return request<API.Schedule>(`/api/schedules/${id}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteSchedule(id: number) {
  return request(`/api/schedules/${id}`, {
    method: 'DELETE',
  });
}

// 成绩管理
export async function getExamResults(params?: API.ExamResultSearchParams) {
  return request<API.ExamResult[]>('/api/exam-results', { params });
}

export async function createExamResult(data: Partial<API.ExamResult>) {
  return request<API.ExamResult>('/api/exam-results', {
    method: 'POST',
    data,
  });
}

export async function updateExamResult(id: number, data: Partial<API.ExamResult>) {
  return request<API.ExamResult>(`/api/exam-results/${id}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteExamResult(id: number) {
  return request(`/api/exam-results/${id}`, {
    method: 'DELETE',
  });
}

export async function getExamResultsByStudent(studentId: number, courseId?: number) {
  return request<API.ExamResult[]>(`/api/exam-results/student/${studentId}`, {
    params: courseId ? { course_id: courseId } : undefined,
  });
}
