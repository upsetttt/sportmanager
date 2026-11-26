import axios from 'axios';
<<<<<<< HEAD

const api = axios.create({ baseURL: '/api', timeout: 15000 });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('snm_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  r => r,
  e => {
    const url = String(e.config?.url || '');
    if (e.response?.status === 401 && !url.includes('/auth/login') && !url.includes('/auth/register')) {
      localStorage.removeItem('snm_token');
      localStorage.removeItem('snm_user');
      window.dispatchEvent(new Event('snm:unauthorized'));
    }
    return Promise.reject(e);
  }
);

=======
const api = axios.create({ baseURL:'/api', timeout:15000 });
api.interceptors.request.use(config=>{ const token=localStorage.getItem('snm_token'); if(token) config.headers.Authorization=`Bearer ${token}`; return config; });
api.interceptors.response.use(r=>r, e=>{ if(e.response?.status===401){ localStorage.removeItem('snm_token'); localStorage.removeItem('snm_user'); } return Promise.reject(e); });
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
export default api;
