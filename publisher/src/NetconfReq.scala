package publisher

import scala.xml._

/** NETCONF RPC generator for the ietf-yang-push-2 module. */
object NetconfReq:
  // Namespace constants
  val NS_NETCONF = "urn:ietf:params:xml:ns:netconf:base:1.0"
  val NS_YP2 = "urn:ietf:params:xml:ns:yang:ietf-yang-push-2"
  val NS_DATASTORES = "urn:ietf:params:xml:ns:yang:ietf-datastores"
  val NS_INTERFACES = "urn:ietf:params:xml:ns:yang:ietf-interfaces"

  /** Generate an establish-subscription RPC request.
    *
    * @param messageId
    *   The NETCONF message ID
    * @param subscriptionName
    *   The name for this subscription
    * @param description
    *   Optional description of the subscription
    * @param datastore
    *   The datastore to subscribe to (e.g., "operational")
    * @param path
    *   The YPath filter expression (e.g., "/ietf-interfaces:interfaces")
    * @param periodCentiseconds
    *   The update period in centiseconds (100ths of a second)
    * @param encoding
    *   The encoding format ("xml" for NETCONF)
    * @param anchorTime
    *   Optional anchor time for periodic updates
    * @return
    *   A well-formed NETCONF RPC as an XML Elem
    */
  def establishSubscription(
      messageId: String = "101",
      subscriptionName: String = "interfaces-subscription",
      description: Option[String] = Some(
        "Periodic subscription to interface data"
      ),
      datastore: String = "operational",
      path: String = "/ietf-interfaces:interfaces",
      periodCentiseconds: Int = 3000, // 30 seconds
      encoding: String = "xml",
      anchorTime: Option[String] = None
  ): Elem =
    val rpc =
      <netconf:rpc message-id={messageId} xmlns:netconf={NS_NETCONF}>
        <establish-subscription xmlns={NS_YP2}>
          <id>{subscriptionName}</id>
          {
        description
          .map(d => <description>{d}</description>)
          .getOrElse(NodeSeq.Empty)
      }
          <target>
            <datastore xmlns:ds={NS_DATASTORES}>{s"ds:$datastore"}</datastore>
            <path>{path}</path>
          </target>
          <update-trigger>
            <periodic>
              <period>{periodCentiseconds}</period>
              {
        anchorTime
          .map(t => <anchor-time>{t}</anchor-time>)
          .getOrElse(NodeSeq.Empty)
      }
            </periodic>
          </update-trigger>
          <encoding>{encoding}</encoding>
        </establish-subscription>
      </netconf:rpc>

    rpc

  /** Generate an RPC reply for an establish-subscription request.
    *
    * @param messageId
    *   The NETCONF message ID (should match the request)
    * @param subscriptionId
    *   The string subscription ID assigned by the publisher
    * @return
    *   A well-formed NETCONF RPC reply as an XML Elem
    */
  def establishSubscriptionReply(
      messageId: String = "101",
      subscriptionId: String = "dyn-52"
  ): Elem =
    val reply =
      <rpc-reply message-id={messageId} xmlns={NS_NETCONF}>
        <id xmlns={NS_YP2}>{subscriptionId}</id>
      </rpc-reply>

    reply

  /** Validates that the generated XML is well-formed.
    *
    * @param elem
    *   The XML element to validate
    * @return
    *   True if valid, throws exception otherwise
    */
  def validate(elem: Elem): Boolean =
    // Basic validation checks
    require(
      elem.label == "rpc",
      s"Root element must be 'rpc', got '${elem.label}'"
    )
    require(
      elem.namespace == NS_NETCONF,
      s"Root element must be in NETCONF namespace, got '${elem.namespace}'"
    )
    require(
      (elem \ "establish-subscription").nonEmpty,
      "Missing establish-subscription element"
    )

    val subscription = (elem \ "establish-subscription").head
    require(
      subscription.namespace == NS_YP2,
      s"establish-subscription must be in ietf-yang-push-2 namespace"
    )
    require((subscription \ "id").nonEmpty, "Missing id element")
    require((subscription \ "target").nonEmpty, "Missing target element")
    require(
      (subscription \ "update-trigger").nonEmpty,
      "Missing update-trigger element"
    )
    require((subscription \ "encoding").nonEmpty, "Missing encoding element")

    val target = (subscription \ "target").head
    require((target \ "datastore").nonEmpty, "Missing datastore element")
    require((target \ "path").nonEmpty, "Missing path element")

    val updateTrigger = (subscription \ "update-trigger").head
    require((updateTrigger \ "periodic").nonEmpty, "Missing periodic element")

    val periodic = (updateTrigger \ "periodic").head
    require((periodic \ "period").nonEmpty, "Missing period element")

    true

  /** Validates that the generated RPC reply XML is well-formed.
    *
    * @param elem
    *   The XML element to validate
    * @return
    *   True if valid, throws exception otherwise
    */
  def validateReply(elem: Elem): Boolean =
    // Basic validation checks
    require(
      elem.label == "rpc-reply",
      s"Root element must be 'rpc-reply', got '${elem.label}'"
    )
    require(
      elem.namespace == NS_NETCONF,
      s"Root element must be in NETCONF namespace, got '${elem.namespace}'"
    )
    require(
      (elem \ "id").nonEmpty,
      "Missing id element in rpc-reply"
    )

    val id = (elem \ "id").head
    require(
      id.namespace == NS_YP2,
      s"id element must be in ietf-yang-push-2 namespace"
    )

    true

  /** Format the XML with proper indentation.
    *
    * @param elem
    *   The XML element to format
    * @param pretty
    *   Whether to use pretty printing
    * @param maxLineLength
    *   Maximum line length before wrapping (default: 80)
    * @return
    *   Formatted XML string
    */
  def format(
      elem: Elem,
      pretty: Boolean = true,
      maxLineLength: Int = 80
  ): String =
    if pretty then
      val printer = new PrettyPrinter(maxLineLength, 2)
      printer.format(elem)
    else elem.toString

  /** Write XML output to a file.
    *
    * @param elem
    *   The XML element to write
    * @param outputPath
    *   The file path to write to
    * @param pretty
    *   Whether to use pretty printing
    * @param maxLineLength
    *   Maximum line length before wrapping
    * @return
    *   Unit on success, throws exception on failure
    */
  def writeToFile(
      elem: Elem,
      outputPath: String,
      pretty: Boolean = true,
      maxLineLength: Int = 80
  ): Unit =
    import java.nio.file.{Files, Paths, StandardOpenOption}
    import java.nio.charset.StandardCharsets

    try
      val content = format(elem, pretty, maxLineLength)
      val path = Paths.get(outputPath)

      // Create parent directories if they don't exist
      Option(path.getParent).foreach(Files.createDirectories(_))

      // Write the file
      Files.writeString(
        path,
        content,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
    catch
      case e: Exception =>
        throw new RuntimeException(
          s"Failed to write to file '$outputPath': ${e.getMessage}",
          e
        )
