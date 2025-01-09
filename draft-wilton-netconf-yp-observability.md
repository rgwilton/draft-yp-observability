---
title: "YANG-Push Operational Data Observability Enhancements"
abbrev: "YANG-Push Observability"
category: info

docname: draft-wilton-netconf-yp-observability-latest
submissiontype: IETF
number:
date:
consensus: true
v: 3
area: "Operations and Management"
workgroup: "Network Configuration"
keyword:
 - YANG Push
 - Observability
 - Network Telemetry
 - Operational Data
venue:
  group: "Network Configuration"
  type: "Working Group"
  mail: "netconf@ietf.org"
  arch: "https://mailarchive.ietf.org/arch/browse/netconf/"
  github: "rgwilton/draft-yp-observability"
  latest: "https://rgwilton.github.io/draft-yp-observability/draft-wilton-netconf-yp-observability.html"

author:
 -
    fullname: Robert Wilton
    organization: Cisco Systems
    email: rwilton@cisco.com

normative:
  I-D.draft-netana-netconf-notif-envelope:
  RFC8340:
  RFC8341:

informative:
  RFC8342:
  RFC8343:
  RFC8639:
  RFC8641:
  I-D.ietf-nmop-network-anomaly-architecture:
  I-D.ietf-nmop-yang-message-broker-integration:
  I-D.draft-ietf-netconf-http-client-server:
  Kafka:
    target: https://kafka.apache.org/
    title: Apache Kafka
    author:
      - name: Apache.org
    date: false
  Consistency:
    target: https://en.wikipedia.org/wiki/Consistency_(database_systems)
    title: Consistency (database systems)
    author:
      - name: Wikipedia
    date: false
  EventualConsistency:
    target: https://www.techopedia.com/definition/29165/eventual-consistency
    title: Eventual Consistency
    author:
      - name: Margaret Rouse
    date: false

--- abstract

**This version of the document is aimed to be a base reference point to compare against to see how YANG Push Lite compares to the two core RFCs {{RFC8639}} & {{RFC8641}} that it is based on.  The next draft revision would serve as a better starting point to see the proposed protocol & data model for YANG Push Lite.**

YANG Push Lite is a simplified specification of YANG Push, specifically optimized for observability of operational data.

This early draft proposes some enhancements to YANG-Push to optimize its behavior for operational data telemetry.  It also lists some additional issues that could potentially be discussed if there is further interest in this work, in particular whether we should be attempting extensions to YANG-Push (as this document is currently framed) or instead should attempt to define a new 'YANG-Push lite'.

--- middle

# Document Status

**PLEASE READ - This version of the document is aimed to be a base reference point () to compare against to see how YANG Push Lite compares to the two core RFCs {{RFC8639}} & {{RFC8641}} that it is based on.  The next draft revision would serve as a better starting point to see the proposed protocol & data model for YANG Push Lite.**

Based on the feedback received during the IETF 121 NETCONF session, this document has currently been written as a self-contained lightweight replacement for {{RFC8639}} and {{RFC8641}}, defining a separate configuration data model.  It leverages {{I-D.draft-netana-netconf-notif-envelope}} as the envelope for the generated notifications with a new update message format.

# Acknowledgement to the authors of RFC 8639 and RFC 8641

This document is intended to be a lightweight alternative for {{RFC8639}} and {{RFC8641}}, but it intentionally reuses substantial part of the design and data model of those RFCs.

For ease of reference, this document, at least initially, sources most of the text and basis for the YANG model directly from those RFCs, rather than creating a separate document that would contain numerous references.

Hence, the authors of this draft would like to thank and acknowledge the very significant previous work by the authors and of those RFCs, i.e., Eric Voit, Alex Clemm, Alberto Gonzalez Prieto, Einar Nilsen-Nygaard, Ambika Prasad Tripathy, and also all those who contributed to the underlying work upon which this document is heavily based.

# Introduction

{{I-D.ietf-nmop-yang-message-broker-integration}} describes an architecture for how YANG-Push {{RFC8641}} can be integrated effectively with message brokers, e.g., {{Kafka}}, that is part of a wider architecture for a **Network Anomaly Detection Framework**, specified in {{I-D.ietf-nmop-network-anomaly-architecture}}.

YANG-Push is a key part of these architectures, but through experience of implementing YANG-Push specifically for the use cases described in the above architecture documents, it became clear that there are aspects of YANG-Push that are not optimal for these use cases for neither producer or consumer, particular as they relate to operational data.

For the consumer of the telemetry data, there is a requirement to associate a schema with the instance-data that will be provided by a subscription.  One approach is to fetch and build the entire schema for the device, e.g., by fetching YANG library, and then use the subscription XPath to select the relevant subtree of the schema that applies only to the subscription.  The problem with this approach is that if the schema ever changes, e.g., after a software update, then it is reasonably likely of some changes occurring with the global device schema even if there are no changes to the schema subtree under the subscription path.  Hence, it would be helpful to identify and version the schema associated with a particular subscription path, and also to encoded the instance data relatively to the subscription path rather than as an absolute path from the root of the operational datastore.

## Introduction text from Subscribed Notifications (RFC 8639)

This document defines a YANG data model and associated mechanisms
enabling subscriber-specific subscriptions to a publisher's event
streams.  This effectively enables a "subscribe, then publish"
capability where the customized information needs and access
permissions of each target receiver are understood by the publisher
before subscribed event records are marshaled and pushed.  The
receiver then gets a continuous, customized feed of
publisher-generated information.

While the functionality defined in this document is transport
agnostic, transports like the Network Configuration Protocol
(NETCONF) [RFC6241] or RESTCONF [RFC8040] can be used to configure or
dynamically signal subscriptions.  Bindings for subscribed event
record delivery for NETCONF and RESTCONF are defined in [RFC8640] and
[RESTCONF-Notif], respectively.

The YANG data model defined in this document conforms to the Network
Management Datastore Architecture defined in [RFC8342].

### Motivation

Various limitations to subscriptions as described in [RFC5277] were
alleviated to some extent by the requirements provided in [RFC7923].
Resolving any remaining issues is the primary motivation for this
work.  Key capabilities supported by this document include:

- multiple subscriptions on a single transport session

- support for dynamic and configured subscriptions

- modification of an existing subscription in progress

- per-subscription operational counters

- negotiation of subscription parameters (through the use of hints
  returned as part of declined subscription requests)

- subscription state change notifications (e.g., publisher-driven
  suspension, parameter modification)

- independence from transport

## Introduction text from YANG Push (RFC 8641)

Traditional approaches for providing visibility into managed entities
from a remote system have been built on polling.  With polling, data
is periodically requested and retrieved by a client from a server to
stay up to date.  However, there are issues associated with polling-
based management:

  *  Polling incurs significant latency.  This latency prohibits many
    types of applications.

  *  Polling cycles may be missed, and requests may be delayed or get
    lost -- often when the network is under stress and the need for
    the data is the greatest.

  *  Polling requests may undergo slight fluctuations, resulting in
    intervals of different lengths.  The resulting data is difficult
    to calibrate and compare.

  *  For applications that monitor for changes, many remote polling
    cycles place unwanted and ultimately wasteful load on the network,
    devices, and applications, particularly when changes occur only
    infrequently.

A more effective alternative to polling is for an application to
receive automatic and continuous updates from a targeted subset of a
datastore.  Accordingly, there is a need for a service that
(1) allows applications to subscribe to updates from a datastore and
(2) enables the server (also referred to as the "publisher") to push
and, in effect, stream those updates.  The requirements for such a
service have been documented in [RFC7923].

This document defines a corresponding solution that is built on
top of [RFC8639].  Supplementing that work are YANG data model
augmentations, extended RPCs, and new datastore-specific update
notifications.  Transport options provided in [RFC8639] will work
seamlessly with this solution.

## Motivations for YANG Push Lite

### Complexities in Modelling the Operational State Datastore

The YANG abstraction of a single datastore of related consistent data works very well for configuration that has a strong requirement to be self consistent, and that is always updated, and validated, in a transactional way.  But for producers of telemetry data, the YANG abstraction of a single operational datastore is not really possible for devices managing a non-trivial quantity of operational data.

Some systems may store their operational data in a single logical database, yet it is less likely that the operational data can always be updated in a transactional way, and often for memory efficiency reasons such a database does not store individual leaves, but instead semi-consistent records of data at a container or list entry level.

For other systems, the operational information may be distributed across multiple internal nodes (e.g., linecards), and potentially many different process daemons within those distributed nodes.  Such systems generally cannot exhibit full consistency {{Consistency}} of the operational data (which would require transactional semantics across all daemons and internal nodes), only offering an eventually consistent {{EventualConsistency}} view of the data instead.

In practice, many network devices will manage their operational data as a combination of some data being stored in a central operational datastore, and other, higher scale, and potentially more frequently changing data (e.g., statistics or FIB information) being stored elsewhere in a more memory efficient and performant way.

### YANG-Push enhancements

To address the needs described in the introduction and architecture documents, this document defines some minor extensions to YANG-Push that are designed to make YANG-Push work better both for producers and consumers of YANG telemetry data.

Currently, it:

- defines a new YANG-Push encoding format that can be used for both on-change and periodic subscriptions that reports the data from the subscription filter point.

- defines a combined periodic and on-change subscription that reports events both on a periodical cadence and also if changes to the data have occurred.

These are detailed in the following sections:

### New encoding format

This document proposes a new opt-in YANG-Push encoding format to use instead of the "push-update" and "push-change-update" notifications defined in {{RFC8641}}.

There are a few reasons for specifying a new encoding format:

1. To use the same encoding format for both periodic and on-change messages, allowing the same messages to be easily received and stored in a time-series database, making use of the same message schema when traversing message buses, such as Apache Kafka.

1. To allow the schema of the notifications to be rooted to the subscription point rather than always being to the root of the operational datastore schema.  This allows messages to be slightly less indented, and makes it easier to convert from a YANG schema to an equivalent message bus schema, where each message is defined with its own schema, rather than a single datastore schema.

1. To move away from the somewhat verbose YANG Patch format {{RFC8072}}, that is not really a great fit for encoding changes of operational data.  Many systems cannot necessarily distinguish between create versus update events (particularly for new subscriptions or after recovering from internal failures within the system), and hence cannot faithfully implement the full YANG Patch semantics defined in {{RFC8641}}.

1. To allow the device to split a subscription into smaller child subscriptions for more efficient independent and concurrent processing.  I.e., reusing the ideas from {{?I-D.ietf-netconf-distributed-notif}}.  However, all child subscriptions are still encoded from the same subscription point.

The practical differences in the encodings may be better illustrated via the examples in {{Examples}}.

### Combined periodic and on-change subscription

Sometimes it is helpful to have a single subscription that covers both periodic and on-change notifications (perhaps with dampening).

There are two ways in which this may be useful:

1. For generally slow changing data (e.g., a device's physical inventory), then on-change notifications may be most appropriate.  However, in case there is any lost notification that isn't always detected, for any reason, then it may also be helpful to have a slow cadence periodic backup notification of the data (e.g., once every 24 hours), to ensure that the management systems should always eventually converge on the current state in the network.

1. For data that is generally polled on a periodic basis (e.g., once every 10 minutes) and put into a time series database, then it may be helpful for some data trees to also get more immediate notifications that the data has changed.  Hence, a combined periodic and on-change subscription, potentially with some dampening, would facilitate more frequent notifications of changes of the state, to reduce the need of having to always wait for the next periodic event.

Hence, this document introduces the fairly intuitive "periodic-and-on-change" update trigger that creates a combined periodic and on-change subscription, and allows the same parameters to be configured.  For some use cases, e.g., where a time-series database is being updated, the new encoding format proposed previously may be most useful.

### Open Issues & Other Potential Enhancements/Changes

This section lists some other potential issues and enhancements that should be considered as part of this work.  If there is working group interest in progressing this work, then the issues in this section could potentially be better managed as github issues.

1. Should we consider a version of the JSON encoding that excludes module prefixes (either everywhere, or perhaps only include the top module prefix).  The reasoning for considering this is to potentially better align the JSON data with how the schema data may be modeled in other data systems, e.g., Kafka.  Obviously, this requires that there be no duplicate data node names in different module namespaces, but most sane device schemas would avoid this anyway.

1. Do we make use of the new notification-envelope format {{?I-D.netana-netconf-notif-envelope}} as the mandatory and only required notification format for these new forms of subscriptions.  I.e., reducing complexity by removing unnecessary flexibility and options?

1. Document how sub-subscriptions can be used to split a higher level subscription into multiple smaller more efficient subscriptions for the device (that can be handled concurrently).

1. The document's current focus is on configured subscriptions, aligned to the proposed initial deployment requirements, but the solution should probably be extended to support dynamic subscriptions, presuming that it is not hard to do so.

1. Some of the YANG-Push behavior is more complex and expensive to implement (e.g., the SHOULD requirement to suggest suitable alternative subscription parameters if a subscription is rejected, subscription dependencies).  Should this document update RFC 8639 or RFC 8641 to indicate that those requirements do not apply to these new extended subscriptions?  The goal of this work should be to specify the minimal required functionality to meet the requirements.

1. What document format should this work take?  The currently proposed approach is to add extra extensions to YANG-Push to cover the required functionality.  An alternative approach could be to write a RFC 8641-bis, or a 'YANG-Push lite'.

1. Currently the encoding and transport parameters are per subscription, but it may make more sense for these to be per receiver definition.  I.e., if you want to use different transports and encodings to the same receiver this should still be possible, but would require a second receiver to be defined with the same destination IP address, but a different name.  Currently, the newly proposed encoding format is configured per subscription (mirroring equivalent transport and encoding configuration), but alternatively it could be configured per receiver.

1. We should consider how a subscription could support multiple subscription paths.  One potential tricky aspect of this is to determine the shared common ancestor path to all the subscriptions.  Related to this is whether XPath 1.0 is the best way of specifying these bind points, or whether it should be modelled as something closer to the NACM node-instance-identifier {{?RFC6536}}, but perhaps using something closer to the JSON style encoding of instance identifier {{?RFC7951}}, section 6.11; or JSON PATH {{?RFC9535}}.

1. What level of subscription filtering do we need and want to support?  For example, I doubt that anyone allows for full XPath filtering of operational data subscriptions because they are likely to be very computationally expensive to implement.  Is there an easier way of expressing the filter requirements rather than using subtree filtering.  Note, this could be added in a future release.

1. Do we need to fold in any text from RFC 8640?

1. Handling lists with separate producers of list entries.

<!--
# Should the appendix contain a list of features in subscriptions notifications and YANG-Push
# that are probably not needed?
#
# E.g., is subscription modified useful (as opposed to just tearing down the subscription and starting again, which clients may handle from a robustness perspective anyway).
-->

# Conventions and Definitions

{::boilerplate bcp14-tagged}

## Definitions taken from Subscribed Notifications (RFC 8639)

- Client: Defined in [RFC8342].

- Configuration: Defined in [RFC8342].

- Configuration datastore: Defined in [RFC8342].

- Configured subscription: A subscription installed via
  configuration into a configuration datastore.

- Dynamic subscription: A subscription created dynamically by a
  subscriber via a Remote Procedure Call (RPC).

- Event: An occurrence of something that may be of interest.
  Examples include a configuration change, a fault, a change in
  status, crossing a threshold, or an external input to the system.

- Event occurrence time: A timestamp matching the time an
  originating process identified as when an event happened.

- Event record: A set of information detailing an event.

- Event stream: A continuous, chronologically ordered set of events
  aggregated under some context.

- Event stream filter: Evaluation criteria that may be applied
  against event records in an event stream.  Event records pass the
  filter when specified criteria are met.

- Notification message: Information intended for a receiver
  indicating that one or more events have occurred.

- Publisher: An entity responsible for streaming notification
  messages per the terms of a subscription.

- Receiver: A target to which a publisher pushes subscribed event
  records.  For dynamic subscriptions, the receiver and subscriber
  are the same entity.

- Subscriber: A client able to request and negotiate a contract for
  the generation and push of event records from a publisher.  For
  dynamic subscriptions, the receiver and subscriber are the same
  entity.

- Subscription: A contract with a publisher, stipulating the
  information that one or more receivers wish to have pushed from
  the publisher without the need for further solicitation.

All YANG tree diagrams used in this document follow the notation
defined in [RFC8340].

## Definitions taken from YANG Push (RFC 8641)

This document uses the terminology defined in [RFC7950], [RFC8341],
[RFC8342], and [RFC8639].  In addition, this document defines the
following terms:

*  Datastore node: A node in the instantiated YANG data tree
  associated with a datastore.  In this document, datastore nodes
  are often also simply referred to as "objects".

*  Datastore node update: A data item containing the current value of
  a datastore node at the time the datastore node update was
  created, as well as the path to the datastore node.

*  Datastore subscription: A subscription to a stream of datastore
  node updates.

*  Datastore subtree: A datastore node and all its descendant
  datastore nodes.

*  On-change subscription: A datastore subscription with updates that
  are triggered when changes in subscribed datastore nodes are
  detected.

*  Periodic subscription: A datastore subscription with updates that
  are triggered periodically according to some time interval.

*  Selection filter: Evaluation and/or selection criteria that may be
  applied against a targeted set of objects.

*  Update record: A representation of one or more datastore node
  updates.  In addition, an update record may contain which type of
  update led to the datastore node update (e.g., whether the
  datastore node was added, changed, or deleted).  Also included in
  the update record may be other metadata, such as a subscription ID
  of the subscription for which the update record was generated.  In
  this document, update records are often also simply referred to as
  "updates".

*  Update trigger: A mechanism that determines when an update record
  needs to be generated.

*  YANG-Push: The subscription and push mechanism for datastore
  updates that is specified in this document.

# YANG Push Lite Solution Overview

This document specifies a solution that provides a subscription service for updates from a datastore.  This solution supports dynamic as well as configured subscriptions to updates of datastore nodes. Subscriptions specify when notification messages (also referred to as *push updates*) should be sent and what data to include in update records.  Datastore node updates are subsequently pushed from the publisher to the receiver per the terms of the subscription.

TODO - The solution here is optimized for streaming data nodes from the operational state datastore {{RFC8342}}.


# Subscribed Notifications Solution Overview (all from RFC 8639)

This document describes a transport-agnostic mechanism for
subscribing to and receiving content from an event stream in a
publisher.  This mechanism operates through the use of a
subscription.

Two types of subscriptions are supported:

1. Dynamic subscriptions, where a subscriber initiates a
    subscription negotiation with a publisher via an RPC.  If the
    publisher is able to serve this request, it accepts it and then
    starts pushing notification messages back to the subscriber.  If
    the publisher is not able to serve it as requested, then an error
    response is returned.  This response MAY include hints for
    subscription parameters that, had they been present, may have
    enabled the dynamic subscription request to be accepted.

2. Configured subscriptions, which allow the management of
    subscriptions via a configuration so that a publisher can send
    notification messages to a receiver.  Support for configured
    subscriptions is optional, with its availability advertised via a
    YANG feature.

Additional characteristics differentiating configured from dynamic
subscriptions include the following:

- The lifetime of a dynamic subscription is bound by the transport
  session used to establish it.  For connection-oriented stateful
  transports like NETCONF, the loss of the transport session will
  result in the immediate termination of any associated dynamic
  subscriptions.  For connectionless or stateless transports like
  HTTP, a lack of receipt acknowledgment of a sequential set of
  notification messages and/or keep-alives can be used to trigger a
  termination of a dynamic subscription.  Contrast this to the
  lifetime of a configured subscription.  This lifetime is driven by
  relevant configuration being present in the publisher's applied
  configuration.  Being tied to configuration operations implies
  that (1) configured subscriptions can be configured to persist
  across reboots and (2) a configured subscription can persist even
  when its publisher is fully disconnected from any network.

- Configured subscriptions can be modified by any configuration
  client with write permission on the configuration of the
  subscription.  Dynamic subscriptions can only be modified via an
  RPC request made by the original subscriber or by a change to
  configuration data referenced by the subscription.

Note that there is no mixing and matching of dynamic and configured
operations on a single subscription.  Specifically, a configured
subscription cannot be modified or deleted using RPCs defined in this
document.  Similarly, a dynamic subscription cannot be directly
modified or deleted by configuration operations.  It is, however,
possible to perform a configuration operation that indirectly impacts
a dynamic subscription.  By changing the value of a preconfigured
filter referenced by an existing dynamic subscription, the selected
event records passed to a receiver might change.

Also note that transport-specific specifications based on this
specification MUST detail the lifecycle of dynamic subscriptions as
well as the lifecycle of configured subscriptions (if supported).

A publisher MAY terminate a dynamic subscription at any time.
Similarly, it MAY decide to temporarily suspend the sending of
notification messages for any dynamic subscription, or for one or
more receivers of a configured subscription.  Such termination or
suspension is driven by internal considerations of the publisher.

## Relationship to RFC 5277

This document is intended to provide a superset of the subscription
capabilities initially defined in [RFC5277].  It is important to
understand what has been reused and what has been replaced,
especially when extending an existing implementation that is based on
[RFC5277].  Key relationships between these two documents include the
following:

- This document defines a transport-independent capability;
  [RFC5277] is specific to NETCONF.

- For the new operations, the data model defined in this document is
  used instead of the data model defined in Section 3.4 of
  [RFC5277].

- The RPC operations in this document replace the operation
  \<create-subscription\> as defined in [RFC5277], Section 4.

- The \<notification\> message of [RFC5277], Section 4 is used.

- The included contents of the "NETCONF" event stream are identical
  between this document and [RFC5277].

- A publisher MAY implement both the Notification Management Schema
  and RPCs defined in [RFC5277] and this document concurrently.

- Unlike [RFC5277], this document enables a single transport session
  to intermix notification messages and RPCs for different
  subscriptions.

- A subscription "stop-time" can be specified as part of a
  notification replay.  This supports a capability analogous to the
  \<stopTime\> parameter of [RFC5277].  However, in this
  specification, a "stop-time" parameter can also be applied without
  replay.

## Solution

Per the overview provided in Section 1.3, this section details the
overall context, state machines, and subsystems that may be assembled
to allow the subscription of events from a publisher.

### Event Streams

An event stream is a named entity on a publisher; this entity exposes
a continuously updating set of YANG-defined event records.  An event
record is an instantiation of a "notification" YANG statement.  If
the "notification" is defined as a child to a data node, the
instantiation includes the hierarchy of nodes that identifies the
data node in the datastore (see Section 7.16.2 of [RFC7950]).  Each
event stream is available for subscription.  Identifying a) how event
streams are defined (other than the NETCONF stream), b) how event
records are defined/generated, and c) how event records are assigned
to event streams is out of scope for this document.

