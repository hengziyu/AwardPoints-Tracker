package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 迁移自 logger.py
 * 功能一比一复原：
 *  - get_logger(name): 获取具名 Logger
 *  - log_exception(logger, exception, message): 统一异常记录（包含堆栈）
 * 不增加多余优化，保持简单。
 */
public final class LoggerUtil {

    private LoggerUtil() { }

    /** 获取具名 Logger */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /** 记录异常（附带 message 与堆栈） */
    public static void logException(Logger logger, Throwable ex, String message) {
        if (logger == null) {
            logger = LoggerFactory.getLogger("UnknownLogger");
        }
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        logger.error(message + " | " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n" + sw.toString());
    }
}

