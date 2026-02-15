import axios from 'axios'

const envBase = (import.meta.env.VITE_API_BASE_URL || '').trim()
const AUTH_TOKEN_KEY = 'ga-auth-token'

export const api = axios.create({
  baseURL: envBase || 'http://localhost:8080/api'
})

export function setApiAuthToken(token) {
  const clean = (token || '').trim()
  if (!clean) {
    delete api.defaults.headers.common.Authorization
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(AUTH_TOKEN_KEY)
    }
    return
  }
  api.defaults.headers.common.Authorization = `Bearer ${clean}`
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(AUTH_TOKEN_KEY, clean)
  }
}

export function loadApiAuthToken() {
  if (typeof window === 'undefined') {
    return ''
  }
  const token = (window.localStorage.getItem(AUTH_TOKEN_KEY) || '').trim()
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`
  }
  return token
}
