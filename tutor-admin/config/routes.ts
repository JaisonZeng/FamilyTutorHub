export default [
  {
    path: '/user',
    layout: false,
    routes: [
      {
        name: 'login',
        path: '/user/login',
        component: './user/login',
      },
    ],
  },
  {
    path: '/welcome',
    name: '首页',
    icon: 'home',
    component: './Welcome',
  },
  {
    path: '/students',
    name: '学生管理',
    icon: 'user',
    component: './Students',
  },
  {
    path: '/courses',
    name: '科目管理',
    icon: 'book',
    component: './Courses',
  },
  {
    path: '/schedules',
    name: '排课管理',
    icon: 'schedule',
    component: './Schedules',
  },
  {
    path: '/calendar',
    name: '排课日历',
    icon: 'calendar',
    component: './Calendar',
  },
  {
    path: '/exam-results',
    name: '成绩管理',
    icon: 'barChart',
    component: './ExamResults',
  },
  {
    path: '/',
    redirect: '/welcome',
  },
  {
    component: '404',
    layout: false,
    path: './*',
  },
];
