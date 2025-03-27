package cn.labzen.web.spring.runtime

import cn.labzen.web.response.result.DownloadableResult
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.util.StreamUtils
import java.io.FileNotFoundException

class LabzenResourceMessageConverter : AbstractHttpMessageConverter<DownloadableResult>() {

  override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean {
    return false
  }

  override fun canWrite(clazz: Class<*>, mediaType: MediaType?): Boolean {
    return DownloadableResult::class.java.isAssignableFrom(clazz)
  }

  override fun supports(clazz: Class<*>): Boolean {
    return true
  }

  override fun readInternal(clazz: Class<out DownloadableResult>, inputMessage: HttpInputMessage): DownloadableResult {
    // no implementation here
    throw HttpMessageNotReadableException("no implementation here", inputMessage)
  }

  override fun writeInternal(result: DownloadableResult, outputMessage: HttpOutputMessage) {
    val headers = outputMessage.headers
    val attachment = result.attachment

    headers.contentType = result.contentType ?: MediaType.APPLICATION_OCTET_STREAM
    headers.contentDisposition = ContentDisposition.attachment().filename(result.filename ?: attachment.name).build()
    headers.contentLength = attachment.length()
    headers.accessControlExposeHeaders = listOf("Content-Disposition")

    val resource = FileSystemResource(attachment)
    writeContent(resource, outputMessage)
  }

  private fun writeContent(resource: Resource, outputMessage: HttpOutputMessage) {
    try {
      val `in` = resource.inputStream
      try {
        StreamUtils.copy(`in`, outputMessage.body)
      } catch (ex: NullPointerException) {
        // ignore, see SPR-13620
      } finally {
        try {
          `in`.close()
        } catch (ex: Throwable) {
          // ignore, see SPR-12999
        }
      }
    } catch (ex: FileNotFoundException) {
      // ignore, see SPR-12999
    }
  }
}