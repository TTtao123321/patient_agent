-- 更新旧会话标题为用户第一个提问
-- 执行前请先备份数据库！

UPDATE chat_session cs
INNER JOIN (
    SELECT 
        cm.session_id,
        cm.content
    FROM chat_message cm
    WHERE cm.sequence_no = 1 
      AND cm.sender_type = 'USER'
      AND cm.is_deleted = 0
) first_message ON cs.id = first_message.session_id
SET cs.title = first_message.content
WHERE (cs.title = '新对话' OR cs.title IS NULL)
  AND cs.is_deleted = 0;

-- 查看更新结果
SELECT 
    cs.id,
    cs.session_no,
    cs.title AS new_title,
    cm.content AS first_user_message
FROM chat_session cs
LEFT JOIN chat_message cm ON cs.id = cm.session_id 
    AND cm.sequence_no = 1 
    AND cm.sender_type = 'USER'
    AND cm.is_deleted = 0
WHERE cs.is_deleted = 0
ORDER BY cs.id DESC;
