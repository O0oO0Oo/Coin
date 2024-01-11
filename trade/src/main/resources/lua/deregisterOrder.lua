-- lock 이 존재
if redis.call('EXISTS', KEYS[1]) == 1 then
    -- 기록이 존재
    if redis.call('HGET', KEYS[2], 'timestamp') then
        -- 현재 처리중인 시간보다, 나중에 등록된 주문이면
        if redis.call('HGET', KEYS[2], 'timestamp') <= ARGV[1] then
            -- 삭제한다.
            redis.call('ZREM', KEYS[3], ARGV[2])
            return true
        end
    end
    -- 실패하면 false 반환
    return false
end
-- 락이 존재하지 않으면 삭제
redis.call('ZREM', KEYS[3], ARGV[2])
return true