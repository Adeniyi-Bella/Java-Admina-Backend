local current = tonumber(redis.call('GET', KEYS[1]) or '0')
if current <= 0 then
  redis.call('DEL', KEYS[1])
  return 0
end
current = redis.call('DECR', KEYS[1])
if current <= 0 then
  redis.call('DEL', KEYS[1])
end
return current