There is only one reserved event stream name in this document:
"NETCONF".  The "NETCONF" event stream contains all NETCONF event
record information supported by the publisher, except where an event
record has explicitly been excluded from the stream.  Beyond the
"NETCONF" stream, implementations MAY define additional event
streams.

As YANG-defined event records are created by a system, they may be
assigned to one or more streams.  The event record is distributed to
a subscription's receiver(s) where (1) a subscription includes the
identified stream and (2) subscription filtering does not exclude the
event record from that receiver.

Access control permissions may be used to silently exclude event
records from an event stream for which the receiver has no read
access.  See [RFC8341], Section 3.4.6 for an example of how this
might be accomplished.  Note that per Section 2.7 of this document,
subscription state change notifications are never filtered out.

If no access control permissions are in place for event records on an
event stream, then a receiver MUST be allowed access to all the event
records.  If subscriber permissions change during the lifecycle of a
subscription and event stream access is no longer permitted, then the
subscription MUST be terminated.

Event records MUST NOT be delivered to a receiver in a different
order than the order in which they were placed on an event stream.

### Event Stream Filters

This document defines an extensible filtering mechanism.  The filter
itself is a boolean test that is placed on the content of an event
record.  A "false" filtering result causes the event record to be
excluded from delivery to a receiver.  A filter never results in
information being stripped from an event record prior to that event
record being encapsulated in a notification message.  The two
optional event stream filtering syntaxes supported are [XPATH] and
subtree [RFC6241].

If no event stream filter is provided in a subscription, all event
records on an event stream are to be sent.

### QoS

This document provides for several Quality of Service (QoS)
parameters.  These parameters indicate the treatment of a
subscription relative to other traffic between publisher and
receiver.  Included are:

- A "dscp" marking to differentiate prioritization of notification
  messages during network transit.

- A "weighting" so that bandwidth proportional to this weighting can
  be allocated to this subscription relative to other subscriptions.

- A "dependency" upon another subscription.

If the publisher supports the "dscp" feature, then a subscription
with a "dscp" leaf MUST result in a corresponding Differentiated
Services Code Point (DSCP) marking [RFC2474] being placed in the IP
header of any resulting notification messages and subscription state
change notifications.  A publisher MUST respect the DSCP markings for
subscription traffic egressing that publisher.

Different DSCP code points require different transport connections.
As a result, where TCP is used, a publisher that supports the "dscp"
feature must ensure that a subscription's notification messages are
returned in a single TCP transport session where all traffic shares
the subscription's "dscp" leaf value.  If this cannot be guaranteed,
any "establish-subscription" RPC request SHOULD be rejected with a
"dscp-unavailable" error.

For the "weighting" parameter, when concurrently dequeuing
notification messages from multiple subscriptions to a receiver, the
publisher MUST allocate bandwidth to each subscription proportional
to the weights assigned to those subscriptions.  "Weighting" is an
optional capability of the publisher; support for it is identified
via the "qos" feature.

If a subscription has the "dependency" parameter set, then any
buffered notification messages containing event records selected by
the parent subscription MUST be dequeued prior to the notification
messages of the dependent subscription.  If notification messages
have dependencies on each other, the notification message queued the
longest MUST go first.  If a "dependency" included in an RPC
references a subscription that does not exist or is no longer
accessible to that subscriber, that "dependency" MUST be silently
removed.  "Dependency" is an optional capability of the publisher;
support for it is identified via the "qos" feature.

"Dependency" and "weighting" parameters will only be respected and
enforced between subscriptions that share the same "dscp" leaf value.

There are additional types of publisher capacity overload that this
specification does not address, as they are out of scope.  For
example, the prioritization of which subscriptions have precedence
when the publisher CPU is overloaded is not discussed.  As a result,
implementation choices will need to be made to address such
considerations.

### Configured Subscriptions

A configured subscription is a subscription installed via
configuration.  Configured subscriptions may be modified by any
configuration client with the proper permissions.  Subscriptions can
be modified or terminated via configuration at any point during their
lifetime.  Multiple configured subscriptions MUST be supportable over
a single transport session.

Configured subscriptions have several characteristics distinguishing
them from dynamic subscriptions:

- persistence across publisher reboots,

- persistence even when transport is unavailable, and

- an ability to send notification messages to more than one
  receiver.  (Note that receivers are unaware of the existence of
  any other receivers.)

On the publisher, support for configured subscriptions is optional
and advertised using the "configured" feature.  On a receiver of a
configured subscription, support for dynamic subscriptions is
optional.  However, if replaying missed event records is required for
a configured subscription, support for dynamic subscription is highly
recommended.  In this case, a separate dynamic subscription can be
established to retransmit the missing event records.

In addition to the subscription parameters available to dynamic
subscriptions as described in Section 2.4.2, the following additional
parameters are also available to configured subscriptions:

- A "transport", which identifies the transport protocol to use to
  connect with all subscription receivers.

- One or more receivers, each intended as the destination for event
  records.  Note that each individual receiver is identifiable by
  its "name".

- Optional parameters to identify where traffic should egress a
  publisher:

  - A "source-interface", which identifies the egress interface to
      use from the publisher.  Publisher support for this parameter
      is optional and advertised using the "interface-designation"
      feature.

  - A "source-address" address, which identifies the IP address to
      stamp on notification messages destined for the receiver.

  - A "source-vrf", which identifies the Virtual Routing and
      Forwarding (VRF) instance on which to reach receivers.  This
      VRF is a network instance as defined in [RFC8529].  Publisher
      support for VRFs is optional and advertised using the
      "supports-vrf" feature.

  If none of the above parameters are set, notification messages
  MUST egress the publisher's default interface.

A tree diagram that includes these parameters is provided in
Figure 20 in Section 3.3.  These parameters are described in the YANG
module in Section 4.

#### Configured Subscription State Machine

Below is the state machine for a configured subscription on the
publisher.  This state machine describes the three states ("valid",
"invalid", and "concluded") as well as the transitions between these
states.  Start and end states are depicted to reflect configured
subscription creation and deletion events.  The creation or
modification of a configured subscription initiates an evaluation by
the publisher to determine if the subscription is in the
"valid" state or the "invalid" state.  The publisher uses its own
criteria in making this determination.  If in the "valid" state, the
subscription becomes operational.  See (1) in the diagram below.

~~~~~~~~~~
 .........
 : start :-.
 :.......: |
      create  .---modify-----.----------------------------------.
           |  |              |                                  |
           V  V          .-------.         .......         .---------.
  .----[evaluate]--no--->|invalid|-delete->: end :<-delete-|concluded|
  |                      '-------'         :.....:         '---------'
  |-[evaluate]--no-(2).      ^                ^                 ^
  |        ^          |      |                |                 |
 yes       |          '->unsupportable      delete           stop-time
  |      modify         (subscription-   (subscription-   (subscription-
  |        |             terminated*)     terminated*)      concluded*)
  |        |                 |                |                 |
 (1)       |                (3)              (4)               (5)
  |   .---------------------------------------------------------------.
  '-->|                         valid                                 |
      '---------------------------------------------------------------'


 Legend:
   Dotted boxes: subscription added or removed via configuration
   Dashed boxes: states for a subscription
   [evaluate]: decision point on whether the subscription
               is supportable
   (*): resulting subscription state change notification

     Figure 8: Publisher's State Machine for a Configured Subscription
~~~~~~~~~~


A subscription in the "valid" state may move to the "invalid" state
in one of two ways.  First, it may be modified in a way that fails a
re-evaluation.  See (2) in the diagram.  Second, the publisher might
determine that the subscription is no longer supportable.  This could
be because of an unexpected but sustained increase in an event
stream's event records, degraded CPU capacity, a more complex
referenced filter, or other subscriptions that have usurped
resources.  See (3) in the diagram.  No matter the case, a
"subscription-terminated" notification is sent to any receivers in
the "active" or "suspended" state.  A subscription in the
"valid" state may also transition to the "concluded" state via (5) if
a configured stop time has been reached.  In this case, a
"subscription-concluded" notification is sent to any receivers in the
"active" or "suspended" state.  Finally, a subscription may be
deleted by configuration (4).

When a subscription is in the "valid" state, a publisher will attempt
to connect with all receivers of a configured subscription and
deliver notification messages.  Below is the state machine for each
receiver of a configured subscription.  This receiver state machine
is fully contained in the state machine of the configured
subscription and is only relevant when the configured subscription is
in the "valid" state.

~~~~~~~~~~
     .-----------------------------------------------------------------.
     |                         valid                                   |
     |   .----------.                           .------------.         |
     |   | receiver |---timeout---------------->|  receiver  |         |
     |   |connecting|<----------------reset--(c)|disconnected|         |
     |   |          |<-transport                '------------'         |
     |   '----------'  loss,reset------------------------------.       |
     |      (a)          |                                     |       |
     |  subscription-   (b)                                   (b)      |
     |  started*    .--------.                             .---------. |
     |       '----->|        |(d)-insufficient CPU,------->|         | |
     |              |receiver|    buffer overflow          |receiver | |
     | subscription-| active |                             |suspended| |
     |   modified*  |        |<----CPU, b/w sufficient,-(e)|         | |
     |        '---->'--------'     subscription-modified*  '---------' |
     '-----------------------------------------------------------------'

     Legend:
       Dashed boxes that include the word "receiver" show the possible
       states for an individual receiver of a valid configured
       subscription.

      * indicates a subscription state change notification

      Figure 9: Receiver State Machine for a Configured Subscription
                              on a Publisher
~~~~~~~~~~

When a configured subscription first moves to the "valid" state, the
"state" leaf of each receiver is initialized to the "connecting"
state.  If transport connectivity is not available to any receivers
and there are any notification messages to deliver, a transport
session is established (e.g., per [RFC8071]).  Individual receivers
are moved to the "active" state when a "subscription-started"
subscription state change notification is successfully passed to that
receiver (a).  Event records are only sent to active receivers.
Receivers of a configured subscription remain active on the publisher
if both (1) transport connectivity to the receiver is active and
(2) event records are not being dropped due to a publisher's sending
capacity being reached.  In addition, a configured subscription's
receiver MUST be moved to the "connecting" state if the receiver is
reset via the "reset" action (b), (c).  For more on the "reset"
action, see Section 2.5.5.  If transport connectivity cannot be
achieved while in the "connecting" state, the receiver MAY be moved
to the "disconnected" state.

A configured subscription's receiver MUST be moved to the "suspended"
state if there is transport connectivity between the publisher and
receiver but (1) delivery of notification messages is failing due to
a publisher's buffer capacity being reached or (2) notification
messages cannot be generated for that receiver due to insufficient
CPU (d).  This is indicated to the receiver by the "subscription-
suspended" subscription state change notification.

A configured subscription's receiver MUST be returned to the "active"
state from the "suspended" state when notification messages can be
generated, bandwidth is sufficient to handle the notification
messages, and a receiver has successfully been sent a "subscription-
resumed" or "subscription-modified" subscription state change
notification (e).  The choice as to which of these two subscription
state change notifications is sent is determined by whether the
subscription was modified during the period of suspension.

Modification of a configured subscription is possible at any time.  A
"subscription-modified" subscription state change notification will
be sent to all active receivers, immediately followed by notification
messages conforming to the new parameters.  Suspended receivers will
also be informed of the modification.  However, this notification
will await the end of the suspension for that receiver (e).

The mechanisms described above are mirrored in the RPCs and
notifications defined in this document.  It should be noted that
these RPCs and notifications have been designed to be extensible and
allow subscriptions into targets other than event streams.  For
instance, the YANG module defined in Section 5 of [RFC8641] augments
"/sn:modify-subscription/sn:input/sn:target".

#### Creating a Configured Subscription

Configured subscriptions are established using configuration
operations against the top-level "subscriptions" subtree.

Because there is no explicit association with an existing transport
session, configuration operations MUST include additional parameters
beyond those of dynamic subscriptions.  These parameters identify
each receiver, how to connect with that receiver, and possibly
whether the notification messages need to come from a specific egress
interface on the publisher.  Receiver-specific transport connectivity
parameters MUST be configured via transport-specific augmentations to
this specification.  See Section 2.5.7 for details.

After a subscription is successfully established, the publisher
immediately sends a "subscription-started" subscription state change
notification to each receiver.  It is quite possible that upon
configuration, reboot, or even steady-state operations, a transport
session may not be currently available to the receiver.  In this
case, when there is something to transport for an active
subscription, transport-specific "call home" operations [RFC8071]
will be used to establish the connection.  When transport
connectivity is available, notification messages may then be pushed.

With active configured subscriptions, it is allowable to buffer event
records even after a "subscription-started" has been sent.  However,
if events are lost (rather than just delayed) due to replay buffer
capacity being reached, a new "subscription-started" must be sent.
This new "subscription-started" indicates an event record
discontinuity.

To see an example of subscription creation using configuration
operations over NETCONF, see Appendix A.

#### Modifying a Configured Subscription

Configured subscriptions can be modified using configuration
operations against the top-level "subscriptions" subtree.

If the modification involves adding receivers, added receivers are
placed in the "connecting" state.  If a receiver is removed, the
subscription state change notification "subscription-terminated" is
sent to that receiver if that receiver is active or suspended.

