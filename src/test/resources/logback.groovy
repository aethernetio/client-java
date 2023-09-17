import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.core.spi.FilterReply

statusListener(NopStatusListener)
appender("SystemWarn", ConsoleAppender) {
    withJansi = true
    filter(LevelFilter) {
        level = WARN
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%yellow(%-5level [%d{yyyy-MM-dd HH:mm}] %-13logger{0}: %msg) %n"
    }
    target = 'System.err'

}
appender("SystemInfo", ConsoleAppender) {
    withJansi = true
    filter(LevelFilter) {
        level = INFO
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%blue(%-5level [%d{yyyy-MM-dd HH:mm}] %-12logger{0}: %msg) %n"
    }
    target = 'System.out'
}
appender("SystemDebug", ConsoleAppender) {
    withJansi = true
    filter(LevelFilter) {
        level = DEBUG
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%cyan(%-5level [%d{yyyy-MM-dd HH:mm}] %-12logger{0}: %msg) %n"
    }
    target = 'System.out'
}
appender("SystemError", ConsoleAppender) {
    filter(LevelFilter) {
        level = ERROR
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%red(%-5level [%d{yyyy-MM-dd HH:mm}] %-12logger{0}: %msg) %n"
    }
    target = 'System.err'
}

root(ERROR)
logger("com.aether", DEBUG, ["SystemInfo", 'SystemDebug', 'SystemError', 'SystemWarn'])
