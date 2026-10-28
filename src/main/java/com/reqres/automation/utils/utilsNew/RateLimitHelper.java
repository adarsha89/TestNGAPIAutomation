package com.reqres.automation.utils.utilsNew;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitHelper {
    private static final Map<String, AtomicLong> lastCalledAtMap = new ConcurrentHashMap<>();

    private RateLimitHelper(){}


    public static void acquire(String endpointKey, EnvConfig envConfig){
        String rawLimit = envConfig.getProperty(endpointKey + Constants.RATE_LIMIT_PER_SECOND_SUFFIX);
        double numberPerSec =  Double.parseDouble(rawLimit);
        AtomicLong lastCalledTime = lastCalledAtMap.get(endpointKey);
        synchronized (lastCalledTime){
            if(lastCalledTime == null){
                lastCalledTime = new AtomicLong();
                lastCalledTime.set(System.currentTimeMillis());
                lastCalledAtMap.put(endpointKey, lastCalledTime);
            }else{
                long remainingTime = (long) (((long)1000/numberPerSec)-(System.currentTimeMillis() - lastCalledTime.get()));
                sleep(remainingTime);

            }
        }


    }

    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
