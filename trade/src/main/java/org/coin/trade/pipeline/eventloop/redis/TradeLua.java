package org.coin.trade.pipeline.eventloop.redis;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class TradeLua {
    // 락 획득, 처리중인 데이터에 대한 기록, 스크립트
    public static final String LOCK_WRITE_HISTORY;
    // 언락, 처리중인 데이터에 대한 기록 삭제, 스크립트
    public static final String UNLOCK_DELETE_HISTORY;
    // 주문 삭제, 스크립트
    public static final String DEREGISTER_ORDER;

    static {
        LOCK_WRITE_HISTORY = getLuaAsString("lua/lockWriteHistory.lua");
        UNLOCK_DELETE_HISTORY = getLuaAsString("lua/unlockDeleteHistory.lua");
        DEREGISTER_ORDER = getLuaAsString("lua/deregisterOrder.lua");
    }

    private static String getLuaAsString(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            InputStream inputStream = resource.getInputStream();
            Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