If the modification involves changing the policies for the
subscription, the publisher sends to currently active receivers a
"subscription-modified" notification.  For any suspended receivers, a
"subscription-modified" notification will be delayed until the
receiver's subscription has been resumed.  (Note: In this case, the
"subscription-modified" notification informs the receiver that the
subscription has been resumed, so no additional "subscription-
resumed" need be sent.  Also note that if multiple modifications have
occurred during the suspension, only the "subscription-modified"
notification describing the latest one need be sent to the receiver.)

#### Deleting a Configured Subscription

Subscriptions can be deleted through configuration against the
top-level "subscriptions" subtree.

Immediately after a subscription is successfully deleted, the
publisher sends to all receivers of that subscription a subscription
state change notification stating that the subscription has ended
(i.e., "subscription-terminated").

####  Resetting a Configured Subscription's Receiver

It is possible that a configured subscription to a receiver needs to
be reset.  This is accomplished via the "reset" action in the YANG
module at "/subscriptions/subscription/receivers/receiver/reset".
This action may be useful in cases where a publisher has timed out
trying to reach a receiver.  When such a reset occurs, a transport
session will be initiated if necessary, and a new "subscription-
started" notification will be sent.  This action does not have any
effect on transport connectivity if the needed connectivity already
exists.

#### Replay for a Configured Subscription

It is possible to do replay on a configured subscription.  This is
supported via the configuration of the "configured-replay" object on
the subscription.  The setting of this object enables the streaming
of the buffered event records for the subscribed event stream.  All
buffered event records that have been retained since the last
publisher restart will be sent to each configured receiver.

Replay of event records created since restart is useful.  It allows
event records generated before transport connectivity establishment
to be passed to a receiver.  Setting the restart time as the earliest
configured replay time precludes the possibility of resending event
records that were logged prior to publisher restart.  It also ensures
that the same records will be sent to each configured receiver,
regardless of the speed of transport connectivity establishment to
each receiver.  Finally, by establishing restart as the earliest
potential time for event records to be included in notification
messages, a well-understood timeframe for replay is defined.

As a result, when any configured subscription's receivers become
active, buffered event records will be sent immediately after the
"subscription-started" notification.  If the publisher knows the last
event record sent to a receiver and the publisher has not rebooted,
the next event record on the event stream that meets filtering
criteria will be the leading event record sent.  Otherwise, the
leading event record will be the first event record meeting filtering
criteria subsequent to the latest of three different times: the
"replay-log-creation-time", the "replay-log-aged-time", or the most
recent publisher boot time.  The "replay-log-creation-time" and
"replay-log-aged-time" are discussed in Section 2.4.2.1.  The most
recent publisher boot time ensures that duplicate event records are
not replayed from a previous time the publisher was booted.

It is quite possible that a receiver might want to retrieve event
records from an event stream prior to the latest boot.  If such
records exist where there is a configured replay, the publisher MUST
send the time of the event record immediately preceding the
"replay-start-time" in the "replay-previous-event-time" leaf.
Through the existence of the "replay-previous-event-time", the
receiver will know that earlier events prior to reboot exist.  In
addition, if the subscriber was previously receiving event records
with the same subscription "id", the receiver can determine if there
was a time gap where records generated on the publisher were not
successfully received.  And with this information, the receiver may
choose to dynamically subscribe to retrieve any event records placed
in the event stream before the most recent boot time.

All other replay functionality remains the same as with dynamic
subscriptions as described in Section 2.4.2.1.

#### Transport Connectivity for a Configured Subscription

This specification is transport independent.  However, supporting a
configured subscription will often require the establishment of
transport connectivity.  And the parameters used for this transport
connectivity establishment are transport specific.  As a result, the
YANG module defined in Section 4 is not able to directly define and
expose these transport parameters.

It is necessary for an implementation to support the connection
establishment process.  To support this function, the YANG data model
defined in this document includes a node where transport-specific
parameters for a particular receiver may be augmented.  This node is
"/subscriptions/subscription/receivers/receiver".  By augmenting
transport parameters from this node, system developers are able to
incorporate the YANG objects necessary to support the transport
connectivity establishment process.

The result of this is the following requirement.  A publisher
supporting the feature "configured" MUST also support at least one
YANG data model that augments transport connectivity parameters on
"/subscriptions/subscription/receivers/receiver".  For an example of
such an augmentation, see Appendix A.

### Dynamic Subscriptions

Dynamic subscriptions are managed via protocol operations (in the
form of RPCs, per [RFC7950], Section 7.14) made against targets
located in the publisher.  These RPCs have been designed extensibly
so that they may be augmented for subscription targets beyond event
streams.  For examples of such augmentations, see the RPC
augmentations in the YANG data model provided in [RFC8641].

#### Dynamic Subscription State Machine

Below is the publisher's state machine for a dynamic subscription.
Each state is shown in its own box.  It is important to note that
such a subscription doesn't exist at the publisher until an
"establish-subscription" RPC is accepted.  The mere request by a
subscriber to establish a subscription is not sufficient for that
subscription to be externally visible.  Start and end states are
depicted to reflect subscription creation and deletion events.

~~~~~~~~~~
                  .........
                  : start :
                  :.......:
                      |
              establish-subscription
                      |
                      |   .-------modify-subscription--------.
                      v   v                                  |
                .-----------.                          .-----------.
      .--------. | receiver  |--insufficient CPU, b/w-->| receiver  |
  modify-       '|  active   |                          | suspended |
  subscription   |           |<----CPU, b/w sufficient--|           |
      ---------->'-----------'                          '-----------'
                      |                                      |
            delete/kill-subscription                     delete/kill-
                      |                                 subscription
                      v                                      |
                  .........                                  |
                  :  end  :<---------------------------------'
                  :.......:

  Figure 1: Publisher's State Machine for a Dynamic Subscription
~~~~~~~~~~

Of interest in this state machine are the following:

o  Successful "establish-subscription" or "modify-subscription" RPCs
  move the subscription to the "active" state.

o  Failed "modify-subscription" RPCs will leave the subscription in
  its previous state, with no visible change to any streaming
  updates.

o  A "delete-subscription" or "kill-subscription" RPC will end the
  subscription, as will reaching a "stop-time".

o  A publisher may choose to suspend a subscription when there is not
  sufficient CPU or bandwidth available to service the subscription.
  This is announced to the subscriber via the "subscription-
  suspended" subscription state change notification.

o  A suspended subscription may be modified by the subscriber (for
  example, in an attempt to use fewer resources).  Successful
  modification returns the subscription to the "active" state.

o  Even without a "modify-subscription" request, a publisher may
  return a subscription to the "active" state when sufficient
  resources are again available.  This is announced to the
  subscriber via the "subscription-resumed" subscription state
  change notification.

#### Establishing a Dynamic Subscription

The "establish-subscription" RPC allows a subscriber to request the
creation of a subscription.

The input parameters of the operation are:

o  A "stream" name, which identifies the targeted event stream
  against which the subscription is applied.

o  An event stream filter, which may reduce the set of event records
  pushed.

o  If the transport used by the RPC supports multiple encodings, an
  optional "encoding" for the event records pushed.  If no
  "encoding" is included, the encoding of the RPC MUST be used.

o  An optional "stop-time" for the subscription.  If no "stop-time"
  is present, notification messages will continue to be sent until
  the subscription is terminated.

o  An optional "replay-start-time" for the subscription.  The
  "replay-start-time" MUST be in the past and indicates that the
  subscription is requesting a replay of previously generated
  information from the event stream.  For more on replay, see
  Section 2.4.2.1.  If there is no "replay-start-time", the
  subscription starts immediately.

If the publisher can satisfy the "establish-subscription" request, it
replies with an identifier for the subscription and then immediately
starts streaming notification messages.

Below is a tree diagram for "establish-subscription".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---x establish-subscription
      +---w input
      |  +---w (target)
      |  |  +--:(stream)
      |  |     +---w (stream-filter)?
      |  |     |  +--:(by-reference)
      |  |     |  |  +---w stream-filter-name
      |  |     |  |          stream-filter-ref
      |  |     |  +--:(within-subscription)
      |  |     |     +---w (filter-spec)?
      |  |     |        +--:(stream-subtree-filter)
      |  |     |        |  +---w stream-subtree-filter?   <anydata>
      |  |     |        |          {subtree}?
      |  |     |        +--:(stream-xpath-filter)
      |  |     |           +---w stream-xpath-filter?
      |  |     |                   yang:xpath1.0 {xpath}?
      |  |     +---w stream                               stream-ref
      |  |     +---w replay-start-time?
      |  |             yang:date-and-time {replay}?
      |  +---w stop-time?
      |  |       yang:date-and-time
      |  +---w dscp?                                      inet:dscp
      |  |       {dscp}?
      |  +---w weighting?                                 uint8
      |  |       {qos}?
      |  +---w dependency?
      |  |       subscription-id {qos}?
      |  +---w encoding?                                  encoding
      +--ro output
          +--ro id                            subscription-id
          +--ro replay-start-time-revision?   yang:date-and-time
                  {replay}?

        Figure 2: "establish-subscription" RPC Tree Diagram
~~~~~~~~~~

A publisher MAY reject the "establish-subscription" RPC for many
reasons, as described in Section 2.4.6.  The contents of the
resulting RPC error response MAY include details on input parameters
that, if considered in a subsequent "establish-subscription" RPC, may
result in successful subscription establishment.  Any such hints MUST
be transported in a yang-data "establish-subscription-stream-error-
info" container included in the RPC error response.

Below is a tree diagram for "establish-subscription-stream-error-
info" RPC yang-data.  All objects contained in this tree are
described in the YANG module in Section 4.

~~~~~~~~~~
    yang-data establish-subscription-stream-error-info
      +--ro establish-subscription-stream-error-info
        +--ro reason?                   identityref
        +--ro filter-failure-hint?      string

        Figure 3: "establish-subscription-stream-error-info"
                    RPC yang-data Tree Diagram
~~~~~~~~~~

##### Requesting a Replay of Event Records

Replay provides the ability to establish a subscription that is also
capable of passing event records generated in the recent past.  In
other words, as the subscription initializes itself, it sends any
event records in the target event stream that meet the filter
criteria that have an event time that is after the "replay-start-
time" and also have an event time before the "stop-time" should this
"stop-time" exist.  The end of these historical event records is
identified via a "replay-completed" subscription state change
notification.  Any event records generated since the subscription
establishment may then follow.  For a particular subscription, all
event records will be delivered in the order in which they are placed
in the event stream.

Replay is an optional feature that is dependent on an event stream
supporting some form of logging.  This document puts no restrictions
on the size or form of the log, where it resides in the publisher, or
when event record entries in the log are purged.

The inclusion of a "replay-start-time" in an "establish-subscription"
RPC indicates a replay request.  If the "replay-start-time" contains
a value that is earlier than what a publisher's retained history
supports, then if the subscription is accepted, the actual
publisher's revised start time MUST be set in the returned
"replay-start-time-revision" object.

A "stop-time" parameter may be included in a replay subscription.
For a replay subscription, the "stop-time" MAY be earlier than the
current time but MUST be later than the "replay-start-time".

If the given "replay-start-time" is later than the time marked in any
event records retained in the replay buffer, then the publisher MUST
send a "replay-completed" notification immediately after a successful
"establish-subscription" RPC response.

If an event stream supports replay, the "replay-support" leaf is
present in the "/streams/stream" list entry for the event stream.  An
event stream that does support replay is not expected to have an
unlimited supply of saved notifications available to accommodate any
given replay request.  To assess the timeframe available for replay,
subscribers can read the leafs "replay-log-creation-time" and
"replay-log-aged-time".  See Figure 18 for the YANG tree and
Section 4 for the YANG module describing these elements.  The actual
size of the replay log at any given time is a publisher-specific
matter.  Control parameters for the replay log are outside the scope
of this document.

#### Modifying a Dynamic Subscription

The "modify-subscription" operation permits changing the terms of an
existing dynamic subscription.  Dynamic subscriptions can be modified
any number of times.  Dynamic subscriptions can only be modified via
this RPC using a transport session connecting to the subscriber.  If
the publisher accepts the requested modifications, it acknowledges
success to the subscriber, then immediately starts sending event
records based on the new terms.

Subscriptions created by configuration cannot be modified via this
RPC.  However, configuration may be used to modify objects referenced
by the subscription (such as a referenced filter).

Below is a tree diagram for "modify-subscription".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---x modify-subscription
      +---w input
          +---w id
          |       subscription-id
          +---w (target)
          |  +--:(stream)
          |     +---w (stream-filter)?
          |        +--:(by-reference)
          |        |  +---w stream-filter-name
          |        |          stream-filter-ref
          |        +--:(within-subscription)
          |           +---w (filter-spec)?
          |              +--:(stream-subtree-filter)
          |              |  +---w stream-subtree-filter?   <anydata>
          |              |          {subtree}?
          |              +--:(stream-xpath-filter)
          |                 +---w stream-xpath-filter?
          |                         yang:xpath1.0 {xpath}?
          +---w stop-time?
                  yang:date-and-time

          Figure 4: "modify-subscription" RPC Tree Diagram
~~~~~~~~~~

If the publisher accepts the requested modifications on a currently
suspended subscription, the subscription will immediately be resumed
(i.e., the modified subscription is returned to the "active" state).
The publisher MAY immediately suspend this newly modified
subscription through the "subscription-suspended" notification before
any event records are sent.

If the publisher rejects the RPC request, the subscription remains as
it was prior to the request.  That is, the request has no impact
whatsoever.  Rejection of the RPC for any reason is indicated via an
RPC error as described in Section 2.4.6.  The contents of such a
rejected RPC MAY include hints on inputs that (if considered) may
result in a successfully modified subscription.  These hints MUST be
transported in a yang-data "modify-subscription-stream-error-info"
container inserted into the RPC error response.

Below is a tree diagram for "modify-subscription-stream-error-info"
RPC yang-data.  All objects contained in this tree are described in
the YANG module in Section 4.

~~~~~~~~~~
    yang-data modify-subscription-stream-error-info
      +--ro modify-subscription-stream-error-info
        +--ro reason?                identityref
        +--ro filter-failure-hint?   string

          Figure 5: "modify-subscription-stream-error-info"
                    RPC yang-data Tree Diagram
~~~~~~~~~~

#### Deleting a Dynamic Subscription

The "delete-subscription" operation permits canceling an existing
subscription.  If the publisher accepts the request and the publisher
has indicated success, the publisher MUST NOT send any more
notification messages for this subscription.

Below is a tree diagram for "delete-subscription".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---x delete-subscription
      +---w input
          +---w id     subscription-id

          Figure 6: "delete-subscription" RPC Tree Diagram
~~~~~~~~~~

Dynamic subscriptions can only be deleted via this RPC using a
transport session connecting to the subscriber.  Configured
subscriptions cannot be deleted using RPCs.

#### Killing a Dynamic Subscription

The "kill-subscription" operation permits an operator to end a
dynamic subscription that is not associated with the transport
session used for the RPC.  A publisher MUST terminate any dynamic
subscription identified by the "id" parameter in the RPC request, if
such a subscription exists.

Configured subscriptions cannot be killed using this RPC.  Instead,
configured subscriptions are deleted as part of regular configuration
operations.  Publishers MUST reject any RPC attempt to kill a
configured subscription.

Below is a tree diagram for "kill-subscription".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---x kill-subscription
      +---w input
          +---w id     subscription-id

          Figure 7: "kill-subscription" RPC Tree Diagram
~~~~~~~~~~

#### RPC Failures

Whenever an RPC is unsuccessful, the publisher returns relevant
information as part of the RPC error response.  Transport-level error
processing MUST be done before the RPC error processing described in
this section.  In all cases, RPC error information returned by the
publisher will use existing transport-layer RPC structures, such as
those seen with NETCONF (Appendix A of [RFC6241]) or RESTCONF
(Section 7.1 of [RFC8040]).  These structures MUST be able to encode
subscription-specific errors identified below and defined in this
document's YANG data model.

As a result of this variety, how subscription errors are encoded in
an RPC error response is transport dependent.  Valid errors that can
occur for each RPC are as follows:

~~~~~~~~~~
  establish-subscription         modify-subscription
  ----------------------         ----------------------
  dscp-unavailable               filter-unsupported
  encoding-unsupported           insufficient-resources
  filter-unsupported             no-such-subscription
  insufficient-resources
  replay-unsupported

  delete-subscription            kill-subscription
  ----------------------         ----------------------
  no-such-subscription           no-such-subscription
~~~~~~~~~~

To see a NETCONF-based example of an error response from the list
above, see the "no-such-subscription" error response illustrated in
[RFC8640], Figure 10.

There is one final set of transport-independent RPC error elements
included in the YANG data model defined in this document: three
yang-data structures that enable the publisher to provide to the
receiver any error information that does not fit into existing
transport-layer RPC structures.  These structures are:

1. "establish-subscription-stream-error-info": This MUST be returned
    with the leaf "reason" populated if an RPC error reason has not
    been placed elsewhere in the transport portion of a failed
    "establish-subscription" RPC response.  This MUST be sent if
    hints on how to overcome the RPC error are included.

2. "modify-subscription-stream-error-info": This MUST be returned
    with the leaf "reason" populated if an RPC error reason has not
    been placed elsewhere in the transport portion of a failed
    "modify-subscription" RPC response.  This MUST be sent if hints
    on how to overcome the RPC error are included.

3. "delete-subscription-error-info": This MUST be returned with the
    leaf "reason" populated if an RPC error reason has not been
    placed elsewhere in the transport portion of a failed
    "delete-subscription" or "kill-subscription" RPC response.

### Event Record Delivery

Whether dynamic or configured, once a subscription has been set up,
the publisher streams event records via notification messages per the
terms of the subscription.  For dynamic subscriptions, notification
messages are sent over the session used to establish the
subscription.  For configured subscriptions, notification messages
are sent over the connections specified by the transport and each
receiver of a configured subscription.

