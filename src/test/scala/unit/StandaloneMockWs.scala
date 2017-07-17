package unit

import mockws.MockWS
import play.api.libs.ws.{StandaloneWSClient, StandaloneWSRequest}

// TODO remove once this is fixed: https://github.com/leanovate/play-mockws/issues/20
object StandaloneMockWs{
  def apply(mockWs: MockWS) = new StandaloneMockWs(mockWs)
}
class StandaloneMockWs(mockWs: MockWS) extends StandaloneWSClient{
  override def underlying[T]: T = mockWs.underlying[T]

  override def url(url: String): StandaloneWSRequest = mockWs.url(url)

  override def close(): Unit = mockWs.close()
}