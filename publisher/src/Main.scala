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

// ietf-yp-lite module data structures (simplified)
object IetfYpLite:
  val MODULE_NAME = "ietf-yp-lite"

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
      id: Int,
      snapshotType: String,
      observationTime: String,
      updates: Seq[UpdateEntry]
  ) extends ModuleNamed:
    def moduleName: String = MODULE_NAME

// ietf-yp-notification envelope structure
object IetfYpNotification:
  val MODULE_NAME = "ietf-yp-notification"

  case class Contents(
      update: IetfYpLite.Update
  ) extends ModuleNamed:
    def moduleName: String = "ietf-yp-lite"

  case class Envelope(
      eventTime: String,
      hostname: String,
      sequenceNumber: Long,
      contents: Contents
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
  def generate(
      @arg(short = 'i', doc = "Number of interfaces to generate")
      interfaces: Int = 4,
      @arg(short = 'p', doc = "Pretty print output")
      pretty: Flag = Flag(),
      @arg(
        short = 'n',
        doc = "Disable RFC 7951 namespace filtering (keep all module prefixes)"
      )
      noFilter: Flag = Flag()
  ): Unit =
    // Generate sample data
    val data = SampleDataGenerator.generateNotification(interfaces)
    val jsonObj = toJsonObject(data)

    // Apply RFC 7951 filtering by default (unless disabled with -n flag)
    val filteredJson =
      if noFilter.value then jsonObj
      else Rfc7951Filter.filterNamespaces(jsonObj)

    val json =
      if pretty.value then ujson.write(filteredJson, indent = 2)
      else ujson.write(filteredJson)
    println(json)

  // Convert to ujson object structure
  def toJsonObject(root: IetfYpNotification.Root): ujson.Obj =
    ujson.Obj(
      s"${IetfYpNotification.MODULE_NAME}:envelope" -> envelopeToJson(
        root.envelope
      )
    )

  def envelopeToJson(env: IetfYpNotification.Envelope): ujson.Obj =
    ujson.Obj(
      "event-time" -> env.eventTime,
      "hostname" -> env.hostname,
      "sequence-number" -> env.sequenceNumber,
      "contents" -> contentsToJson(env.contents)
    )

  def contentsToJson(contents: IetfYpNotification.Contents): ujson.Obj =
    ujson.Obj(
      s"${IetfYpLite.MODULE_NAME}:update" -> updateToJson(contents.update)
    )

  def updateToJson(update: IetfYpLite.Update): ujson.Obj =
    ujson.Obj(
      "id" -> update.id,
      "snapshot-type" -> update.snapshotType,
      "observation-time" -> update.observationTime,
      "updates" -> ujson.Arr(update.updates.map(updateEntryToJson)*)
    )

  def updateEntryToJson(entry: IetfYpLite.UpdateEntry): ujson.Obj =
    ujson.Obj(
      "target-path" -> entry.targetPath,
      "data" -> ujson.Obj(
        s"${IetfInterfaces.MODULE_NAME}:interfaces" -> interfacesToJson(
          entry.data.interfaces
        )
      )
    )

  def interfacesToJson(interfaces: IetfInterfaces.Interfaces): ujson.Obj =
    ujson.Obj(
      s"${IetfInterfaces.MODULE_NAME}:interface" -> ujson.Arr(
        interfaces.interface.map(interfaceToJson)*
      )
    )

  def interfaceToJson(iface: IetfInterfaces.Interface): ujson.Obj =
    val baseFields = ujson.Obj(
      "name" -> iface.name,
      "description" -> iface.description,
      "type" -> iface.`type`,
      "enabled" -> iface.enabled,
      s"${IetfInterfaces.MODULE_NAME}:admin-status" -> iface.adminStatus,
      s"${IetfInterfaces.MODULE_NAME}:oper-status" -> iface.operStatus,
      s"${IetfInterfaces.MODULE_NAME}:last-change" -> iface.lastChange,
      s"${IetfInterfaces.MODULE_NAME}:if-index" -> iface.ifIndex,
      s"${IetfInterfaces.MODULE_NAME}:phys-address" -> iface.physAddress,
      s"${IetfInterfaces.MODULE_NAME}:speed" -> iface.speed,
      s"${IetfInterfaces.MODULE_NAME}:statistics" -> statisticsToJson(
        iface.statistics
      )
    )

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
      "in-discards" -> stats.inDiscards,
      "in-errors" -> stats.inErrors,
      "in-unknown-protos" -> stats.inUnknownProtos,
      "out-octets" -> stats.outOctets,
      "out-unicast-pkts" -> stats.outUnicastPkts,
      "out-broadcast-pkts" -> stats.outBroadcastPkts,
      "out-multicast-pkts" -> stats.outMulticastPkts,
      "out-discards" -> stats.outDiscards,
      "out-errors" -> stats.outErrors
    )

  def ipv4ToJson(ipv4: IetfIp.IPv4): ujson.Obj =
    ujson.Obj(
      "enabled" -> ipv4.enabled,
      "forwarding" -> ipv4.forwarding,
      "mtu" -> ipv4.mtu,
      "address" -> ujson.Arr(ipv4.address.map(ipv4AddressToJson)*)
    )

  def ipv4AddressToJson(addr: IetfIp.IPv4Address): ujson.Obj =
    ujson.Obj(
      "ip" -> addr.ip,
      "prefix-length" -> addr.prefixLength
    )

  def ipv6ToJson(ipv6: IetfIp.IPv6): ujson.Obj =
    ujson.Obj(
      "enabled" -> ipv6.enabled,
      "forwarding" -> ipv6.forwarding,
      "mtu" -> ipv6.mtu,
      "address" -> ujson.Arr(ipv6.address.map(ipv6AddressToJson)*)
    )

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

  def generateNotification(numInterfaces: Int): IetfYpNotification.Root =
    val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    val interfaces = generateInterfaces(numInterfaces)

    IetfYpNotification.Root(
      envelope = IetfYpNotification.Envelope(
        eventTime = now,
        hostname = "example-router",
        sequenceNumber = 3219L,
        contents = IetfYpNotification.Contents(
          update = IetfYpLite.Update(
            id = 1011,
            snapshotType = "periodic",
            observationTime = now,
            updates = Seq(
              IetfYpLite.UpdateEntry(
                targetPath = "ietf-interfaces:interfaces/interface",
                data = IetfYpLite.InterfacesData(
                  interfaces = IetfInterfaces.Interfaces(
                    interface = interfaces
                  )
                )
              )
            )
          )
        )
      )
    )

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
