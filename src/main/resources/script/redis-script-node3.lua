local prefix = '__idgenerator:'; 

local step = 3; 
local startStep = 2; 

local tag = KEYS[1]; 
local year = KEYS[2]; 
local day = KEYS[3]; 

local year_key = redis.call('GET',prefix..tag..':year'); 
local day_key = redis.call('GET',prefix..tag..':day'); 

if year_key ~= year then 
	if year_key and (year - year_key) > 0 then 
		redis.call('SET',prefix..tag..':year', year); 
		redis.call('SET',prefix..tag..':day', day); 
		redis.call('SET',prefix..tag..':seq',startStep); 
	elseif not year_key then 
	    redis.call('SET',prefix..tag..':year', year);  
		redis.call('SET',prefix..tag..':seq',startStep); 
	end 
end 
if day_key ~= day then 
	if day_key and (day - day_key) > 0 then 
		redis.call('SET',prefix..tag..':day', day);  
		redis.call('SET',prefix..tag..':seq',startStep); 
	elseif not day_key then 
	    redis.call('SET',prefix..tag..':day', day);  
		redis.call('SET',prefix..tag..':seq',startStep); 
	end 
end 
local sequence 
if redis.call('GET',prefix..tag..':seq') == nil then 
	sequence = startStep; 
	redis.call('SET',prefix..tag..':seq',startStep); 
else 
	sequence = tonumber(redis.call('INCRBY', prefix..tag..':seq', step)) 
end 

return string.format('%02d', redis.call('GET',prefix..tag..':year'))..string.format('%03d', redis.call('GET',prefix..tag..':day'))..string.format('%012d', sequence)