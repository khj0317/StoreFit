import { api } from '../lib/api'

export function uploadImages(files: File[]) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))

  return api
    .post<string[]>('/uploads/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data)
}
