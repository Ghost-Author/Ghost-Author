import axios from 'axios'

const envBase = (import.meta.env.VITE_API_BASE_URL || '').trim()

export const api = axios.create({
  baseURL: envBase || 'http://localhost:8080/api'
})
