import axios from 'axios';
const api = axios.create({ baseURL:'/api', timeout:15000 });
api.interceptors.request.use(config=>{ const token=localStorage.getItem('snm_token'); if(token) config.headers.Authorization=`Bearer ${token}`; return config; });
api.interceptors.response.use(r=>r, e=>{ if(e.response?.status===401){ localStorage.removeItem('snm_token'); localStorage.removeItem('snm_user'); } return Promise.reject(e); });
export default api;