A notification message is sent to a receiver when an event record is
not blocked by either the specified filter criteria or receiver
permissions.  This notification message MUST include an \<eventTime\>
object, as shown in [RFC5277], Section 4.  This \<eventTime\> MUST be
at the top level of a YANG structured event record.

The following example of XML [W3C.REC-xml-20081126], adapted from
Section 4.2.10 of [RFC7950], illustrates a compliant message:

~~~~~~~~~~
  <notification
          xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
      <eventTime>2007-09-01T10:00:00Z</eventTime>
      <link-failure xmlns="https://acme.example.com/system">
          <if-name>so-1/2/3.0</if-name>
          <if-admin-status>up</if-admin-status>
          <if-oper-status>down</if-oper-status>
      </link-failure>
  </notification>

            Figure 10: Subscribed Notification Message
~~~~~~~~~~

[RFC5277], Section 2.2.1 states that a notification message is to be
sent to a subscriber that initiated a \<create-subscription\>.  With
this document, this statement from [RFC5277] should be more broadly
interpreted to mean that notification messages can also be sent to a
subscriber that initiated an "establish-subscription" or to a
configured receiver that has been sent a "subscription-started".

When a dynamic subscription has been started or modified with
"establish-subscription" or "modify-subscription", respectively,
event records matching the newly applied filter criteria MUST NOT be
sent until after the RPC reply has been sent.

When a configured subscription has been started or modified, event
records matching the newly applied filter criteria MUST NOT be sent
until after the "subscription-started" or "subscription-modified"
notification has been sent, respectively.

### Subscription State Change Notifications

In addition to sending event records to receivers, a publisher MUST
also send subscription state change notifications when events related
to subscription management have occurred.

Subscription state change notifications are unlike other
notifications in that they are never included in any event stream.
Instead, they are inserted (as defined in this section) into the
sequence of notification messages sent to a particular receiver.
Subscription state change notifications cannot be dropped or filtered
out, they cannot be stored in replay buffers, and they are delivered
only to impacted receivers of a subscription.  The identification of
subscription state change notifications is easy to separate from
other notification messages through the use of the YANG extension
"subscription-state-notif".  This extension tags a notification as a
subscription state change notification.

The complete set of subscription state change notifications is
described in the following subsections.

#### "subscription-started"

This notification indicates that a configured subscription has
started, and event records may be sent.  Included in this
subscription state change notification are all the parameters of the
subscription, except for (1) transport connection information for one
or more receivers and (2) origin information indicating where
notification messages will egress the publisher.  Note that if a
referenced filter from the "filters" container has been used in the
subscription, the notification still provides the contents of that
referenced filter under the "within-subscription" subtree.

Note that for dynamic subscriptions, no "subscription-started"
notifications are ever sent.

Below is a tree diagram for "subscription-started".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n subscription-started {configured}?
      +--ro id
      |       subscription-id
      +--ro (target)
      |  +--:(stream)
      |     +--ro (stream-filter)?
      |     |  +--:(by-reference)
      |     |  |  +--ro stream-filter-name
      |     |  |          stream-filter-ref
      |     |  +--:(within-subscription)
      |     |     +--ro (filter-spec)?
      |     |        +--:(stream-subtree-filter)
      |     |        |  +--ro stream-subtree-filter?   <anydata>
      |     |        |          {subtree}?
      |     |        +--:(stream-xpath-filter)
      |     |           +--ro stream-xpath-filter?     yang:xpath1.0
      |     |                   {xpath}?
      |     +--ro stream                               stream-ref
      |     +--ro replay-start-time?
      |     |       yang:date-and-time {replay}?
      |     +--ro replay-previous-event-time?
      |             yang:date-and-time {replay}?
      +--ro stop-time?
      |       yang:date-and-time
      +--ro dscp?                                      inet:dscp
      |       {dscp}?
      +--ro weighting?                                 uint8 {qos}?
      +--ro dependency?
      |       subscription-id {qos}?
      +--ro transport?                                 transport
      |       {configured}?
      +--ro encoding?                                  encoding
      +--ro purpose?                                   string
              {configured}?

    Figure 11: "subscription-started" Notification Tree Diagram
~~~~~~~~~~

####  "subscription-modified"

This notification indicates that a subscription has been modified by
configuration operations.  It is delivered directly after the last
event records processed using the previous subscription parameters,
and before any event records processed after the modification.

Below is a tree diagram for "subscription-modified".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n subscription-modified
      +--ro id
      |       subscription-id
      +--ro (target)
      |  +--:(stream)
      |     +--ro (stream-filter)?
      |     |  +--:(by-reference)
      |     |  |  +--ro stream-filter-name
      |     |  |          stream-filter-ref
      |     |  +--:(within-subscription)
      |     |     +--ro (filter-spec)?
      |     |        +--:(stream-subtree-filter)
      |     |        |  +--ro stream-subtree-filter?   <anydata>
      |     |        |          {subtree}?
      |     |        +--:(stream-xpath-filter)
      |     |           +--ro stream-xpath-filter?     yang:xpath1.0
      |     |                   {xpath}?
      |     +--ro stream                               stream-ref
      |     +--ro replay-start-time?
      |             yang:date-and-time {replay}?
      +--ro stop-time?
      |       yang:date-and-time
      +--ro dscp?                                      inet:dscp
      |       {dscp}?
      +--ro weighting?                                 uint8 {qos}?
      +--ro dependency?
      |       subscription-id {qos}?
      +--ro transport?                                 transport
      |       {configured}?
      +--ro encoding?                                  encoding
      +--ro purpose?                                   string
              {configured}?

    Figure 12: "subscription-modified" Notification Tree Diagram
~~~~~~~~~~

A publisher most often sends this notification directly after the
modification of any configuration parameters impacting a configured
subscription.  But it may also be sent at two other times:

1. If a configured subscription has been modified during the
    suspension of a receiver, the notification will be delayed until
    the receiver's suspension is lifted.  In this situation, the
    notification indicates that the subscription has been both
    modified and resumed.

2. A "subscription-modified" subscription state change notification
    MUST be sent if the contents of the filter identified by the
    subscription's "stream-filter-ref" leaf have changed.  This state
    change notification is to be sent for a filter change impacting
    any active receivers of a configured or dynamic subscription.

####  "subscription-terminated"

This notification indicates that no further event records for this
subscription should be expected from the publisher.  A publisher may
terminate the sending of event records to a receiver for the
following reasons:

1. Configuration that removes a configured subscription, or a
    "kill-subscription" RPC that ends a dynamic subscription.  These
    are identified via the reason "no-such-subscription".

2. A referenced filter is no longer accessible.  This reason is
    identified by the "filter-unavailable" identity.

3. The event stream referenced by a subscription is no longer
    accessible by the receiver.  This reason is identified by the
    "stream-unavailable" identity.

4. A suspended subscription has exceeded some timeout.  This reason
    is identified by the "suspension-timeout" identity.

Each reason listed above derives from the "subscription-terminated-
reason" base identity specified in the YANG data model in this
document.

Below is a tree diagram for "subscription-terminated".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n subscription-terminated
      +--ro id        subscription-id
      +--ro reason    identityref

  Figure 13: "subscription-terminated" Notification Tree Diagram
~~~~~~~~~~

Note: This subscription state change notification MUST be sent to a
dynamic subscription's receiver when the subscription ends
unexpectedly.  This might happen when a "kill-subscription" RPC is
successful or when some other event, not including reaching the
subscription's "stop-time", results in a publisher choosing to end
the subscription.

#### "subscription-suspended"

This notification indicates that a publisher has suspended the
sending of event records to a receiver and also indicates the
possible loss of events.  Suspension happens when capacity
constraints stop a publisher from serving a valid subscription.  The
two conditions where this is possible are:

1. "insufficient-resources", when a publisher is unable to produce
    the requested event stream of notification messages, and

2. "unsupportable-volume", when the bandwidth needed to get
    generated notification messages to a receiver exceeds a
    threshold.

These conditions are encoded in the "reason" object.  No further
notifications will be sent until the subscription resumes or is
terminated.

Below is a tree diagram for "subscription-suspended".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n subscription-suspended
      +--ro id        subscription-id
      +--ro reason    identityref

    Figure 14: "subscription-suspended" Notification Tree Diagram
~~~~~~~~~~

#### "subscription-resumed"

This notification indicates that a previously suspended subscription
has been resumed under the unmodified terms previously in place.
Subscribed event records generated after the issuance of this
subscription state change notification may now be sent.

Below is a tree diagram for "subscription-resumed".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n subscription-resumed
      +--ro id    subscription-id

    Figure 15: "subscription-resumed" Notification Tree Diagram
~~~~~~~~~~

#### "subscription-completed"

This notification indicates that a subscription that includes a
"stop-time" has successfully finished passing event records upon
reaching that time.

Below is a tree diagram for "subscription-completed".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n subscription-completed {configured}?
      +--ro id    subscription-id

    Figure 16: "subscription-completed" Notification Tree Diagram
~~~~~~~~~~

####  "replay-completed"

This notification indicates that all of the event records prior to
the current time have been passed to a receiver.  It is sent before
any notification messages containing an event record with a timestamp
later than (1) the "stop-time" or (2) the subscription's start time.

If a subscription does not contain a "stop-time" or has a "stop-time"
that has not been reached, then after the "replay-completed"
notification has been sent, additional event records will be sent in
sequence as they arise naturally on the publisher.

Below is a tree diagram for "replay-completed".  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
    +---n replay-completed {replay}?
      +--ro id    subscription-id

      Figure 17: "replay-completed" Notification Tree Diagram
~~~~~~~~~~

### Subscription Monitoring

In the operational state datastore, the "subscriptions" container
maintains the state of all dynamic subscriptions as well as all
configured subscriptions.  Using datastore retrieval operations
[RFC8641] or subscribing to the "subscriptions" container
(Section 3.3) allows the state of subscriptions and their
connectivity to receivers to be monitored.

Each subscription in the operational state datastore is represented
as a list element.  Included in this list are event counters for each
receiver, the state of each receiver, and the subscription parameters
currently in effect.  The appearance of the leaf "configured-
subscription-state" indicates that a particular subscription came

into being via configuration.  This leaf also indicates whether the
current state of that subscription is "valid", "invalid", or
"concluded".

To understand the flow of event records in a subscription, there are
two counters available for each receiver.  The first counter is
"sent-event-records", which shows the number of events identified for
sending to a receiver.  The second counter is "excluded-event-
records", which shows the number of event records not sent to a
receiver.  "excluded-event-records" shows the combined results of
both access control and per-subscription filtering.  For configured
subscriptions, counters are reset whenever the subscription's state
is evaluated as "valid" (see (1) in Figure 8).

Dynamic subscriptions are removed from the operational state
datastore once they expire (reaching "stop-time") or when they are
terminated.  While many subscription objects are shown as
configurable, dynamic subscriptions are only included in the
operational state datastore and as a result are not configurable.

### Support for the "ietf-subscribed-notifications" YANG Module

Publishers supporting this document MUST indicate support of the YANG
module "ietf-subscribed-notifications" in the YANG library of the
publisher.  In addition, if supported, the optional features
"encode-xml", "encode-json", "configured", "supports-vrf", "qos",
"xpath", "subtree", "interface-designation", "dscp", and "replay"
MUST be indicated.

# YANG Push Solution Overview (all from RFC 8641)

This document specifies a solution that provides a subscription
service for updates from a datastore.  This solution supports dynamic
as well as configured subscriptions to updates of datastore nodes.
Subscriptions specify when notification messages (also referred to as
"push updates") should be sent and what data to include in update
records.  Datastore node updates are subsequently pushed from the
publisher to the receiver per the terms of the subscription.

## Subscription Model

YANG-Push subscriptions are defined using a YANG data model.  This
model enhances the subscription model defined in [RFC8639] with
capabilities that allow subscribers to subscribe to datastore node
updates -- specifically, to specify the update triggers defining when
to generate update records as well as what to include in an update
record.  Key enhancements include:

- The specification of selection filters that identify targeted YANG
  datastore nodes and/or datastore subtrees for which updates are to
  be pushed.

- The specification of update policies that contain conditions that
  trigger the generation and pushing of new update records.  There
  are two types of subscriptions, distinguished by how updates are
  triggered:

    - For periodic subscriptions, the update trigger is specified by
        two parameters that define when updates are to be pushed.
        These parameters are (1) the period interval with which to
        report updates and (2) an "anchor-time", i.e., a reference
        point in time that can be used to calculate at which points in
        time periodic updates need to be assembled and sent.

    -  For on-change subscriptions, an update trigger occurs whenever
        a change in the subscribed information is detected.  The
        following additional parameters are included:

        -  "dampening-period": In an on-change subscription, detected
          object changes should be sent as quickly as possible.
          However, it may be undesirable to send a rapid series of
          object changes.  Such behavior has the potential to exhaust
          resources in the publisher or receiver.  In order to protect
          against this type of scenario, a dampening period MAY be
          used to specify the interval that has to pass before
          successive update records for the same subscription are
          generated for a receiver.  The dampening period collectively
          applies to the set of all datastore nodes selected by a
          single subscription.  This means that when there is a change
          to one or more subscribed objects, an update record
          containing those objects is created immediately (when no
          dampening period is in effect) or at the end of a dampening
          period (when a dampening period is in fact in effect).  If
          multiple changes to a single object occur during a dampening
          period, only the value that is in effect at the time when
          the update record is created is included.  The dampening
          period goes into effect every time the assembly of an update
          record is completed.

        -  "change-type": This parameter can be used to reduce the
          types of datastore changes for which updates are sent (e.g.,
          you might only send an update when an object is created or
          deleted, but not when an object value changes).

        -  "sync-on-start": This parameter defines whether or not a
          complete "push-update" (Section 3.7) of all subscribed data
          will be sent at the beginning of a subscription.  Such early
          synchronization establishes the frame of reference for
          subsequent updates.

- An encoding (using anydata) for the contents of periodic and
  on-change push updates.

### Negotiation of Subscription Policies

A dynamic subscription request SHOULD be declined if a publisher
determines that it may be unable to provide update records meeting
the terms of an "establish-subscription" or "modify-subscription" RPC
request.  In this case, a subscriber may quickly follow up with a new
RPC request using different parameters.

Random guessing of different parameters by a subscriber is to be
discouraged.  Therefore, in order to minimize the number of
subscription iterations between subscriber and publisher, a dynamic
subscription supports a simple negotiation between subscribers and
publishers for subscription parameters.  This negotiation is in the
form of supplemental information that should be inserted into error
responses to a failed RPC request.  This returned error response
information, when considered, should increase the likelihood of
success for subsequent RPC requests.  Such hints include suggested
periodic time intervals, acceptable dampening periods, and size
estimates for the number of objects that would be returned from a
proposed selection filter.  However, there are no guarantees that
subsequent requests that consider these hints will be accepted.

### On-Change Considerations

On-change subscriptions allow receivers to receive updates whenever
changes to targeted objects occur.  As such, on-change subscriptions
are particularly effective for data that changes infrequently but for
which applications need to be quickly notified, with minimal delay,
whenever a change does occur.

On-change subscriptions tend to be more difficult to implement than
periodic subscriptions.  Accordingly, on-change subscriptions may not
be supported by all implementations or for every object.

Whether or not to accept or reject on-change subscription requests
when the scope of the subscription contains objects for which
on-change is not supported is up to the publisher implementation.  A
publisher MAY accept an on-change subscription even when the scope of
the subscription contains objects for which on-change is not
supported.  In that case, updates are sent only for those objects
within the scope of the subscription that do support on-change
updates, whereas other objects are excluded from update records, even
if their values change.  In order for a subscriber to determine
whether objects support on-change subscriptions, objects are marked
accordingly on a publisher.  Accordingly, when subscribing, it is the
responsibility of the subscriber to ensure that it is aware of which
objects support on-change and which do not.  For more on how objects
are so marked, see Section 3.10.

Alternatively, a publisher MAY decide to simply reject an on-change
subscription if the scope of the subscription contains objects for
which on-change is not supported.  In the case of a configured
subscription, the publisher MAY suspend the subscription.

To avoid flooding receivers with repeated updates for subscriptions
containing fast-changing objects or objects with oscillating values,
an on-change subscription allows for the definition of a dampening
period.  Once an update record for a given object is generated, no
other updates for this particular subscription will be created until
the end of the dampening period.  Values sent at the end of the
dampening period are the values that are current at the end of the
dampening period of all changed objects.  Changed objects include
those objects that were deleted or newly created during that
dampening period.  If an object has returned to its original value
(or even has been created and then deleted) during the dampening
period, that value (and not the interim change) will still be sent.
This will indicate that churn is occurring on that object.

On-change subscriptions can be refined to let users subscribe only to
certain types of changes.  For example, a subscriber might only want
object creations and deletions, but not modifications of object
values.

Putting it all together, the conceptual process for creating an
update record as part of an on-change subscription is as follows:

1. Just before a change, or at the start of a dampening period,
    evaluate any filtering and any access control rules to ensure
    that a receiver is authorized to view all subscribed datastore
    nodes (filtering out any nodes for which this is not the case).
    The result is a set "A" of datastore nodes and subtrees.

2. Just after a change, or at the end of a dampening period,
    evaluate any filtering and any (possibly new) access control
    rules.  The result is a set "B" of datastore nodes and subtrees.

3. Construct an update record, which takes the form of a YANG Patch
    record [RFC8072] for going from A to B.

4. If there were any changes made between A and B that canceled each
    other out, insert into the YANG Patch record the last change
    made, even if the new value is no different from the original
    value (since changes that were made in the interim were canceled
    out).  If the changes involve creating a new datastore node and
    then deleting it, the YANG Patch record will indicate the
    deletion of the datastore node.  Similarly, if the changes
    involve deleting a new datastore node and then recreating it,
    the YANG Patch record will indicate the creation of the
    datastore node.

5. If the resulting YANG Patch record is non-empty, send it to the
    receiver.

Note: In cases where a subscriber wants to have separate dampening
periods for different objects, the subscriber has the option to
create multiple subscriptions with different selection filters.

### Reliability Considerations

