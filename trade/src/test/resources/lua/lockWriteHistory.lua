-- lockWriteHistory.lua
local ret = {}
for i = 1, #KEYS do
    local isLock = redis.call('SET', KEYS[i], ARGV[1], 'PX', ARGV[2], 'NX')
    local historyKey = 'history:' .. KEYS[i]
    if isLock then
        -- 락이 없다면, 0 ~ 현재까지로 추가
        redis.call("HSET", historyKey, 'timestamp', ARGV[i+2])
        redis.call('PEXPIRE', historyKey, ARGV[2])
        table.insert(ret, KEYS[i] .. ':0:' .. ARGV[i+2])
    else
        -- 락이 이미 있다면, 기록 갱신
        local previousTS = redis.call('HGET', historyKey, 'timestamp')
        if ARGV[i+2] < previousTS then
            return ret
        end
        if previousTS ~= nil then
            redis.call('HSET', historyKey, 'timestamp', ARGV[i+2])
            redis.call('PEXPIRE', historyKey, ARGV[2])
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            table.insert(ret, KEYS[i] .. ':' .. previousTS .. ':' .. ARGV[i+2])
        else
            redis.call('HSET', historyKey, 'timestamp', ARGV[i+2])
            redis.call('PEXPIRE', historyKey, ARGV[2])
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            table.insert(ret, KEYS[i] .. ':0:' .. ARGV[i+2])
        end
    end
end
return ret