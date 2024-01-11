for i = 1, #KEYS do
    redis.call('DEL', KEYS[i])
    redis.call('DEL', 'history:' .. KEYS[i])
end
return true