A subscription to updates from a datastore is intended to obviate the
need for polling.  However, in order to do so, it is critical that
subscribers can rely on the subscription and have confidence that
they will indeed receive the subscribed updates without having to
worry about updates being silently dropped.  In other words, a
subscription constitutes a promise on the side of the publisher to
provide the receivers with updates per the terms of the subscription.

Now, there are many reasons why a publisher may at some point no
longer be able to fulfill the terms of the subscription, even if the
subscription had been initiated in good faith.  For example, the
volume of datastore nodes may be larger than anticipated, the
interval may prove too short to send full updates in rapid
succession, or an internal problem may prevent objects from being
collected.  For this reason, the solution defined in this document
(1) mandates that a publisher notify receivers immediately and
reliably whenever it encounters a situation in which it is unable to
keep the terms of the subscription and (2) provides the publisher
with the option to suspend the subscription in such a case.  This
includes indicating the fact that an update is incomplete as part of
a "push-update" or "push-change-update" notification, as well as
emitting a "subscription-suspended" notification as applicable.  This
is described further in Section 3.11.1.

A publisher SHOULD reject a request for a subscription if it is
unlikely that the publisher will be able to fulfill the terms of that
subscription request.  In such cases, it is preferable to have a
subscriber request a less resource-intensive subscription than to
deal with frequently degraded behavior.

The solution builds on [RFC8639].  As defined therein, any loss of an
underlying transport connection will be detected and result in
subscription termination (in the case of dynamic subscriptions) or
suspension (in the case of configured subscriptions), ensuring that
situations where the loss of update notifications would go unnoticed
will not occur.

### Data Encodings

#### Periodic Subscriptions

In a periodic subscription, the data included as part of an update
record corresponds to data that could have been read using a
retrieval operation.

#### On-Change Subscriptions

In an on-change subscription, update records need to indicate not
only values of changed datastore nodes but also the types of changes
that occurred since the last update.  Therefore, encoding rules for
data in on-change updates will generally follow YANG Patch operations
as specified in [RFC8072].  The YANG Patch operations will describe
what needs to be applied to the earlier state reported by the
preceding update in order to result in the now-current state.  Note
that objects referred to in an update are not limited to

configuration data but can include any objects (including operational
data), whereas [RFC8072] patches apply only to configuration data in
configuration datastores.

A publisher indicates the type of change to a datastore node using
the different YANG Patch operations: the "create" operation is used
for newly created objects (except entries in a user-ordered list),
the "delete" operation is used for deleted objects (including in
user-ordered lists), the "replace" operation is used when only the
object value changes, the "insert" operation is used when a new entry
is inserted in a list, and the "move" operation is used when an
existing entry in a user-ordered list is moved.

However, a patch must be able to do more than just describe the delta
from the previous state to the current state.  As per Section 3.3, it
must also be able to identify whether transient changes have occurred
on an object during a dampening period.  To support this, it is valid
to encode a YANG Patch operation so that its application would result
in no change between the previous state and the current state.  This
indicates that some churn has occurred on the object.  An example of
this would be a patch that indicates a "create" operation for a
datastore node where the receiver believes one already exists or a
"replace" operation that replaces a previous value with the same
value.  Note that this means that the "create" and "delete" errors as
described in [RFC8072], Section 2.5 are not errors in the case of
YANG-Push (i.e., they are considered valid operations for YANG-Push).

### Defining the Selection with a Datastore

A subscription must specify both the selection filters and the
datastore against which these selection filters will be applied.
This information is used to choose and subsequently push data from
the publisher's datastore to the receivers.

Only a single selection filter can be applied to a subscription at a
time.  An RPC request proposing a new selection filter replaces any
existing filter.  The following selection filter types are included
in the YANG-Push data model and may be applied against a datastore:

- subtree: A subtree selection filter identifies one or more
  datastore subtrees.  When specified, update records will only come
  from the datastore nodes of selected datastore subtree(s).  The
  syntax and semantics correspond to those specified in [RFC6241],
  Section 6.

- xpath: An "xpath" selection filter is an XPath expression that
  returns a node set.  (XPath is a query language for selecting
  nodes in an XML document; see [XPATH] for details.)  When
  specified, updates will only come from the selected datastore
  nodes.

These filters are intended to be used as selectors that define which
objects are within the scope of a subscription.  A publisher MUST
support at least one type of selection filter.

XPath itself provides powerful filtering constructs, and care must be
used in filter definition.  Consider an XPath filter that only passes
a datastore node when an interface is up.  It is up to the receiver
to understand the implications of the presence or absence of objects
in each update.

When the set of selection-filtering criteria is applied for a
periodic subscription, these criteria are applied whenever a periodic
update record is constructed, and only datastore nodes that pass the
filter and to which a receiver has access are provided to that
receiver.  If the same filtering criteria are applied to an on-change
subscription, only the subset of those datastore nodes supporting
on-change is provided.  A datastore node that doesn't support
on-change is never sent as part of an on-change subscription's
"push-update" or "push-change-update" (Section 3.7).

### Streaming Updates

Contrary to traditional data retrieval requests, datastore
subscription enables an unbounded series of update records to be
streamed over time.  Two generic YANG notifications for update
records have been defined for this scenario: "push-update" and
"push-change-update".

A "push-update" notification defines a complete, filtered update of
the datastore per the terms of a subscription.  This type of YANG
notification is used for continuous updates of periodic
subscriptions.  A "push-update" notification can also be used for the
on-change subscriptions in two cases.  First, it MUST be used as the
initial "push-update" if there is a need to synchronize the receiver
at the start of a new subscription.  Second, it MAY be sent if the
publisher later chooses to resync an on-change subscription.  The
"push-update" update record contains an instantiated datastore
subtree with all of the subscribed contents.  The content of the
update record is equivalent to the contents that would be obtained
had the same data been explicitly retrieved using a datastore
retrieval operation using the same transport with the same filters
applied.

A "push-change-update" notification is the most common type of update
for on-change subscriptions.  The update record in this case contains
the set of changes that datastore nodes have undergone since the last
notification message.  In other words, this indicates which datastore
nodes have been created, have been deleted, or have had changes to
their values.  In cases where multiple changes have occurred over the
course of a dampening period and the object has not been deleted, the
object's most current value is reported.  (In other words, for each
object, only one change is reported, not its entire history.  Doing
so would defeat the purpose of the dampening period.)

"push-update" and "push-change-update" are encoded and placed in
notification messages and are ultimately queued for egress over the
specified transport.

Figure 1 provides an example of a notification message for a
subscription tracking the operational status of a single Ethernet
interface (per {{RFC8343}}).  This notification message is encoded XML
*W3C.REC-xml-20081126* over the Network Configuration Protocol
(NETCONF) as per *RFC8640*.

~~~~~~~~~~
<notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
<eventTime>2017-10-25T08:00:11.22Z</eventTime>
<push-update xmlns="urn:ietf:params:xml:ns:yang:ietf-yang-push">
  <id>1011</id>
  <datastore-contents>
    <interfaces xmlns="urn:ietf:params:xml:ns:yang:ietf-interfaces">
      <interface>
        <name>eth0</name>
        <oper-status>up</oper-status>
      </interface>
    </interfaces>
  </datastore-contents>
</push-update>
</notification>

                      Figure 1: Push Example
~~~~~~~~~~

Figure 2 provides an example of an on-change notification message for
the same subscription.

~~~~~~~~~~
<notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
<eventTime>2017-10-25T08:22:33.44Z</eventTime>
<push-change-update
    xmlns="urn:ietf:params:xml:ns:yang:ietf-yang-push">
  <id>89</id>
  <datastore-changes>
    <yang-patch>
      <patch-id>0</patch-id>
      <edit>
        <edit-id>edit1</edit-id>
        <operation>replace</operation>
        <target>/ietf-interfaces:interfaces</target>
        <value>
          <interfaces
              xmlns="urn:ietf:params:xml:ns:yang:ietf-interfaces">
            <interface>
              <name>eth0</name>
              <oper-status>down</oper-status>
            </interface>
          </interfaces>
        </value>
      </edit>
    </yang-patch>
  </datastore-changes>
</push-change-update>
</notification>

    Figure 2: Push Example for an On-Change Notification Message
~~~~~~~~~~

Of note in the above example is the "patch-id" with a value of "0".
Per [RFC8072], the "patch-id" is an arbitrary string.  With
YANG-Push, the publisher SHOULD put into the "patch-id" a counter
starting at "0" that increments with every "push-change-update"
generated for a subscription.  If used as a counter, this counter
MUST be reset to "0" any time a resynchronization occurs (i.e., with
the sending of a "push-update").  Also, if used as a counter, the
counter MUST be reset to "0" after passing a maximum value of
"4294967295" (i.e., the maximum value that can be represented using
the uint32 data type).  Such a mechanism allows easy identification
of lost or out-of-sequence update records.


### Subscription Management

The RPCs defined in {{RFC8639}} have been enhanced to support datastore
subscription negotiation.  Also, new error codes have been added that
are able to indicate why a datastore subscription attempt has failed,
along with new yang-data that MAY be used to include details on input
parameters that might result in a successful subsequent RPC
invocation.

The establishment or modification of a datastore subscription can be
rejected for multiple reasons, including a subtree request that is
too large or the inability of the publisher to push update records as
frequently as requested.  In such cases, no subscription is
established.  Instead, a subscription result that includes the reason
for the failure is returned as part of the RPC response.  As part of
this response, a set of alternative subscription parameters MAY be
returned that would likely have resulted in acceptance of the
subscription request.  The subscriber may consider including such
parameters in future subscription attempts.

In the case of a rejected request for establishment of a datastore
subscription, if there are hints, the hints SHOULD be transported in
a yang-data "establish-subscription-datastore-error-info" container
inserted into the RPC error response, in lieu of the "establish-
subscription-stream-error-info" that is inserted in the case of a
stream subscription.

Figure 3 shows a tree diagram for "establish-subscription-datastore-
error-info".  All tree diagrams used in this document follow the
notation defined in {{RFC8340}}.

~~~~~~~~~~
          yang-data establish-subscription-datastore-error-info
            +--ro establish-subscription-datastore-error-info
               +--ro reason?                identityref
               +--ro period-hint?           centiseconds
               +--ro filter-failure-hint?   string
               +--ro object-count-estimate? uint32
               +--ro object-count-limit?    uint32
               +--ro kilobytes-estimate?    uint32
               +--ro kilobytes-limit?       uint32

   Figure 3: "establish-subscription-datastore-error-info" Tree Diagram
~~~~~~~~~~

   Similarly, in the case of a rejected request for modification of a
   datastore subscription, if there are hints, the hints SHOULD be
   transported in a yang-data "modify-subscription-datastore-error-info"
   container inserted into the RPC error response, in lieu of the
   "modify-subscription-stream-error-info" that is inserted in the case
   of a stream subscription.

   Figure 4 shows a tree diagram for "modify-subscription-datastore-
   error-info".

~~~~~~~~~~
          yang-data modify-subscription-datastore-error-info
            +--ro modify-subscription-datastore-error-info
               +--ro reason?                identityref
               +--ro period-hint?           centiseconds
               +--ro filter-failure-hint?   string
               +--ro object-count-estimate? uint32
               +--ro object-count-limit?    uint32
               +--ro kilobytes-estimate?    uint32
               +--ro kilobytes-limit?       uint32

     Figure 4: "modify-subscription-datastore-error-info" Tree Diagram
~~~~~~~~~~


### Receiver Authorization

A receiver of subscription data MUST only be sent updates for which
it has proper authorization.  A publisher MUST ensure that no
unauthorized data is included in push updates.  To do so, it needs to
apply all corresponding checks applicable at the time of a specific
pushed update and, if necessary, silently remove any unauthorized
data from datastore subtrees.  This enables YANG data that is pushed
based on subscriptions to be authorized in a way that is equivalent
to a regular data retrieval ("get") operation.

Each "push-update" and "push-change-update" MUST have access control
applied, as depicted in Figure 5.  This includes validating that read
access is permitted for any new objects selected since the last
notification message was sent to a particular receiver.  A publisher
MUST silently omit data nodes from the results that the client is not
authorized to see.  To accomplish this, implementations SHOULD apply
the conceptual authorization model of {{RFC8341}}, specifically
Section 3.2.4, extended to apply analogously to data nodes included
in notifications, not just \<rpc-reply\> messages sent in response to
\<get\> and \<get-config\> requests.

~~~~~~~~~~
                      +-----------------+      +--------------------+
  push-update or -->  | datastore node  |  yes | add datastore node |
 push-change-update   | access allowed? | ---> | to update record   |
                      +-----------------+      +--------------------+

              Figure 5: Access Control for Push Updates
~~~~~~~~~~

A publisher MUST allow for the possibility that a subscription's
selection filter references nonexistent data or data that a receiver
is not allowed to access.  Such support permits a receiver the
ability to monitor the entire lifecycle of some datastore tree
without needing to explicitly enumerate every individual datastore
node.  If, after access control has been applied, there are no
objects remaining in an update record, then the effect varies given
if the subscription is a periodic or on-change subscription.  For a
periodic subscription, an empty "push-update" notification MUST be
sent, so that clients do not get confused into thinking that an
update was lost.  For an on-change subscription, a "push-update"
notification MUST NOT be sent, so that clients remain unaware of
changes made to nodes they don't have read-access for.  By the same
token, changes to objects that are filtered MUST NOT affect any
dampening intervals.

A publisher MAY choose to reject an "establish-subscription" request
that selects nonexistent data or data that a receiver is not allowed
to access.  The error identity "unchanging-selection" SHOULD be
returned as the reason for the rejection.  In addition, a publisher
MAY choose to terminate a dynamic subscription or suspend a
configured receiver when the authorization privileges of a receiver
change or the access controls for subscribed objects change.  In that
case, the publisher SHOULD include the error identity "unchanging-
selection" as the reason when sending the "subscription-terminated"
or "subscription-suspended" notification, respectively.  Such a
capability enables the publisher to avoid having to support
continuous and total filtering of a subscription's content for every
update record.  It also reduces the possibility of leakage of
access-controlled objects.

If read access into previously accessible nodes has been lost due to
a receiver permissions change, this SHOULD be reported as a patch
"delete" operation for on-change subscriptions.  If not capable of
handling such receiver permission changes with such a "delete",
publisher implementations MUST force dynamic subscription
re-establishment or configured subscription reinitialization so that
appropriate filtering is installed.

### On-Change Notifiable Datastore Nodes

In some cases, a publisher supporting on-change notifications may not
be able to push on-change updates for some object types.  Reasons for
this might be that the value of the datastore node changes frequently
(e.g., the in-octets counter as defined in {{RFC8343}}), small object
changes are frequent and meaningless (e.g., a temperature gauge
changing 0.1 degrees), or the implementation is not capable of
on-change notification for a particular object.

In those cases, it will be important for client applications to have
a way to identify for which objects on-change notifications are
supported and for which ones they are not supported.  Otherwise,
client applications will have no way of knowing whether they can
indeed rely on their on-change subscription to provide them with the
change updates that they are interested in.  In other words, if
implementations do not provide a solution and do not support
comprehensive on-change notifiability, clients of those
implementations will have no way of knowing what their on-change
subscription actually covers.

Implementations are therefore strongly advised to provide a solution
to this problem.  One solution might involve making discoverable to
clients which objects are on-change notifiable, specified using
another YANG data model.  Such a solution is specified in
**Yang-Push-Notif-Cap**.  Until this solution is standardized,
implementations SHOULD provide their own solution.

### Other Considerations

#### Robustness and Reliability

It is important that updates as discussed in this document, and
on-change updates in particular, do not get lost.  If the loss of an
update is unavoidable, it is critical that the receiver be notified
accordingly.

Update records for a single subscription MUST NOT be resequenced
prior to transport.

It is conceivable that, under certain circumstances, a publisher will
recognize that it is unable to include in an update record the full
set of objects desired per the terms of a subscription.  In this
case, the publisher MUST act as follows:

  *  The publisher MUST set the "incomplete-update" flag on any update
    record that is known to be missing information.

  *  The publisher MAY choose to suspend the subscription as per
    [RFC8639].  If the publisher does not create an update record at
    all, it MUST suspend the subscription.

  *  When resuming an on-change subscription, the publisher SHOULD
    generate a complete patch from the previous update record.  If
    this is not possible and the "sync-on-start" option is set to
    "true" for the subscription, then the full datastore contents MAY
    be sent via a "push-update" instead (effectively replacing the
    previous contents).  If neither scenario above is possible, then
    an "incomplete-update" flag MUST be included on the next
    "push-change-update".

Note: It is perfectly acceptable to have a series of "push-change-
update" notifications (and even "push-update" notifications) serially
queued at the transport layer awaiting transmission.  It is not
required for the publisher to merge pending update records sent at
the same time.

On the receiver side, what action to take when a record with an
"incomplete-update" flag is received depends on the application.  It
could simply choose to wait and do nothing.  It could choose to
resync, actively retrieving all subscribed information.  It could
also choose to tear down the subscription and start a new one,
perhaps with a smaller scope that contains fewer objects.

#### Publisher Capacity

It is far preferable to decline a subscription request than to accept
such a request when it cannot be met.

Whether or not a subscription can be supported will be determined by
a combination of several factors, such as the subscription update
trigger (on-change or periodic), the period in which to report
changes (one-second periods will consume more resources than one-hour
periods), the amount of data in the datastore subtree that is being
subscribed to, and the number and combination of other subscriptions
that are concurrently being serviced.

## Subscription State Change Notifications

# YANG Data Model Tree Diagrams (from RFC 8639)

This section contains tree diagrams for nodes defined in Section 4.
For tree diagrams of subscription state change notifications, see
Section 2.7.  For the tree diagrams for the RPCs, see Section 2.4.

## The "streams" Container

A publisher maintains a list of available event streams as
operational data.  This list contains both standardized and
vendor-specific event streams.  This enables subscribers to discover
what streams a publisher supports.

