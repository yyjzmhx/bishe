/**
 * 歌词行接口
 */
export interface LyricsLine {
  time: number  // 时间（秒）
  text: string  // 歌词文本
}

/**
 * 解析LRC格式歌词
 * LRC格式示例：
 * [00:12.50]第一句歌词
 * [00:15.30]第二句歌词
 * [01:23.45]第三句歌词
 */
export function parseLRC(lrcText: string): LyricsLine[] {
  if (!lrcText || !lrcText.trim()) {
    return []
  }

  const lines: LyricsLine[] = []
  const lrcLines = lrcText.split('\n')

  for (const line of lrcLines) {
    const trimmedLine = line.trim()
    if (!trimmedLine) continue

    // 匹配时间标签，格式：[mm:ss.xx] 或 [mm:ss:xx]
    const timeRegex = /\[(\d{2}):(\d{2})[\.:](\d{2})\]/g
    const text = trimmedLine.replace(timeRegex, '').trim()

    if (!text) continue

    // 重置正则表达式
    const regex = /\[(\d{2}):(\d{2})[\.:](\d{2})\]/g
    let match
    let hasTime = false

    // 匹配所有时间标签（一行可能对应多个时间）
    while ((match = regex.exec(trimmedLine)) !== null) {
      hasTime = true
      const minutes = parseInt(match[1], 10)
      const seconds = parseInt(match[2], 10)
      const centiseconds = parseInt(match[3], 10)
      
      // 转换为秒
      const time = minutes * 60 + seconds + centiseconds / 100
      
      lines.push({
        time,
        text
      })
    }

    // 如果没有匹配到时间标签，但有文本，则作为没有时间的歌词行（通常用于空行或注释）
    if (!hasTime && text && !text.startsWith('[')) {
      lines.push({
        time: -1, // -1表示没有时间标签
        text
      })
    }
  }

  // 按时间排序，-1的时间排在最后
  return lines.sort((a, b) => {
    if (a.time < 0) return 1
    if (b.time < 0) return -1
    return a.time - b.time
  })
}

/**
 * 根据当前播放时间获取当前应该显示的歌词索引
 */
export function getCurrentLyricsIndex(lines: LyricsLine[], currentTime: number): number {
  if (!lines || lines.length === 0) return -1
  if (currentTime < 0) return -1

  // 从后往前查找，找到最后一个时间小于等于当前时间的歌词行
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i].time >= 0 && lines[i].time <= currentTime) {
      return i
    }
  }

  return -1
}

