package fr.hmil.roshttp

import java.nio.ByteBuffer

import fr.hmil.roshttp.body.BulkBodyPart
import fr.hmil.roshttp.body.JSONBody.JSONValue

/**
  * Created by antonkulaga on 4/26/17.
  */
case class AnyBody(value: String, contentType: String = s"application/json; charset=utf-8")  extends BulkBodyPart {

  override def contentData: ByteBuffer = ByteBuffer.wrap(value.getBytes("utf-8"))
}