Below is a tree diagram for the "streams" container.  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
  +--ro streams
    +--ro stream* [name]
        +--ro name                        string
        +--ro description                 string
        +--ro replay-support?             empty {replay}?
        +--ro replay-log-creation-time    yang:date-and-time
        |       {replay}?
        +--ro replay-log-aged-time?       yang:date-and-time
                {replay}?

            Figure 18: "streams" Container Tree Diagram
~~~~~~~~~~

## The "filters" Container

The "filters" container maintains a list of all subscription filters
that persist outside the lifecycle of a single subscription.  This
enables predefined filters that may be referenced by more than one
subscription.

Below is a tree diagram for the "filters" container.  All objects
contained in this tree are described in the YANG module in Section 4.

~~~~~~~~~~
  +--rw filters
    +--rw stream-filter* [name]
        +--rw name                           string
        +--rw (filter-spec)?
          +--:(stream-subtree-filter)
          |  +--rw stream-subtree-filter?   <anydata> {subtree}?
          +--:(stream-xpath-filter)
              +--rw stream-xpath-filter?     yang:xpath1.0 {xpath}?

            Figure 19: "filters" Container Tree Diagram
~~~~~~~~~~

## The "subscriptions" Container

The "subscriptions" container maintains a list of all subscriptions
on a publisher, both configured and dynamic.  It can be used to
retrieve information about the subscriptions that a publisher is
serving.

Below is a tree diagram for the "subscriptions" container.  All
objects contained in this tree are described in the YANG module in
Section 4.

~~~~~~~~~~
  +--rw subscriptions
    +--rw subscription* [id]
        +--rw id
        |       subscription-id
        +--rw (target)
        |  +--:(stream)
        |     +--rw (stream-filter)?
        |     |  +--:(by-reference)
        |     |  |  +--rw stream-filter-name
        |     |  |          stream-filter-ref
        |     |  +--:(within-subscription)
        |     |     +--rw (filter-spec)?
        |     |        +--:(stream-subtree-filter)
        |     |        |  +--rw stream-subtree-filter?   <anydata>
        |     |        |          {subtree}?
        |     |        +--:(stream-xpath-filter)
        |     |           +--rw stream-xpath-filter?
        |     |                   yang:xpath1.0 {xpath}?
        |     +--rw stream                               stream-ref
        |     +--ro replay-start-time?
        |     |       yang:date-and-time {replay}?
        |     +--rw configured-replay?                   empty
        |             {configured,replay}?
        +--rw stop-time?
        |       yang:date-and-time
        +--rw dscp?                                      inet:dscp
        |       {dscp}?
        +--rw weighting?                                 uint8 {qos}?
        +--rw dependency?
        |       subscription-id {qos}?
        +--rw transport?                                 transport
        |       {configured}?
        +--rw encoding?                                  encoding
        +--rw purpose?                                   string
        |       {configured}?
        +--rw (notification-message-origin)? {configured}?
        |  +--:(interface-originated)
        |  |  +--rw source-interface?
        |  |          if:interface-ref {interface-designation}?
        |  +--:(address-originated)
        |     +--rw source-vrf?
        |     |       -> /ni:network-instances/network-instance/name
        |     |       {supports-vrf}?
        |     +--rw source-address?
        |             inet:ip-address-no-zone
        +--ro configured-subscription-state?             enumeration
        |       {configured}?
        +--rw receivers
          +--rw receiver* [name]
              +--rw name                      string
              +--ro sent-event-records?
              |       yang:zero-based-counter64
              +--ro excluded-event-records?
              |       yang:zero-based-counter64
              +--ro state                     enumeration
              +---x reset {configured}?
                +--ro output
                    +--ro time    yang:date-and-time

          Figure 20: "subscriptions" Container Tree Diagram
~~~~~~~~~~

# A YANG Data Model for Management of Datastore Push Subscriptions (from RFC 8641)

##  Overview

The YANG data model for datastore push subscriptions is depicted in
Figures 6 through 9.  The tree diagram that is used follows the
notation defined in [RFC8340].  New schema objects defined here
(i.e., beyond those from [RFC8639]) are identified with "yp".  For
the reader's convenience, in order to compact the tree
representation, some nodes that are defined in the ietf-subscribed-
notifications YANG module [RFC8639] and therefore are not essential
to the understanding of the data model defined here have been
removed.  This is indicated by "..." in the diagram where applicable.

Because the tree diagram is quite large, its depiction is broken up
into four figures.  Figure 6 depicts the augmentations that are
introduced in YANG module ietf-yang-push to the subscription
configuration specified in YANG module ietf-subscribed-notifications.

~~~~
  module: ietf-subscribed-notifications
    ...
    +--rw filters
    |  ...
    |  +--rw yp:selection-filter* [filter-id]
    |     +--rw yp:filter-id                   string
    |     +--rw (yp:filter-spec)?
    |        +--:(yp:datastore-subtree-filter)
    |        |  +--rw yp:datastore-subtree-filter?   <anydata>
    |        |          {sn:subtree}?
    |        +--:(yp:datastore-xpath-filter)
    |           +--rw yp:datastore-xpath-filter?     yang:xpath1.0
    |                   {sn:xpath}?
    +--rw subscriptions
      +--rw subscription* [id]
          |  ...
          +--rw (target)
          |  +--:(stream)
          |  |   ...
          |  +--:(yp:datastore)
          |     +--rw yp:datastore                     identityref
          |     +--rw (yp:selection-filter)?
          |        +--:(yp:by-reference)
          |        |  +--rw yp:selection-filter-ref
          |        |          selection-filter-ref
          |        +--:(yp:within-subscription)
          |           +--rw (yp:filter-spec)?
          |              +--:(yp:datastore-subtree-filter)
          |              |  +--rw yp:datastore-subtree-filter?
          |              |          <anydata> {sn:subtree}?
          |              +--:(yp:datastore-xpath-filter)
          |                 +--rw yp:datastore-xpath-filter?
          |                         yang:xpath1.0 {sn:xpath}?
          | ...
          +--rw (yp:update-trigger)
            +--:(yp:periodic)
            |  +--rw yp:periodic!
            |     +--rw yp:period         centiseconds
            |     +--rw yp:anchor-time?   yang:date-and-time
            +--:(yp:on-change) {on-change}?
                +--rw yp:on-change!
                  +--rw yp:dampening-period?   centiseconds
                  +--rw yp:sync-on-start?      boolean
                  +--rw yp:excluded-change*    change-type

      Figure 6: Data Model Structure: Subscription Configuration
~~~~

Figure 7 depicts the augmentations of YANG module ietf-yang-push made
to RPCs specified in YANG module ietf-subscribed-notifications
[RFC8639].  Specifically, these augmentations concern the "establish-
subscription" and "modify-subscription" RPCs, which are augmented
with parameters that are needed to specify datastore push
subscriptions.

~~~~
  rpcs:
    +---x establish-subscription
    |  +---w input
    |  |  ...
    |  |  +---w (target)
    |  |  |  +--:(stream)
    |  |  |  |  ...
    |  |  |  +--:(yp:datastore)
    |  |  |     +---w yp:datastore                   identityref
    |  |  |     +---w (yp:selection-filter)?
    |  |  |        +--:(yp:by-reference)
    |  |  |        |  +---w yp:selection-filter-ref
    |  |  |        |          selection-filter-ref
    |  |  |        +--:(yp:within-subscription)
    |  |  |           +---w (yp:filter-spec)?
    |  |  |              +--:(yp:datastore-subtree-filter)
    |  |  |              |  +---w yp:datastore-subtree-filter?
    |  |  |              |          <anydata> {sn:subtree}?
    |  |  |              +--:(yp:datastore-xpath-filter)
    |  |  |                 +---w yp:datastore-xpath-filter?
    |  |  |                         yang:xpath1.0 {sn:xpath}?
    |  |  | ...
    |  |  +---w (yp:update-trigger)
    |  |     +--:(yp:periodic)
    |  |     |  +---w yp:periodic!
    |  |     |     +---w yp:period         centiseconds
    |  |     |     +---w yp:anchor-time?   yang:date-and-time
    |  |     +--:(yp:on-change) {on-change}?
    |  |        +---w yp:on-change!
    |  |           +---w yp:dampening-period?   centiseconds
    |  |           +---w yp:sync-on-start?      boolean
    |  |           +---w yp:excluded-change*    change-type
    |  +--ro output
    |     +--ro id                            subscription-id
    |     +--ro replay-start-time-revision?   yang:date-and-time
    |             {replay}?
    +---x modify-subscription
    |  +---w input
    |     ...
    |     +---w (target)
    |     |  ...
    |     |  +--:(yp:datastore)
    |     |     +---w yp:datastore                   identityref
    |     |     +---w (yp:selection-filter)?
    |     |        +--:(yp:by-reference)
    |     |        |  +---w yp:selection-filter-ref
    |     |        |          selection-filter-ref
    |     |        +--:(yp:within-subscription)
    |     |           +---w (yp:filter-spec)?
    |     |              +--:(yp:datastore-subtree-filter)
    |     |              |  +---w yp:datastore-subtree-filter?
    |     |              |          <anydata> {sn:subtree}?
    |     |              +--:(yp:datastore-xpath-filter)
    |     |                 +---w yp:datastore-xpath-filter?
    |     |                         yang:xpath1.0 {sn:xpath}?
    |     | ...
    |     +---w (yp:update-trigger)
    |        +--:(yp:periodic)
    |        |  +---w yp:periodic!
    |        |     +---w yp:period         centiseconds
    |        |     +---w yp:anchor-time?   yang:date-and-time
    |        +--:(yp:on-change) {on-change}?
    |           +---w yp:on-change!
    |              +---w yp:dampening-period?   centiseconds
    +---x delete-subscription
    |  ...
    +---x kill-subscription
      ...

  yang-data (for placement into RPC error responses)
    ...

                Figure 7: Data Model Structure: RPCs
~~~~

Figure 8 depicts augmentations of YANG module ietf-yang-push to the
notifications that are specified in YANG module ietf-subscribed-
notifications.  The augmentations allow the inclusion of subscription
configuration parameters that are specific to datastore push
subscriptions as part of "subscription-started" and "subscription-
modified" notifications.

~~~~
     notifications:
       +---n replay-completed {replay}?
       |  ...
       +---n subscription-completed
       |  ...
       +---n subscription-started {configured}?
       |  |  ...
       |  +--ro (target)
       |  |  ...
       |  |  +--:(yp:datastore)
       |  |     +--ro yp:datastore                   identityref
       |  |     +--ro (yp:selection-filter)?
       |  |        +--:(yp:by-reference)
       |  |        |  +--ro yp:selection-filter-ref
       |  |        |          selection-filter-ref
       |  |        +--:(yp:within-subscription)
       |  |           +--ro (yp:filter-spec)?
       |  |              +--:(yp:datastore-subtree-filter)
       |  |              |  +--ro yp:datastore-subtree-filter?
       |  |              |          <anydata> {sn:subtree}?
       |  |              +--:(yp:datastore-xpath-filter)
       |  |                 +--ro yp:datastore-xpath-filter?
       |  |                         yang:xpath1.0 {sn:xpath}?
       |  ...
       |  +--ro (yp:update-trigger)
       |     +--:(yp:periodic)
       |     |  +--ro yp:periodic!
       |     |     +--ro yp:period         centiseconds
       |     |     +--ro yp:anchor-time?   yang:date-and-time
       |     +--:(yp:on-change) {on-change}?
       |        +--ro yp:on-change!
       |           +--ro yp:dampening-period?   centiseconds
       |           +--ro yp:sync-on-start?      boolean
       |           +--ro yp:excluded-change*    change-type
       +---n subscription-resumed
       |  ...
       +---n subscription-modified
       |  ...
       |  +--ro (target)
       |  |  |  ...
       |  |  +--:(yp:datastore)
       |  |     +--ro yp:datastore                   identityref
       |  |     +--ro (yp:selection-filter)?
       |  |        +--:(yp:by-reference)
       |  |        |  +--ro yp:selection-filter-ref
       |  |        |          selection-filter-ref
       |  |        +--:(yp:within-subscription)
       |  |           +--ro (yp:filter-spec)?
       |  |              +--:(yp:datastore-subtree-filter)
       |  |              |  +--ro yp:datastore-subtree-filter?
       |  |              |          <anydata> {sn:subtree}?
       |  |              +--:(yp:datastore-xpath-filter)
       |  |                 +--ro yp:datastore-xpath-filter?
       |  |                         yang:xpath1.0 {sn:xpath}?
       |  ...
       |  +--ro (yp:update-trigger)?
       |     +--:(yp:periodic)
       |     |  +--ro yp:periodic!
       |     |     +--ro yp:period         centiseconds
       |     |     +--ro yp:anchor-time?   yang:date-and-time
       |     +--:(yp:on-change) {on-change}?
       |        +--ro yp:on-change!
       |           +--ro yp:dampening-period?    centiseconds
       |           +--ro yp:sync-on-start?       boolean
       |           +--ro yp:excluded-change*     change-type
       +---n subscription-terminated
       |  ...
       +---n subscription-suspended
          ...

               Figure 8: Data Model Structure: Notifications
~~~~

Finally, Figure 9 depicts the parts of YANG module ietf-yang-push
that are newly introduced in this document (i.e., that are not simply
augmentations of another YANG module).

~~~~
module: ietf-yang-push

  rpcs:
    +---x resync-subscription {on-change}?
      +---w input
          +---w id    sn:subscription-id

  yang-data (for placement into RPC error responses):
    +-- resync-subscription-error
    |  +--ro reason?                   identityref
    |  +--ro period-hint?              centiseconds
    |  +--ro filter-failure-hint?      string
    |  +--ro object-count-estimate?    uint32
    |  +--ro object-count-limit?       uint32
    |  +--ro kilobytes-estimate?       uint32
    |  +--ro kilobytes-limit?          uint32
    +-- establish-subscription-error-datastore
    |  +--ro reason?                   identityref
    |  +--ro period-hint?              centiseconds
    |  +--ro filter-failure-hint?      string
    |  +--ro object-count-estimate?    uint32
    |  +--ro object-count-limit?       uint32
    |  +--ro kilobytes-estimate?       uint32
    |  +--ro kilobytes-limit?          uint32
    +-- modify-subscription-error-datastore
      +--ro reason?                   identityref
      +--ro period-hint?              centiseconds
      +--ro filter-failure-hint?      string
      +--ro object-count-estimate?    uint32
      +--ro object-count-limit?       uint32
      +--ro kilobytes-estimate?       uint32
      +--ro kilobytes-limit?          uint32
    notifications:
      +---n push-update
      |  +--ro id?                   sn:subscription-id
      |  +--ro datastore-contents?   <anydata>
      |  +--ro incomplete-update?    empty
      +---n push-change-update {on-change}?
          +--ro id?                   sn:subscription-id
          +--ro datastore-changes
          |  +--ro yang-patch
          |     +--ro patch-id    string
          |     +--ro comment?    string
          |     +--ro edit* [edit-id]
          |        +--ro edit-id      string
          |        +--ro operation    enumeration
          |        +--ro target       target-resource-offset
          |        +--ro point?       target-resource-offset
          |        +--ro where?       enumeration
          |        +--ro value?       <anydata>
          +--ro incomplete-update?    empty

      Figure 9: Data Model Structure: Non-augmentation Portions
~~~~

Selected components of the data model are summarized below.

## Subscription Configuration

Both configured and dynamic subscriptions are represented in the list
"subscription".  New parameters extending the basic subscription data
model in [RFC8639] include:

- The targeted datastore from which the selection is being made.
  The potential datastores include those from [RFC8342].  A platform
  may also choose to support a custom datastore.

- A selection filter identifying YANG nodes of interest in a
  datastore.  Filter contents are specified via a reference to an
  existing filter or via an in-line definition for only that
  subscription.  Referenced filters allow an implementation to avoid
  evaluating filter acceptability during a dynamic subscription
  request.  The "case" statement differentiates the options.

- For periodic subscriptions, triggered updates will occur at the
  boundaries of a specified time interval.  These boundaries can be
  calculated from the periodic parameters:

  -  a "period" that defines the duration between push updates.

  -  an "anchor-time"; update intervals fall on the points in time
      that are a multiple of a "period" from an "anchor-time".  If an
      "anchor-time" is not provided, then the "anchor-time" MUST be
      set with the creation time of the initial update record.

- For on-change subscriptions, assuming that any dampening period
  has completed, triggering occurs whenever a change in the
  subscribed information is detected.  On-change subscriptions have
  more-complex semantics that are guided by their own set of
  parameters:

  - a "dampening-period" that specifies the interval that must pass
      before a successive update for the subscription is sent.  If no
      dampening period is in effect, the update is sent immediately.
      If a subsequent change is detected, another update is only sent
      once the dampening period has passed for this subscription.

  - an "excluded-change" that allows the restriction of the types
      of changes for which updates should be sent (e.g., only add to
      an update record on object creation).

  - a "sync-on-start" that specifies whether a complete update with
      all the subscribed data is to be sent at the beginning of a
      subscription.

##  YANG Notifications

###  State Change Notifications

Subscription state notifications and mechanisms are reused from
[RFC8639].  Notifications "subscription-started" and "subscription-
modified" have been augmented to include the datastore-specific
objects.

###  Notifications for Subscribed Content

Along with the subscribed content, there are other objects that might
be part of a "push-update" or "push-change-update" notification.

- An "id" (that identifies the subscription).  This object MUST be
  transported along with the subscribed contents.  It allows a
  receiver to determine which subscription resulted in a particular
  update record.

- An "incomplete-update" leaf.  This leaf indicates that not all
  changes that have occurred since the last update are actually
  included with this update.  In other words, the publisher has
  failed to fulfill its full subscription obligations.  (For
  example, a datastore was unable to provide the full set of
  datastore nodes to a publisher process.)  To facilitate the
  resynchronization of on-change subscriptions, a publisher MAY
  subsequently send a "push-update" containing a full selection
  snapshot of subscribed data.

