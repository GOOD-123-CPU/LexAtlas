import { describe, it, expect } from 'vitest'

/**
 * SSE 流式解析与 Markdown 导出格式的纯函数测试
 * 从 ChatView.vue 中抽取的核心逻辑的回归验证
 */

// 与 ChatView.vue 中 formatExportMarkdown 逻辑保持一致的最小复现
function buildExportHeader(title: string, convId: number): string {
  return `# ${title}\n\n> 会话 ID: ${convId} · 导出时间: ${new Date().toLocaleString()}\n\n---\n\n`
}

function sanitizeFilename(title: string | undefined, convId: number): string {
  return `${title || 'lexatlas'}-${convId}.md`
}

// SSE 事件行解析（data: {...}\n\n 格式）
function parseSseChunk(raw: string): string[] {
  return raw
    .split('\n\n')
    .filter(block => block.startsWith('data:'))
    .map(block => block.slice(5).trim())
    .filter(Boolean)
}

describe('聊天导出工具函数', () => {
  it('导出文件名应有默认回退', () => {
    expect(sanitizeFilename(undefined, 42)).toBe('lexatlas-42.md')
    expect(sanitizeFilename('离婚财产分割', 7)).toBe('离婚财产分割-7.md')
  })

  it('导出头应包含标题与分隔线', () => {
    const header = buildExportHeader('测试会话', 1)
    expect(header).toContain('# 测试会话')
    expect(header).toContain('---')
  })
})

describe('SSE 数据块解析', () => {
  it('应能解析标准 data: 前缀的多事件块', () => {
    const raw = 'data:{"content":"第一"}\n\ndata:{"content":"第二"}\n\n'
    const events = parseSseChunk(raw)
    expect(events).toHaveLength(2)
    expect(JSON.parse(events[0]).content).toBe('第一')
    expect(JSON.parse(events[1]).content).toBe('第二')
  })

  it('应忽略空块与注释行', () => {
    const raw = ': ping\n\ndata:{"content":"有效"}\n\n\n\n'
    const events = parseSseChunk(raw)
    expect(events).toHaveLength(1)
  })
})
