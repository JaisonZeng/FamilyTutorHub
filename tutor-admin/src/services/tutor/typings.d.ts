declare namespace API {
  interface CurrentUser {
    id: number;
    username: string;
    name: string;
    avatar?: string;
  }

  interface Student {
    id: number;
    name: string;
    parent_phone: string;
    grade: string;
    notes: string;
    created_at?: string;
    updated_at?: string;
  }

  interface Course {
    id: number;
    name: string;
    description: string;
    created_at?: string;
    updated_at?: string;
  }

  interface Schedule {
    id: number;
    student_id: number;
    student?: Student;
    course_id: number;
    course?: Course;
    start_time: string;
    end_time: string;
    status: 'scheduled' | 'completed' | 'cancelled';
    created_at?: string;
    updated_at?: string;
  }

  interface ScheduleSearchParams {
    start_date?: string;
    end_date?: string;
    student_id?: number;
    course_id?: number;
    status?: string;
  }

  interface ExamResult {
    id: number;
    student_id: number;
    student?: Student;
    course_id: number;
    course?: Course;
    exam_type: 'midterm' | 'final' | 'quiz';
    exam_name: string;
    score: number;
    full_score: number;
    exam_date: string;
    comment?: string;
    created_at?: string;
    updated_at?: string;
  }

  interface ExamResultSearchParams {
    student_id?: number;
    course_id?: number;
    exam_type?: string;
  }
}