##  YANG RPCs

YANG-Push subscriptions are established, modified, and deleted using
RPCs augmented from [RFC8639].

###  "establish-subscription" RPC

The subscriber sends an "establish-subscription" RPC with the
parameters listed in Section 3.1.  An example might look like:

~~~~
 <netconf:rpc message-id="101"
     xmlns:netconf="urn:ietf:params:xml:ns:netconf:base:1.0">
   <establish-subscription
       xmlns="urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications"
       xmlns:yp="urn:ietf:params:xml:ns:yang:ietf-yang-push">
     <yp:datastore
          xmlns:ds="urn:ietf:params:xml:ns:yang:ietf-datastores">
       ds:operational
     </yp:datastore>
     <yp:datastore-xpath-filter
         xmlns:ex="https://example.com/sample-data/1.0">
       /ex:foo
     </yp:datastore-xpath-filter>
     <yp:periodic>
       <yp:period>500</yp:period>
     </yp:periodic>
   </establish-subscription>
 </netconf:rpc>

                  Figure 10: "establish-subscription" RPC
~~~~

A positive response includes the "id" of the accepted subscription.
In that case, a publisher may respond as follows:

~~~~
 <rpc-reply message-id="101"
    xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <id
      xmlns="urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications">
       52
    </id>
 </rpc-reply>

         Figure 11: "establish-subscription" Positive RPC Response
~~~~

A subscription can be rejected for multiple reasons, including the
lack of authorization to establish a subscription, no capacity to
serve the subscription at the publisher, or the inability of the
publisher to select datastore content at the requested cadence.

If a request is rejected because the publisher is not able to serve
it, the publisher SHOULD include in the returned error hints that
help a subscriber understand what subscription parameters might have
been accepted for the request.  These hints would be included in the
yang-data structure "establish-subscription-error-datastore".
However, even with these hints, there are no guarantees that
subsequent requests will in fact be accepted.

The specific parameters to be returned as part of the RPC error
response depend on the specific transport that is used to manage the
subscription.  For NETCONF, those parameters are defined in
[RFC8640].  For example, for the following NETCONF request:

~~~~
  <rpc message-id="101"
      xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <establish-subscription
        xmlns=
          "urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications"
        xmlns:yp="urn:ietf:params:xml:ns:yang:ietf-yang-push">
      <yp:datastore
          xmlns:ds="urn:ietf:params:xml:ns:yang:ietf-datastores">
        ds:operational
      </yp:datastore>
      <yp:datastore-xpath-filter
          xmlns:ex="https://example.com/sample-data/1.0">
        /ex:foo
      </yp:datastore-xpath-filter>
      <yp:on-change>
        <yp:dampening-period>100</yp:dampening-period>
      </yp:on-change>
    </establish-subscription>
  </rpc>

      Figure 12: "establish-subscription" Request: Example 2
~~~~

A publisher that cannot serve on-change updates but can serve
periodic updates might return the following NETCONF response:

~~~~
 <rpc-reply message-id="101"
   xmlns="urn:ietf:params:xml:ns:netconf:base:1.0"
   xmlns:yp="urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications">
   <rpc-error>
     <error-type>application</error-type>
     <error-tag>operation-failed</error-tag>
     <error-severity>error</error-severity>
     <error-path>/yp:periodic/yp:period</error-path>
     <error-info>
       <yp:establish-subscription-error-datastore>
         <yp:reason>yp:on-change-unsupported</yp:reason>
       </yp:establish-subscription-error-datastore>
     </error-info>
   </rpc-error>
 </rpc-reply>

       Figure 13: "establish-subscription" Error Response: Example 2
~~~~

### "modify-subscription" RPC

The subscriber MAY invoke the "modify-subscription" RPC for a
subscription it previously established.  The subscriber will include
newly desired values in the "modify-subscription" RPC.  Parameters
not included MUST remain unmodified.  Figure 14 provides an example
where a subscriber attempts to modify the period and datastore XPath
filter of a subscription using NETCONF.

~~~~
  <rpc message-id="102"
      xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <modify-subscription
        xmlns=
          "urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications"
        xmlns:yp="urn:ietf:params:xml:ns:yang:ietf-yang-push">
      <id>1011</id>
      <yp:datastore
          xmlns:ds="urn:ietf:params:xml:ns:yang:ietf-datastores">
        ds:operational
      </yp:datastore>
      <yp:datastore-xpath-filter
          xmlns:ex="https://example.com/sample-data/1.0">
        /ex:bar
      </yp:datastore-xpath-filter>
      <yp:periodic>
        <yp:period>250</yp:period>
      </yp:periodic>
    </modify-subscription>
  </rpc>

              Figure 14: "modify-subscription" Request
~~~~

The publisher MUST respond to the subscription modification request.
If the request is rejected, the existing subscription is left
unchanged, and the publisher MUST send an RPC error response.  This
response might have hints encapsulated in the yang-data structure
"modify-subscription-error-datastore".  A subscription MAY be
modified multiple times.

The specific parameters to be returned as part of the RPC error
response depend on the specific transport that is used to manage the
subscription.  For NETCONF, those parameters are specified in
[RFC8640].

A configured subscription cannot be modified using a
"modify-subscription" RPC.  Instead, the configuration needs to be
edited as needed.

###  "delete-subscription" RPC

To stop receiving updates from a subscription and effectively delete
a subscription that had previously been established using an
"establish-subscription" RPC, a subscriber can send a
"delete-subscription" RPC, which takes as its only input the
subscription's "id".  This RPC is unmodified from [RFC8639].

###  "resync-subscription" RPC

This RPC is supported only for on-change subscriptions previously
established using an "establish-subscription" RPC.  For example:

~~~~
  <rpc message-id="103"
        xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <resync-subscription
        xmlns="urn:ietf:params:xml:ns:yang:ietf-yang-push">
      <id>1011</id>
    </resync-subscription>
  </rpc>

                  Figure 15: "resync-subscription"
~~~~

On receipt, a publisher must either (1) accept the request and
quickly follow with a "push-update" or (2) send an appropriate error
in an RPC error response.  In its error response, the publisher MAY
include, in the yang-data structure "resync-subscription-error",
supplemental information about the reasons for the error.

### YANG Module Synchronization

To make subscription requests, the subscriber needs to know the YANG
datastore schemas used by the publisher.  These schemas are available
in the YANG library module ietf-yang-library.yang as defined in
[RFC8525].  The receiver is expected to know the YANG library
information before starting a subscription.

The set of modules, revisions, features, and deviations can change at
runtime (if supported by the publisher implementation).  For this
purpose, the YANG library provides a simple "yang-library-change"
notification that informs the subscriber that the library has
changed.  In this case, a subscription may need to be updated to take
the updates into account.  The receiver may also need to be informed
of module changes in order to process updates regarding datastore
nodes from changed modules correctly.

# YANG Model

