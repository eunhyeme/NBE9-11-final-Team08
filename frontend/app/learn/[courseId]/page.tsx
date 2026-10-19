import { notFound } from 'next/navigation'
import { LearnView } from '@/components/learn/learn-view'
import { api } from '@/lib/api'

export default async function LearnPage({
  params,
}: {
  params: Promise<{ courseId: string }>
}) {
  const { courseId } = await params
  const course = await api.getCourse(courseId)
  if (!course) notFound()

  return <LearnView course={course} />
}
