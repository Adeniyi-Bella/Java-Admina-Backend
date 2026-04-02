local current = redis.call('INCR', KEYS[1])
local max_documents = tonumber(ARGV[1])
if current > max_documents then
  redis.call('DECR', KEYS[1])
  return 0
end
if current == 1 then
  redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return 1