RFC 8639: This module imports typedefs from [RFC6991], [RFC8343], [RFC8341],
[RFC8529], and [RFC8040].  It references [RFC6241], [XPATH] ("XML
Path Language (XPath) Version 1.0"), [RFC7049], [RFC8259], [RFC7950],
[RFC7951], and [RFC7540].

RFC 8641: This YANG module imports typedefs from [RFC6991], identities from
[RFC8342], the "yang-data" extension from [RFC8040], and the
"yang-patch" grouping from [RFC8072].  In addition, it imports and
augments many definitions from [RFC8639].  It also references
[RFC6241], [XPATH] ("XML Path Language (XPath) Version 1.0"), and
[RFC7950].

~~~~ yang
{::include yang/published/ietf-subscribed-notifications@2019-09-09.yang}
~~~~
{: align="left" sourcecode-markers="true" sourcecode-name="ietf-subscribed-notifications@2019-09-09.yang" title="YANG module ietf-subscribed-notifications"}

~~~~ yang
{::include yang/published/ietf-yang-push@2019-09-09.yang}
~~~~
{: align="left" sourcecode-markers="true" sourcecode-name="ietf-yang-push@2019-09-09.yang" title="YANG module ietf-yang-push"}


# Implementation Considerations (from RFC 8639)

To support deployments that include both configured and dynamic
subscriptions, it is recommended that the subscription "id" domain be
split into static and dynamic halves.  This will eliminate the
possibility of collisions if the configured subscriptions attempt to
set a "subscription-id" that might have already been dynamically
allocated.  A best practice is to use the lower half of the "id"
object's integer space when that "id" is assigned by an external
entity (such as with a configured subscription).  This leaves the
upper half of the subscription integer space available to be
dynamically assigned by the publisher.

If a subscription is unable to marshal a series of filtered event
records into transmittable notification messages, the receiver should
be suspended with the reason "unsupportable-volume".

For configured subscriptions, operations are performed against the
set of receivers using the subscription "id" as a handle for that
set.  But for streaming updates, subscription state change
notifications are local to a receiver.  In the case of this
specification, receivers do not get any information from the
publisher about the existence of other receivers.  But if a network
operator wants to let the receivers correlate results, it is useful
to use the subscription "id" across the receivers to allow that
correlation.  Note that due to the possibility of different access
control permissions per receiver, each receiver may actually get a
different set of event records.

For configured replay subscriptions, the receiver is protected from
duplicated events being pushed after a publisher is rebooted.
However, it is possible that a receiver might want to acquire event
records that failed to be delivered just prior to the reboot.
Delivering these event records can be accomplished by leveraging the
\<eventTime\> [RFC5277] from the last event record received prior to
the receipt of a "subscription-started" subscription state change
notification.  With this \<eventTime\> and the "replay-start-time" from
the "subscription-started" notification, an independent dynamic
subscription can be established that retrieves any event records that
may have been generated but not sent to the receiver.

# Transport Requirements (from RFC 8639)

This section provides requirements for any subscribed notification
transport supporting the solution presented in this document.

The transport selected by the subscriber to reach the publisher MUST
be able to support multiple "establish-subscription" requests made in
the same transport session.

For both configured and dynamic subscriptions, the publisher MUST
authenticate a receiver via some transport-level mechanism before
sending any event records that the receiver is authorized to see.  In
addition, the receiver MUST authenticate the publisher at the
transport level.  The result is mutual authentication between
the two.

A secure transport is highly recommended.  Beyond this, the publisher
MUST ensure that the receiver has sufficient authorization to perform
the function it is requesting against the specific subset of content
involved.

A specification for a transport built upon this document may or may
not choose to require the use of the same logical channel for the
RPCs and the event records.  However, the event records and the
subscription state change notifications MUST be sent on the same
transport session to ensure properly ordered delivery.

A specification for a transport MUST identify any encodings that are
supported.  If a configured subscription's transport allows different
encodings, the specification MUST identify the default encoding.

A subscriber that includes a "dscp" leaf in an "establish-
subscription" request will need to understand and consider what the
corresponding DSCP value represents in the domain of the publisher.

Additional transport requirements will be dictated by the choice of
transport used with a subscription.  For an example of such
requirements, see [RFC8640].

# Security Considerations

TODO.  New YANG models will be defined that need to document their security considerations, but otherwise the security considerations in YANG-Push should be sufficient.  Note, we should use the new security considerations template, which will allow this section to be considerable shorter.

## Security Considerations from RFC 8639

The YANG module specified in this document defines a schema for data
that is designed to be accessed via network management protocols such
as NETCONF [RFC6241] or RESTCONF [RFC8040].  The lowest NETCONF layer
is the secure transport layer, and the mandatory-to-implement secure
transport is Secure Shell (SSH) [RFC6242].  The lowest RESTCONF layer
is HTTPS, and the mandatory-to-implement secure transport is TLS
[RFC5246].

The Network Configuration Access Control Model (NACM) [RFC8341]
provides the means to restrict access for particular NETCONF or
RESTCONF users to a preconfigured subset of all available NETCONF or
RESTCONF protocol operations and content.

With configured subscriptions, one or more publishers could be used
to overwhelm a receiver.  To counter this, notification messages
SHOULD NOT be sent to any receiver that does not support this
specification.  Receivers that do not want notification messages need
only terminate or refuse any transport sessions from the publisher.

When a receiver of a configured subscription gets a new
"subscription-started" message for a known subscription where it is
already consuming events, it may indicate that an attacker has done
something that has momentarily disrupted receiver connectivity.  To
acquire events lost during this interval, the receiver SHOULD
retrieve any event records generated since the last event record was
received.  This can be accomplished by establishing a separate
dynamic replay subscription with the same filtering criteria with the
publisher, assuming that the publisher supports the "replay" feature.

For dynamic subscriptions, implementations need to protect against
malicious or buggy subscribers that may send a large number of
"establish-subscription" requests and thereby use up system
resources.  To cover this possibility, operators SHOULD monitor for
such cases and, if discovered, take remedial action to limit the
resources used, such as suspending or terminating a subset of the
subscriptions or, if the underlying transport is session based,
terminating the underlying transport session.

The replay mechanisms described in Sections 2.4.2.1 and 2.5.6 provide
access to historical event records.  By design, the access control
model that protects these records could enable subscribers to view
data to which they were not authorized at the time of collection.

Using DNS names for configured subscription's receiver "name" lookups
can cause situations where the name resolves differently than
expected on the publisher, so the recipient would be different than
expected.

An attacker that can cause the publisher to use an incorrect time can
induce message replay by setting the time in the past and can
introduce a risk of message loss by setting the time in the future.

There are a number of data nodes defined in this YANG module that are
writable/creatable/deletable (i.e., config true, which is the
default).  These data nodes may be considered sensitive or vulnerable
in some network environments.  Write operations (e.g., edit-config)
to these data nodes without proper protection can have a negative
effect on network operations.  These are the subtrees and data nodes
and their sensitivity/vulnerability:

Container: "/filters"

- "stream-subtree-filter": Updating a filter could increase the
  computational complexity of all referencing subscriptions.

- "stream-xpath-filter": Updating a filter could increase the
  computational complexity of all referencing subscriptions.

Container: "/subscriptions"

The following considerations are only relevant for configuration
operations made upon configured subscriptions:

- "configured-replay": Can be used to send a large number of event
  records to a receiver.

- "dependency": Can be used to force important traffic to be queued
  behind updates that are not as important.

- "dscp": If unvalidated, can result in the sending of traffic with
  a higher-priority marking than warranted.

- "id": Can overwrite an existing subscription, perhaps one
  configured by another entity.

- "name": Adding a new key entry can be used to attempt to send
  traffic to an unwilling receiver.

- "replay-start-time": Can be used to push very large logs, wasting
  resources.

- "source-address": The configured address might not be able to
  reach a desired receiver.

- "source-interface": The configured interface might not be able to
  reach a desired receiver.

- "source-vrf": Can place a subscription in a virtual network where
  receivers are not entitled to view the subscribed content.

- "stop-time": Could be used to terminate content at an inopportune
  time.

- "stream": Could set a subscription to an event stream that does
  not contain content permitted for the targeted receivers.

- "stream-filter-name": Could be set to a filter that is not
  relevant to the event stream.

- "stream-subtree-filter": A complex filter can increase the
  computational resources for this subscription.

- "stream-xpath-filter": A complex filter can increase the
  computational resources for this subscription.

- "weighting": Allocating a large weight can overwhelm the dequeuing
  of other subscriptions.

Some of the readable data nodes in this YANG module may be considered
sensitive or vulnerable in some network environments.  It is thus
important to control read access (e.g., via get, get-config, or
notification) to these data nodes.  These are the subtrees and data
nodes and their sensitivity/vulnerability:

Container: "/streams"

- "name": If access control is not properly configured, can expose
  system internals to those who should not have access to this
  information.

- "replay-support": If access control is not properly configured,
  can expose logs to those who should not have access.

Container: "/subscriptions"

- "excluded-event-records": This leaf can provide information about
  filtered event records.  A network operator should have the proper
  permissions to know about such filtering.  However, exposing the
  count of excluded events to a receiver could leak information
  about the presence of access control filters that might be in
  place for that receiver.

- "subscription": Different operational teams might have a desire to
  set varying subsets of subscriptions.  Access control should be
  designed to permit read access to just the allowed set.

Some of the RPC operations in this YANG module may be considered
sensitive or vulnerable in some network environments.  It is thus
important to control access to these operations.  These are the
operations and their sensitivity/vulnerability:

RPC: all

- If a malicious or buggy subscriber sends an unexpectedly large
  number of RPCs, the result might be an excessive use of system
  resources on the publisher just to determine that these
  subscriptions should be declined.  In such a situation,
  subscription interactions MAY be terminated by terminating the
  transport session.

RPC: "delete-subscription"

- No special considerations.

RPC: "establish-subscription"

- Subscriptions could overload a publisher's resources.  For this
  reason, publishers MUST ensure that they have sufficient resources
  to fulfill this request; otherwise, they MUST reject the request.

RPC: "kill-subscription"

- The "kill-subscription" RPC MUST be secured so that only
  connections with administrative rights are able to invoke
  this RPC.

RPC: "modify-subscription"

- Subscriptions could overload a publisher's resources.  For this
  reason, publishers MUST ensure that they have sufficient resources
  to fulfill this request; otherwise, they MUST reject the request.

## Security Considerations from RFC 8641

The YANG module specified in this document defines a schema for data
that is designed to be accessed via network management protocols such
as NETCONF [RFC6241] or RESTCONF [RFC8040].  The lowest NETCONF layer
is the secure transport layer, and the mandatory-to-implement secure
transport is Secure Shell (SSH) [RFC6242].  The lowest RESTCONF layer
is HTTPS, and the mandatory-to-implement secure transport is TLS
[RFC8446].

The Network Configuration Access Control Model (NACM) [RFC8341]
provides the means to restrict access for particular NETCONF or
RESTCONF users to a preconfigured subset of all available NETCONF or
RESTCONF protocol operations and content.

There are a number of data nodes defined in this YANG module that are
writable/creatable/deletable (i.e., config true, which is the
default).  These data nodes may be considered sensitive or vulnerable
in some network environments.  Write operations (e.g., edit-config)
to these data nodes without proper protection can have a negative
effect on network operations.  These are the subtrees and data nodes
and their sensitivity/vulnerability.  (It should be noted that the
YANG module defined in this document augments the YANG module defined
in [RFC8639].  All security considerations that are listed in

[RFC8639] are also relevant for datastore subscriptions.  In the
following list, we focus on the new data nodes that are introduced in
this document.)

- Subtree "selection-filter" under container "filters": This subtree
  allows a subscriber to specify which objects or subtrees to
  include in a datastore subscription.  An attacker could attempt to
  modify the filter.  For example, the filter might be modified to
  result in very few objects being filtered in order to attempt to
  overwhelm the receiver.  Alternatively, the filter might be
  modified to result in certain objects being excluded from updates,
  in which case certain changes would go unnoticed.

- Subtree "datastore" in choice "target" in list "subscription":
  Analogous to "selection filter", an attacker might attempt to
  modify the objects being filtered in order to overwhelm a receiver
  with a larger volume of object updates than expected or cause
  certain changes to go unnoticed.

- Choice "update-trigger" in list "subscription": By modifying the
  update trigger, an attacker might alter the updates that are being
  sent in order to confuse a receiver, withhold certain updates to
  be sent to the receiver, and/or overwhelm a receiver.  For
  example, an attacker might modify the period with which updates
  are reported for a periodic subscription, or it might modify the
  dampening period for an on-change subscription, resulting in a
  greater delay for successive updates (potentially affecting the
  responsiveness of applications that depend on the updates) or in a
  high volume of updates (to exhaust receiver resources).

The NACM provides one means to mitigate these threats on the
publisher side.  In order to address those threats as a subscriber,
the subscriber could monitor the subscription configuration for any
unexpected changes and subscribe to updates to the YANG datastore
nodes that represent its datastore subscriptions.  As this volume of
data is small, a paranoid subscriber could even revert to occasional
polling to guard against a compromised subscription against
subscription configuration updates itself.

Some of the readable data nodes in this YANG module may be considered
sensitive or vulnerable in some network environments.  It is thus
important to control read access (e.g., via get, get-config, or
notification) to these data nodes.  These are the subtrees and data
nodes and their sensitivity/vulnerability:

- Subtree "selection-filter" under container "filters": If access
  control is not properly configured, can expose system internals to
  those who should not have access to this information.

- Subtree "datastore" in choice "target" in list "subscription": If
  access control is not properly configured, can expose system
  internals to those who should not have access to this information.

- Choice "update-trigger" in list "subscription": If access control
  is not properly configured, can expose system internals to those
  who should not have access to this information.

Some of the RPC operations in this YANG module may be considered
sensitive or vulnerable in some network environments.  It is thus
important to control access to these operations.  These are the
operations and their sensitivity/vulnerability:

- RPC "resync-subscription": This RPC allows a subscriber of an
  on-change subscription to request a full push of objects in the
  subscription's scope.  This can result in a large volume of data.
  An attacker could attempt to use this RPC to exhaust resources on
  the server to generate the data and could then attempt to
  overwhelm a receiver with the resulting large volume of data.

# IANA Considerations

TODO - This document will need to register new YANG models with IANA.

## IANA Considerations from RFC 8639

IANA has registered one URI in the "ns" subregistry of the "IETF XML
Registry" [RFC3688] maintained at <https://www.iana.org/assignments/xml-registry>.  The following registration has been made per the
format in [RFC3688]:

URI: urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications
Registrant Contact: The NETCONF WG of the IETF.
XML: N/A; the requested URI is an XML namespace.

IANA has registered one YANG module in the "YANG Module Names"
registry [RFC6020] maintained at <https://www.iana.org/assignments/yang-parameters>.  The following registration has been made per the
format in [RFC6020]:

Name: ietf-subscribed-notifications
Namespace: urn:ietf:params:xml:ns:yang:ietf-subscribed-notifications
Prefix: sn
Reference: RFC 8639


## IANA Considerations from RFC 8641

   This document registers the following namespace URI in the "IETF XML Registry" [RFC3688]:

   URI: urn:ietf:params:xml:ns:yang:ietf-yang-push

   Registrant Contact: The IESG.

   XML: N/A; the requested URI is an XML namespace.

   This document registers the following YANG module in the "YANG Module
   Names" registry [RFC6020]:

   Name: ietf-yang-push

   Namespace: urn:ietf:params:xml:ns:yang:ietf-yang-push

   Prefix: yp

   Reference: RFC 8641

# Acknowledgments
{:numbered="false"}

This inital draft is early work is based on discussions with various folk, particularly Thomas Graf, Holger Keller, Dan Voyer, Nils Warnke, and Alex Huang Feng; but also wider conversations that include: Benoit Claise, Pierre Francois, Paolo Lucente, Jean Quilbeuf, among others.

--- back

# Combined YANG tree

This section shows the relevant subsets of the combined Subscribed Notification YANG trees that are augmented by the ietf-yp-ext.yang additions.

~~~~ yangtree
{::include yang/all.tree.txt}
~~~~
{: align="left" title="YANG tree for ietf-subscribed-notifications.yang with ietf-yang-push.yang and ietf-yp-ext.yang (and a couple of others)."}

# Example Configured Transport Augmentation (from RFC 8639)

This appendix provides a non-normative example of how the YANG module
defined in Section 4 may be enhanced to incorporate the configuration
parameters needed to support the transport connectivity process.
This example is not intended to be a complete transport model.  In
this example, connectivity via an imaginary transport type of "foo"
is explored.  For more on the overall objectives behind configuring
transport connectivity for a configured subscription, see
Section 2.5.7.

The YANG module example defined in this appendix contains two main
elements.  First is a transport identity "foo".  This transport
identity allows a configuration agent to define "foo" as the selected
type of transport for a subscription.  Second is a YANG case
augmentation "foo", which is made to the
"/subscriptions/subscription/receivers/receiver" node of Section 4.
In this augmentation are the transport configuration parameters
"address" and "port", which are necessary to make the connection to
the receiver.

~~~~ yang
module example-foo-subscribed-notifications {
  yang-version 1.1;
  namespace
    "urn:example:foo-subscribed-notifications";

  prefix fsn;

  import ietf-subscribed-notifications {
    prefix sn;
  }
  import ietf-inet-types {
    prefix inet;
  }

  description
    "Defines 'foo' as a supported type of configured transport for
    subscribed event notifications.";

  identity foo {
    base sn:transport;
    description
      "Transport type 'foo' is available for use as a configured
      subscription's transport protocol for subscribed
      notifications.";
  }

  augment
    "/sn:subscriptions/sn:subscription/sn:receivers/sn:receiver" {
    when 'derived-from(../../../transport, "fsn:foo")';
    description
      "This augmentation makes transport parameters specific to 'foo'
      available for a receiver.";
    leaf address {
      type inet:host;
      mandatory true;
      description
        "Specifies the address to use for messages destined for a
        receiver.";
    }
    leaf port {
      type inet:port-number;
      mandatory true;
      description
        "Specifies the port number to use for messages destined for a
        receiver.";
    }
  }
}

              Figure 21: Example Transport Augmentation
                  for the Fictitious Protocol "foo"
~~~~

This example YANG module for transport "foo" will not be seen in a
real-world deployment.  For a real-world deployment supporting an
actual transport technology, a similar YANG module must be defined.

# Subscription Errors (from RFC 8641)

## RPC Failures

Rejection of an RPC for any reason is indicated via an RPC error
response from the publisher.  Valid RPC errors returned include both
(1) existing transport-layer RPC error codes, such as those seen with
NETCONF in [RFC6241] and (2) subscription-specific errors, such as
those defined in the YANG data model.  As a result, how subscription
errors are encoded in an RPC error response is transport dependent.

References to specific identities in the ietf-subscribed-
notifications YANG module [RFC8639] or the ietf-yang-push YANG module
may be returned as part of the error responses resulting from failed
attempts at datastore subscription.  For errors defined as part of
the ietf-subscribed-notifications YANG module, please refer to
[RFC8639].  The errors defined in this document, grouped per RPC, are
as follows:

~~~~
      establish-subscription          modify-subscription
      ---------------------------     ---------------------
       cant-exclude                    period-unsupported
       datastore-not-subscribable      update-too-big
       on-change-unsupported           sync-too-big
       on-change-sync-unsupported      unchanging-selection
       period-unsupported
       update-too-big                 resync-subscription
       sync-too-big                   ----------------------------
       unchanging-selection            no-such-subscription-resync
                                       sync-too-big
~~~~

There is one final set of transport-independent RPC error elements
included in the YANG data model.  These are the four yang-data
structures for failed datastore subscriptions:

1.  yang-data "establish-subscription-error-datastore": This MUST be
    returned if information identifying the reason for an RPC error
    has not been placed elsewhere in the transport portion of a
    failed "establish-subscription" RPC response.  This MUST be sent
    if hints are included.

2.  yang-data "modify-subscription-error-datastore": This MUST be
    returned if information identifying the reason for an RPC error
    has not been placed elsewhere in the transport portion of a
    failed "modify-subscription" RPC response.  This MUST be sent if
    hints are included.

3.  yang-data "sn:delete-subscription-error": This MUST be returned
    if information identifying the reason for an RPC error has not
    been placed elsewhere in the transport portion of a failed
    "delete-subscription" or "kill-subscription" RPC response.

4.  yang-data "resync-subscription-error": This MUST be returned if
    information identifying the reason for an RPC error has not been
    placed elsewhere in the transport portion of a failed
    "resync-subscription" RPC response.

## Failure Notifications
A subscription may be unexpectedly terminated or suspended
independently of any RPC or configuration operation.  In such cases,
indications of such a failure MUST be provided.  To accomplish this,
a number of errors can be returned as part of the corresponding
subscription state change notification.  For this purpose, the
following error identities are introduced in this document, in
addition to those that were already defined in [RFC8639]:

~~~~
  subscription-terminated        subscription-suspended
  ---------------------------    ----------------------
  datastore-not-subscribable     period-unsupported
  unchanging-selection           update-too-big
                                  synchronization-size
~~~~

# Examples {#Examples}

Notes on examples:

- These examples have been given using a JSON encoding of the regular YANG-Push notification format, i.e., encoded using {{!RFC5277}}, but it is anticipated that these notifications could be defined to exclusively use the new format proposed by {{?I-D.netana-netconf-notif-envelope}}.

- Some additional meta data fields, e.g., like those defined in {{?I-D.tgraf-netconf-notif-sequencing}} would also likely be included, but have also been excluded to allow for slightly more concise examples.

- The examples include the {{?I-D.tgraf-netconf-yang-push-observation-time}} field for the existing YANG-Push Notification format, and the proposed equivalent "observation-time" leaf for the new update notification format.

- All these examples are created by hand, may contain errors, and may not parse correctly.

## Example of an {{RFC8641}} style push-update notification

This example illustrates a periodic update message for a subscription at "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces" that is returning data from two internal data providers, one of which is returning data for "interface-summary" and the other that is returning data for "interfaces".  The device resident management agents stitches the reply together from the two data providers into a single data tree before returning it in a single message.

For the periodic message to be correct, and to allow it to replace any previous periodic message published on that subscription, it must include **all** data below the subscription path.  This may increase the total amount of internal IPC within the device and make the timestamps less accurate, since the observation timestamp only reports when the device starts polling the data providers.  If those providers are distributed across multiple processes and linecards then it may take a bit of time to complete the periodic on-change.

~~~~
{
  "ietf-notification:notification": {
    "eventTime": "2024-09-27T14:16:27.773Z",
    "ietf-yang-push:push-update": {
      "id": 1,
      "ietf-yp-observation:timestamp": "2024-09-27T14:16:27.773Z",
      "ietf-yp-observation:point-in-time": "current-accounting",
      "datastore-contents": {
        "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces": {
          "interface-summary": {
            "interface-type": [
              {
                "interface-type-name": "IFT_GETHERNET",
                "interface-type-description": "GigabitEthernet",
                "interface-counts": {
                  "interface-count": 5,
                  "up-interface-count": 2,
                  "down-interface-count": 0,
                  "admin-down-interface-count": 3
                }
              }
            ],
            "interface-counts": {
              "interface-count": 8,
              "up-interface-count": 5,
              "down-interface-count": 0,
              "admin-down-interface-count": 3
            }
          },
          "interfaces": {
            "interface": [
              {
                "interface-name": "GigabitEthernet0/0/0/0",
                "interface": "GigabitEthernet0/0/0/0",
                "state": "im-state-admin-down",
                "line-state": "im-state-admin-down"
              },
              {
                "interface-name": "GigabitEthernet0/0/0/4",
                "interface": "GigabitEthernet0/0/0/4",
                "state": "im-state-admin-down",
                "line-state": "im-state-admin-down"
              },
            ]
          }
        }
      }
    }
  }
}
~~~~

## Example of periodic updates using the new style update message

The subscription was made on "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces", but for efficiency reasons, the device has split the subscription into separate child subscriptions for the different data providers, and makes use of the new message format.

Hence, this first periodic message is being published for the "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces/interface-summary" container, but it is encoded rooted relative to the schema for "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces".

~~~~
{
  "ietf-notification:notification": {
    "eventTime": "2024-09-27T14:16:27.773Z",
    "ietf-yp-ext:update": {
      "id": 1,
      "subscription-path": "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces",
      "target-path":
        "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces/interface-summary",
      "snapshot-type": "periodic"
      "observation-time": "2024-09-27T14:16:27.773Z",
      "datastore-snapshot": {
        "interface-summary" : {
          "interface-type": [
            {
              "interface-type-name": "IFT_GETHERNET",
              "interface-type-description": "GigabitEthernet",
              "interface-counts": {
                "interface-count": 5,
                "up-interface-count": 2,
                "down-interface-count": 0,
                "admin-down-interface-count": 3
              }
            }
          ],
          "interface-counts": {
            "interface-count": 8,
            "up-interface-count": 5,
            "down-interface-count": 0,
            "admin-down-interface-count": 3
          }
        }
      }
    }
  }
}
~~~~

The second periodic message is being published for the "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces/interfaces/interface" list, but again, it is encoded rooted relative to the schema for "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces".  This message has a separate observation time that represents the more accurate time that this periodic date was read.

~~~~
{
  "ietf-notification:notification": {
    "eventTime": "2024-09-27T14:16:27.973Z",
    "ietf-yp-ext:update": {
      "id": 1,
      "subscription-path": "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces",
      "target-path":
        "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces/interfaces/interface[]",
      "snapshot-type": "periodic"
      "observation-time": "2024-09-27T14:16:27.973Z",
      "datastore-snapshot": {
        "interfaces": {
          "interface": [
            {
              "interface-name": "GigabitEthernet0/0/0/0",
              "interface": "GigabitEthernet0/0/0/0",
              "state": "im-state-admin-down",
              "line-state": "im-state-admin-down"
            },
            {
              "interface-name": "GigabitEthernet0/0/0/4",
              "interface": "GigabitEthernet0/0/0/4",
              "state": "im-state-admin-down",
              "line-state": "im-state-admin-down"
            },
          ]
        }
      }
    }
  }
}
~~~~

Each child subscription would use the same period and anchor time as the configured subscription, possibly with a little bit of initial jitter to avoid all daemons attempting to publish the data at exactly the same time.

## Example of an on-change-update notification using the new style update message

If the subscription is for on-change notifications, or periodic-and-on-change-notifications, then, if the interface state changed (specifically if the 'state' leaf of the interface changed state), and if the device was capable of generating on-change notifications, then you may see the following message.  A few points of notes here:

- The on-change notification contains **all** of the state at the "target-path"

  - Not present in the below example, but if the notification excluded some state under an interfaces list entry (e.g., the line-state leaf) then this would logically represent the implicit deletion of that field under the given list entry.

  - In this example it is restricted to a single interface. It could also publish an on-change notification for all interfaces, by indicating a target-path without any keys specified.  TODO - Can it represent notifications for a subset of interfaces?

- The schema of the change message is exactly the same as for the equivalent periodic message.  It doesn't use the YANG Patch format {{?RFC8072}} for on-change messages.

- The "observation time" leaf represents when the system first observed the on-change event occurring.

- The on-change event doesn't differentiate the type of change to operational state.  The on-change-update snapshot type is used to indicate the creation of a new entry or some update to some existing state.  Basically, the message can be thought of as the state existing with some current value.

~~~~
{
  "ietf-notification:notification": {
    "eventTime": "2024-09-27T14:16:30.973Z",
    "ietf-yp-ext:update": {
      "id": 1,
      "subscription-path": "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces",
      "target-path":
        "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces/interfaces/
         interface[interface=GigabitEthernet0/0/0/0]",
      "snapshot-type": "on-change-update"
      "observation-time": "2024-09-27T14:16:30.973Z",
      "datastore-snapshot": {
        "interfaces": {
          "interface": [
            {
              "interface-name": "GigabitEthernet0/0/0/0",
              "interface": "GigabitEthernet0/0/0/0",
              "state": "im-state-up",
              "line-state": "im-state-up"
            }
          ]
        }
      }
    }
  }
}
~~~~

## Example of an on-change-delete notification using the new style update message

If the interface was deleted, and if the system was capable of reporting on-change events for the delete event, then an on-change delete message would be encoded as per the following message.  Of note:

- The on-change-delete snapshot type doesn't include a "datastore-snapshot", instead it represents a delete of the list entry at the path identified by the target-path, which is similar to a YANG Patch delete notification.

~~~~
{
  "ietf-notification:notification": {
    "eventTime": "2024-09-27T14:16:40.973Z",
    "ietf-yp-ext:update": {
      "id": 1,
      "subscription-path": "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces",
      "target-path":
        "Cisco-IOS-XR-pfi-im-cmd-oper:interfaces/interfaces/
         interface[interface=GigabitEthernet0/0/0/0]",
      "snapshot-type": "on-change-delete"
      "observation-time": "2024-09-27T14:16:40.973Z",
    }
  }
}
~~~~
