import axios from 'axios'

const envBase = (import.meta.env.VITE_API_BASE_URL || '').trim()
const runtimeBase = typeof window !== 'undefined' ? `${window.location.origin}/api` : ''

export const api = axios.create({
  baseURL: envBase || runtimeBase || 'http://localhost:8080/api'
})
