package publisher

import mainargs.{main, arg, ParserForMethods, Flag}
import upickle.default.*

// Base trait for elements with module names
trait ModuleNamed:
  def moduleName: String

// ietf-interfaces module data structures
object IetfInterfaces:
  val MODULE_NAME = "ietf-interfaces"

  case class Statistics(
      discontinuityTime: String,
      inOctets: Long,
      inUnicastPkts: Long,
      inBroadcastPkts: Long,
      inMulticastPkts: Long,
      inDiscards: Long,
      inErrors: Long,
      inUnknownProtos: Long,
      outOctets: Long,
      outUnicastPkts: Long,
      outBroadcastPkts: Long,
      outMulticastPkts: Long,
      outDiscards: Long,
      outErrors: Long
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class Interface(
      name: String,
      description: String,
      `type`: String,
      enabled: Boolean,
      adminStatus: String,
      operStatus: String,
      lastChange: String,
      ifIndex: Int,
      physAddress: String,
      speed: Long,
      statistics: Statistics,
      // Augmentation from ietf-ip
      ipv4: IetfIp.IPv4,
      ipv6: IetfIp.IPv6
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class Interfaces(
      interface: Seq[Interface]
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

// ietf-ip module data structures
object IetfIp:
  val MODULE_NAME = "ietf-ip"

  case class IPv4Address(
      ip: String,
      prefixLength: Int
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class IPv4(
      enabled: Boolean,
      forwarding: Boolean,
      mtu: Int,
      address: Seq[IPv4Address]
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class IPv6Address(
      ip: String,
      prefixLength: Int
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class IPv6(
      enabled: Boolean,
      forwarding: Boolean,
      mtu: Int,
      address: Seq[IPv6Address]
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

// ietf-yang-push-2 notification data structures (simplified)
object IetfYangPush2:
  val MODULE_NAME = "ietf-yang-push-2"

  case class UpdateEntry(
      targetPath: String,
      data: InterfacesData
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class InterfacesData(
      interfaces: IetfInterfaces.Interfaces
  ) extends ModuleNamed:
    def moduleName: String = "ietf-interfaces"

  case class Update(
      id: String,
      snapshotType: String,
      observationTime: String,
      updates: Seq[UpdateEntry],
      complete: Boolean
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class Contents(
      update: IetfYangPush2.Update
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class Envelope(
      eventTime: String,
      hostname: String,
      sequenceNumber: Long,
      contents: Contents,
      publisherId: Long = 0L
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

  case class Root(
      envelope: Envelope
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

// RFC 7951 Namespace filtering
// Removes redundant module prefixes per RFC 7951 rules:
// - Top-level elements always have module prefixes
// - Child elements only need prefixes if different from parent module
object Rfc7951Filter:
  def filterNamespaces(
      json: ujson.Value,
      parentModule: Option[String] = None
  ): ujson.Value =
    json match
      case obj: ujson.Obj =>
        val newObj = ujson.Obj()
        obj.value.foreach: (key, value) =>
          // Check if key has module prefix
          if key.contains(":") then
            val Array(module, localName) = key.split(":", 2)
            // Keep prefix if parent module doesn't match, or if this is top level
            val shouldKeepPrefix =
              parentModule.isEmpty || parentModule.get != module
            val newKey = if shouldKeepPrefix then key else localName
            newObj(newKey) = filterNamespaces(value, Some(module))
          else
            // No prefix, just recurse with current parent module
            newObj(key) = filterNamespaces(value, parentModule)
        newObj

      case arr: ujson.Arr =>
        ujson.Arr(arr.value.map(v => filterNamespaces(v, parentModule)).toSeq*)

      case other => other

// Custom uPickle serialization with module prefixes
object ModulePrefixedWriter:
  import upickle.core.*

  // Helper to write with module prefix
  def writeWithPrefix[T](
      ctx: ObjVisitor[?, ?],
      key: String,
      value: T,
      moduleName: String,
      parentModule: String
  )(using w: Writer[T]): Unit =
    val prefixedKey =
      if moduleName != parentModule then s"$moduleName:$key" else key
    ctx.visitKey(-1).visitString(prefixedKey, -1)
    w.write(ctx.subVisitor, value)

object Main:
  @main
  def netconfReply(
      @arg(short = 'm', doc = "NETCONF message ID")
      messageId: String = "101",
      @arg(short = 'i', doc = "Subscription ID")
      subscriptionId: String = "dyn-52",
      @arg(
        short = 'o',
        doc = "Output file path (prints to stdout if not specified)"
      )
      output: String = "",
      @arg(
        short = 'l',
        doc = "Maximum line length for formatting (default: 80)"
      )
      maxLineLength: Int = 80,
      @arg(doc = "Compact output (no pretty printing)")
      compact: Flag = Flag()
  ): Unit =
    val xml = NetconfReq.establishSubscriptionReply(
      messageId = messageId,
      subscriptionId = subscriptionId
    )

    // Validate the XML
    try
      NetconfReq.validateReply(xml)

      // Write to file or stdout
      if output.nonEmpty then
        NetconfReq.writeToFile(
          xml,
          output,
          pretty = !compact.value,
          maxLineLength = maxLineLength
        )
        println(s"Successfully wrote NETCONF RPC reply to: $output")
      else
        val formattedOutput = NetconfReq.format(
          xml,
          pretty = !compact.value,
          maxLineLength = maxLineLength
        )
        println(formattedOutput)
    catch
      case e: IllegalArgumentException =>
        System.err.println(s"Validation failed: ${e.getMessage}")
        System.exit(1)
      case e: RuntimeException =>
        System.err.println(s"Error: ${e.getMessage}")
        System.exit(1)

  @main
  def netconfExample(
      @arg(short = 'm', doc = "NETCONF message ID")
      messageId: String = "101",
      @arg(short = 'n', doc = "Subscription name")
      name: String = "interfaces-subscription",
      @arg(short = 'd', doc = "Subscription description")
      description: String = "Periodic subscription to interface data",
      @arg(short = 's', doc = "Datastore to subscribe to")
      datastore: String = "operational",
      @arg(short = 'f', doc = "YPath filter expression")
      path: String = "/ietf-interfaces:interfaces",
      @arg(
        short = 'p',
        doc = "Update period in centiseconds (default: 3000 = 30 seconds)"
      )
      period: Int = 3000,
      @arg(short = 'e', doc = "Encoding format")
      encoding: String = "xml",
      @arg(short = 't', doc = "Anchor time (ISO 8601 format)")
      anchorTime: String = "",
      @arg(
        short = 'o',
        doc = "Output file path (prints to stdout if not specified)"
      )
      output: String = "",
      @arg(
        short = 'l',
        doc = "Maximum line length for formatting (default: 80)"
      )
      maxLineLength: Int = 80,
      @arg(doc = "Compact output (no pretty printing)")
      compact: Flag = Flag()
  ): Unit =
    val anchorTimeOpt = if anchorTime.isEmpty then None else Some(anchorTime)
    val descriptionOpt = if description.isEmpty then None else Some(description)

    val xml = NetconfReq.establishSubscription(
      messageId = messageId,
      subscriptionName = name,
      description = descriptionOpt,
      datastore = datastore,
      path = path,
      periodCentiseconds = period,
      encoding = encoding,
      anchorTime = anchorTimeOpt
    )

    // Validate the XML
    try
      NetconfReq.validate(xml)

      // Write to file or stdout
      if output.nonEmpty then
        NetconfReq.writeToFile(
          xml,
          output,
          pretty = !compact.value,
          maxLineLength = maxLineLength
        )
        println(s"Successfully wrote NETCONF subscription to: $output")
      else
        val formattedOutput = NetconfReq.format(
          xml,
          pretty = !compact.value,
          maxLineLength = maxLineLength
        )
        println(formattedOutput)
    catch
      case e: IllegalArgumentException =>
        System.err.println(s"Validation failed: ${e.getMessage}")
        System.exit(1)
      case e: RuntimeException =>
        System.err.println(s"Error: ${e.getMessage}")
        System.exit(1)

  @main
  def generate(
      @arg(short = 'i', doc = "Number of interfaces to generate")
      interfaces: Int = 4,
      @arg(short = 'p', doc = "Pretty print output")
      pretty: Flag = Flag(),
      @arg(
        short = 'n',
        doc = "Disable RFC 7951 namespace filtering (keep all module prefixes)"
      )
      noFilter: Flag = Flag(),
      @arg(short = 'e', doc = "Exclude envelope header (return contents only)")
      noEnvelope: Flag = Flag(),
      @arg(short = 's', doc = "Split interface list across N messages")
      splits: Int = 1,
      @arg(doc = "Split by data provider (statistics, ip, interface)")
      splitProvider: Flag = Flag(),
      @arg(doc = "Publisher process ID (0 is the parent)")
      publisherId: Long = 0L,
      @arg(short = 'o', doc = "Write sample JSON to this file")
      output: String = ""
  ): Unit =
    require(interfaces > 0, "The interface count must be positive")
    require(splits > 0, "The split count must be positive")
    require(publisherId >= 0 && publisherId <= 4294967295L,
      "The publisher ID must fit uint32")
    val notifications = SampleDataGenerator.generateNotifications(
      numInterfaces = interfaces,
      listSplits = splits,
      providerSplit = splitProvider.value,
      publisherId = publisherId
    )
    val messages = notifications.map { data =>
      val jsonObj =
        if noEnvelope.value then contentsToJson(data.envelope.contents)
        else toJsonObject(data)
      if noFilter.value then jsonObj
      else Rfc7951Filter.filterNamespaces(jsonObj)
    }
    // Arrays collect multiple sample messages; each element is a notification.
    val sample: ujson.Value =
      if messages.size == 1 then messages.head else ujson.Arr(messages*)
    val json = ujson.write(sample, indent = if pretty.value then 2 else -1)
    if output.nonEmpty then
      java.nio.file.Files.writeString(java.nio.file.Paths.get(output), json + "\n")
    else println(json)

  // Convert to ujson object structure
  def toJsonObject(root: IetfYangPush2.Root): ujson.Obj =
    ujson.Obj(
      s"${IetfYangPush2.MODULE_NAME}:envelope" -> envelopeToJson(
        root.envelope
      )
    )

  def envelopeToJson(env: IetfYangPush2.Envelope): ujson.Obj =
    val envelope = ujson.Obj(
      "event-time" -> env.eventTime,
      "hostname" -> env.hostname
    )
    if env.publisherId != 0 then
      envelope("publisher-id") = ujson.Num(env.publisherId.toDouble)
    envelope("sequence-number") = ujson.Num(env.sequenceNumber.toDouble)
    envelope("contents") = contentsToJson(env.contents)
    envelope

  def contentsToJson(contents: IetfYangPush2.Contents): ujson.Obj =
    ujson.Obj(
      s"${IetfYangPush2.MODULE_NAME}:update" -> updateToJson(contents.update)
    )

  def updateToJson(update: IetfYangPush2.Update): ujson.Obj =
    ujson.Obj(
      "id" -> update.id,
      "snapshot-type" -> update.snapshotType,
      "observation-time" -> update.observationTime,
      "updates" -> ujson.Arr(update.updates.map(updateEntryToJson)*),
      "complete" -> update.complete
    )

  def updateEntryToJson(entry: IetfYangPush2.UpdateEntry): ujson.Obj =
    val interfaces = entry.data.interfaces
    val payload =
      if entry.targetPath.endsWith("/statistics") then
        statisticsToJson(interfaces.interface.head.statistics)
      else if entry.targetPath.contains("[name=") then
        interfaceToJson(interfaces.interface.head)
      else interfacesToJson(interfaces)
    ujson.Obj(
      "target" -> entry.targetPath,
      "update" -> payload
    )

  def interfacesToJson(interfaces: IetfInterfaces.Interfaces): ujson.Obj =
    ujson.Obj(
      s"${IetfInterfaces.MODULE_NAME}:interface" -> ujson.Arr(
        interfaces.interface.map(interfaceToJson)*
      )
    )

  def interfaceToJson(iface: IetfInterfaces.Interface): ujson.Obj =
    val baseFields = ujson.Obj("name" -> iface.name)

    // Check if this is a partial interface (for provider split)
    // If only name + one section (statistics or IP), only include name and that section
    val hasOnlyStatistics = iface.statistics != null &&
      iface.description == null &&
      iface.`type` == null &&
      iface.ipv4 == null &&
      iface.ipv6 == null

    val hasOnlyIP = (iface.ipv4 != null || iface.ipv6 != null) &&
      iface.description == null &&
      iface.`type` == null &&
      iface.statistics == null

    // Add non-null fields only if this is not a partial interface
    if iface.description != null then
      baseFields("description") = iface.description
    if iface.`type` != null then baseFields("type") = iface.`type`

    // Only add enabled if we have other interface data (not just statistics or IP)
    if !hasOnlyStatistics && !hasOnlyIP then
      baseFields("enabled") = iface.enabled

    if iface.adminStatus != null then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:admin-status") =
        iface.adminStatus
    if iface.operStatus != null then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:oper-status") =
        iface.operStatus
    if iface.lastChange != null then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:last-change") =
        iface.lastChange
    if iface.ifIndex != 0 then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:if-index") = iface.ifIndex
    if iface.physAddress != null then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:phys-address") =
        iface.physAddress
    if iface.speed != 0 then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:speed") = iface.speed
    if iface.statistics != null then
      baseFields(s"${IetfInterfaces.MODULE_NAME}:statistics") =
        statisticsToJson(iface.statistics)

    // Add IPv4 if present
    if iface.ipv4 != null then
      baseFields(s"${IetfIp.MODULE_NAME}:ipv4") = ipv4ToJson(iface.ipv4)

    // Add IPv6 if present
    if iface.ipv6 != null then
      baseFields(s"${IetfIp.MODULE_NAME}:ipv6") = ipv6ToJson(iface.ipv6)

    baseFields

  def statisticsToJson(stats: IetfInterfaces.Statistics): ujson.Obj =
    ujson.Obj(
      "discontinuity-time" -> stats.discontinuityTime,
      "in-octets" -> stats.inOctets,
      "in-unicast-pkts" -> stats.inUnicastPkts,
      "in-broadcast-pkts" -> stats.inBroadcastPkts,
      "in-multicast-pkts" -> stats.inMulticastPkts,
      "in-discards" -> ujson.Num(stats.inDiscards.toDouble),
      "in-errors" -> ujson.Num(stats.inErrors.toDouble),
      "in-unknown-protos" -> ujson.Num(stats.inUnknownProtos.toDouble),
      "out-octets" -> stats.outOctets,
      "out-unicast-pkts" -> stats.outUnicastPkts,
      "out-broadcast-pkts" -> stats.outBroadcastPkts,
      "out-multicast-pkts" -> stats.outMulticastPkts,
      "out-discards" -> ujson.Num(stats.outDiscards.toDouble),
      "out-errors" -> ujson.Num(stats.outErrors.toDouble)
    )

  def ipv4ToJson(ipv4: IetfIp.IPv4): ujson.Obj =
    val data = ujson.Obj(
      "enabled" -> ipv4.enabled,
      "forwarding" -> ipv4.forwarding,
      "mtu" -> ipv4.mtu
    )
    if ipv4.address.nonEmpty then
      data("address") = ujson.Arr(ipv4.address.map(ipv4AddressToJson)*)
    data

  def ipv4AddressToJson(addr: IetfIp.IPv4Address): ujson.Obj =
    ujson.Obj(
      "ip" -> addr.ip,
      "prefix-length" -> addr.prefixLength
    )

  def ipv6ToJson(ipv6: IetfIp.IPv6): ujson.Obj =
    val data = ujson.Obj(
      "enabled" -> ipv6.enabled,
      "forwarding" -> ipv6.forwarding,
      "mtu" -> ipv6.mtu
    )
    if ipv6.address.nonEmpty then
      data("address") = ujson.Arr(ipv6.address.map(ipv6AddressToJson)*)
    data

  def ipv6AddressToJson(addr: IetfIp.IPv6Address): ujson.Obj =
    ujson.Obj(
      "ip" -> addr.ip,
      "prefix-length" -> addr.prefixLength
    )

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)

// Sample data generator
object SampleDataGenerator:
  import java.time.Instant
  import java.time.format.DateTimeFormatter

  def generateNotifications(
      numInterfaces: Int,
      listSplits: Int = 1,
      providerSplit: Boolean = false,
      publisherId: Long = 0L
  ): Seq[IetfYangPush2.Root] =
    val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    val interfaces = generateInterfaces(numInterfaces)

    val updateEntries =
      if providerSplit then
        // Split by data provider (statistics, IP, other interface data)
        generateProviderSplitEntries(interfaces)
      else if listSplits > 1 then
        // Split interface list across multiple messages
        generateListSplitEntries(interfaces, listSplits)
      else
        // Single message with all interfaces
        Seq(
          IetfYangPush2.UpdateEntry(
            targetPath = "/ietf-interfaces:interfaces/interface",
            data = IetfYangPush2.InterfacesData(
              interfaces = IetfInterfaces.Interfaces(interface = interfaces)
            )
          )
        )

    updateEntries.zipWithIndex.map { (entry, index) =>
      IetfYangPush2.Root(
        envelope = IetfYangPush2.Envelope(
          eventTime = now,
          hostname = "example-router",
          sequenceNumber = 3219L + index,
          publisherId = publisherId,
          contents = IetfYangPush2.Contents(
            update = IetfYangPush2.Update(
              id = "interfaces",
              snapshotType = "periodic",
              observationTime = now,
              updates = Seq(entry),
              complete = index == updateEntries.size - 1
            )
          )
        )
      )
    }

  // Split interface list across N messages
  def generateListSplitEntries(
      interfaces: Seq[IetfInterfaces.Interface],
      splits: Int
  ): Seq[IetfYangPush2.UpdateEntry] =
    val interfacesPerSplit =
      Math.ceil(interfaces.length.toDouble / splits).toInt
    interfaces
      .grouped(interfacesPerSplit)
      .map { group =>
        IetfYangPush2.UpdateEntry(
          targetPath = "/ietf-interfaces:interfaces/interface",
          data = IetfYangPush2.InterfacesData(
            interfaces = IetfInterfaces.Interfaces(interface = group.toSeq)
          )
        )
      }
      .toSeq

  // Split by data provider (statistics, IP info, other interface data)
  def generateProviderSplitEntries(
      interfaces: Seq[IetfInterfaces.Interface]
  ): Seq[IetfYangPush2.UpdateEntry] =
    // For each interface, create 3 separate update entries
    interfaces.flatMap { iface =>
      Seq(
        // 1. Interface data without statistics or IP
        IetfYangPush2.UpdateEntry(
          targetPath =
            s"/ietf-interfaces:interfaces/interface[name='${iface.name}']",
          data = IetfYangPush2.InterfacesData(
            interfaces = IetfInterfaces.Interfaces(
              interface = Seq(
                iface.copy(
                  statistics = null,
                  ipv4 = null,
                  ipv6 = null
                )
              )
            )
          )
        ),
        // 2. Statistics data
        IetfYangPush2.UpdateEntry(
          targetPath =
            s"/ietf-interfaces:interfaces/interface[name='${iface.name}']/statistics",
          data = IetfYangPush2.InterfacesData(
            interfaces = IetfInterfaces.Interfaces(
              interface = Seq(
                IetfInterfaces.Interface(
                  name = iface.name,
                  description = null,
                  `type` = null,
                  enabled = false,
                  adminStatus = null,
                  operStatus = null,
                  lastChange = null,
                  ifIndex = 0,
                  physAddress = null,
                  speed = 0,
                  statistics = iface.statistics,
                  ipv4 = null,
                  ipv6 = null
                )
              )
            )
          )
        ),
        // 3. IP data
        IetfYangPush2.UpdateEntry(
          targetPath =
            s"/ietf-interfaces:interfaces/interface[name='${iface.name}']",
          data = IetfYangPush2.InterfacesData(
            interfaces = IetfInterfaces.Interfaces(
              interface = Seq(
                IetfInterfaces.Interface(
                  name = iface.name,
                  description = null,
                  `type` = null,
                  enabled = false,
                  adminStatus = null,
                  operStatus = null,
                  lastChange = null,
                  ifIndex = 0,
                  physAddress = null,
                  speed = 0,
                  statistics = null,
                  ipv4 = iface.ipv4,
                  ipv6 = iface.ipv6
                )
              )
            )
          )
        )
      )
    }

  def generateInterfaces(count: Int): Seq[IetfInterfaces.Interface] =
    (0 until count).map: i =>
      val isDown = i == 3 // Last interface is admin-down
      val baseTime = "2024-10-10T08:00:00.00Z"

      IetfInterfaces.Interface(
        name = s"Eth$i",
        description = s"Ethernet interface $i",
        `type` = "iana-if-type:ethernetCsmacd",
        enabled = !isDown,
        adminStatus = if isDown then "down" else "up",
        operStatus = if isDown then "down" else "up",
        lastChange = baseTime,
        ifIndex = i + 1,
        physAddress = f"00:11:22:33:44:${i}%02x",
        speed = 1000000000L, // 1 Gbps
        statistics = generateStatistics(isDown),
        ipv4 = generateIPv4(i, isDown),
        ipv6 = generateIPv6(i, isDown)
      )

  def generateStatistics(isDown: Boolean): IetfInterfaces.Statistics =
    val baseTime = "2024-10-10T08:00:00.00Z"
    if isDown then
      IetfInterfaces.Statistics(
        discontinuityTime = baseTime,
        inOctets = 0L,
        inUnicastPkts = 0L,
        inBroadcastPkts = 0L,
        inMulticastPkts = 0L,
        inDiscards = 0L,
        inErrors = 0L,
        inUnknownProtos = 0L,
        outOctets = 0L,
        outUnicastPkts = 0L,
        outBroadcastPkts = 0L,
        outMulticastPkts = 0L,
        outDiscards = 0L,
        outErrors = 0L
      )
    else
      IetfInterfaces.Statistics(
        discontinuityTime = baseTime,
        inOctets = 1234567890L,
        inUnicastPkts = 9876543L,
        inBroadcastPkts = 12345L,
        inMulticastPkts = 54321L,
        inDiscards = 10L,
        inErrors = 5L,
        inUnknownProtos = 0L,
        outOctets = 9876543210L,
        outUnicastPkts = 8765432L,
        outBroadcastPkts = 23456L,
        outMulticastPkts = 65432L,
        outDiscards = 8L,
        outErrors = 3L
      )

  def generateIPv4(index: Int, isDown: Boolean): IetfIp.IPv4 =
    // Use RFC 5737 TEST-NET-1 documentation range: 192.0.2.0/24
    val addresses =
      if isDown then Seq.empty
      else
        Seq(
          IetfIp.IPv4Address(
            ip = s"192.0.2.${index + 1}",
            prefixLength = 24
          )
        )

    IetfIp.IPv4(
      enabled = !isDown,
      forwarding = false,
      mtu = 1500,
      address = addresses
    )

  def generateIPv6(index: Int, isDown: Boolean): IetfIp.IPv6 =
    // Use RFC 3849 documentation range: 2001:db8::/32
    val addresses =
      if isDown then Seq.empty
      else
        Seq(
          IetfIp.IPv6Address(
            ip = s"2001:db8::${index + 1}",
            prefixLength = 64
          )
        )

    IetfIp.IPv6(
      enabled = !isDown,
      forwarding = false,
      mtu = 1500,
      address = addresses
    )
