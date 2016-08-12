package unit

import java.net.URI

import io.waylay.kairosdb.driver.models.KairosDBConfig
import org.specs2.matcher.ResultMatchers
import org.specs2.mutable.Specification

class KairosDBConfigSpec extends Specification with ResultMatchers {
  "Creating a KairosDBConfig from a Java URI" should {
    "work correctly" in {
      val javaUri = URI.create("https://example.org:9999")
      KairosDBConfig(javaUri) should be equalTo KairosDBConfig("https", "example.org", 9999)
    }

    "work correctly with authentication" in {
      val javaUri = URI.create("https://user:pass@example.org:9999")
      KairosDBConfig(javaUri) should be equalTo KairosDBConfig("https", "example.org", 9999, Some("user"), Some("pass"))
    }
  }

}
