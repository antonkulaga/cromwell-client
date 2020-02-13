package group.research.aging.cromwell.web.utils


import wvlet.log.LogFormatter.{appendStackTrace, highlightLog, withColor}
import wvlet.log.{LogFormatter, LogRecord}
import wvlet.log.LogTimestampFormatter.formatTimestamp


object SimpleSourceFormatter extends SimpleSourceFormatter(1000)
class SimpleSourceFormatter(cutOffLimit: Int) extends LogFormatter {

  def cutOff(message: String) = {
    if(message.length < cutOffLimit) message else message.substring(0, cutOffLimit) + "..."
  }

  override def formatLog(r: LogRecord): String = {
    val loc =
      r.source
        .map(source => s" - (${source.fileLoc})")
        .getOrElse("")

    val logTag = r.level.name
    val log =
      f"${formatTimestamp(r.getMillis)} ${logTag} [${r.leafLoggerName}] ${cutOff(r.getMessage)} ${loc}"
    appendStackTrace(log, r)
  }
}

