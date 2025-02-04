---
title: "YANG Datastore Telemetry (YANG Push Lite)"
abbrev: "YANG-Push Lite"
category: std

docname: draft-wilton-netconf-yang-push-lite-latest
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
  RFC6241:
  RFC6991:
  RFC7950:
  RFC7951:
  RFC8340:
  RFC8341:
  RFC8342:
  RFC8525:
  RFC8529:
  RFC8791:
  RFC9254:
  RFC9595:


informative:
  RFC3411:
  RFC3688:
  RFC4252:
  RFC6020:
  RFC6536:
  RFC7049:
  RFC7540:
  RFC8259:
  RFC8343:
  RFC8446:
  RFC8040:
  RFC8071:
  RFC8072:
  RFC8639:
  RFC8640:
  RFC8641:
  RFC9000:
  RFC9535:
  I-D.draft-ietf-netmod-rfc8407bis:
  I-D.ietf-nmop-network-anomaly-architecture:
  I-D.ietf-nmop-yang-message-broker-integration:
  I-D.draft-ietf-netconf-http-client-server:
  I-D.draft-ietf-netconf-udp-notif:
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
  XPATH:
    target: https://www.w3.org/TR/1999/REC-xpath-19991116/
    title: XML Path Language (XPath) Version 1.0
    author:
      - name: W3C
    date: false
  gNMI:
    target: https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md
    title: gRPC Network Management Interface (gNMI)
    author:
      - name: OpenConfig
    date: false

--- abstract

YANG Push Lite is a YANG datastore telemetry solution, as an alternative specification to the Subscribed Notifications and YANG Push solution, specifically optimized for the efficient observability of operational data.

--- middle

# Document Status

*RFC Editor: If present, please remove this section before publication.*

Based on the feedback received during the IETF 121 NETCONF session, this document has currently been written as a self-contained lightweight protocol and document replacement for {{RFC8639}} and {{RFC8641}}, defining a separate configuration data model.

# Acknowledgement to the authors of RFC 8639 and RFC 8641

This document is intended to be a lightweight alternative for {{RFC8639}} and {{RFC8641}}, but it intentionally reuses substantial parts of the design and data model of those RFCs.

For ease of reference, this document, sourced much of the starting text and basis for the *ietf-yp-lite* YANG model directly from those RFCs, rather than either starting from scratch or creating a separate document that would contain numerous references back to sections in those RFCs and correspondingly would be hard to read and follow.

Hence, the authors of this draft would like to sincerely thank and acknowledge the very significant previous effort put into those RFCs by authors, contributors and reviewers.  We would particularly like to thank the authors: Eric Voit, Alex Clemm, Alberto Gonzalez Prieto, Einar Nilsen-Nygaard, Ambika Prasad Tripathy, but also everyone who contributed to the underlying work from which this document is derived.

# Summary of Open Issues & Potential Enhancements

This section lists some other potential issues and enhancements that should be discussed and considered as part of this work.  If there is continued interest in this work, i.e., it becomes adopted, then these should move to github issues.

**At the moment, this list also reports more of the aspects that have changed relative to YANG Push, but the intention is that this would quickly be removed to only list open issues for discussion.  Please see {{DifferencesFromYangPush}} for a definitive list of how this differs compared to YANG Push.**

1. Further refinements on how a subscription can be decomposed internally into child subscriptions with the data returned for each child subscription:

   - Handling lists with separate producers of list entries.

   - If a subscription is decomposed, then should the subscription started message for the configured subscription indicate how that subscription has been decomposed?

   - Do we need to add an optional replay-end (i.e., after a sync-on-start) or collection-end (i.e., after every collection) notification so that clients can determine when data can be implicitly deleted.  (Rob: I think that we should add the latter, but make it a subscription config option to turn it on).

1. Constraints on supporting multiple receivers:

   - Should they all have the same encoding? (I think so, the client can always set up different subscriptions if different receivers and subscriptions if different encodings are needed.)

   - Should we also constrain that they have the same transports? (Rob: Probably not)

   - Lifecycle message should be sent per subscription, not per receiver (i.e., every receiver gets the same message (ignoring transport headers)):

     - Hence, every receiver gets the same sequence number in the message.  Otherwise, if separate lifecycle messages are sent to individual receivers then either those messages need to exclude sequence numbers of the sequence numbers will go out of sync (which I think will break the reason why Thomas introduced them in the first place).

     - Or perhaps receiver removed can be a special case and a subscription-terminated is only sent to the receiver that is removed (because the sequence number doesn't matter - since it won't receive any new messages until another subscription-started).

     - subscription-created notification could perhaps have an enum giving a reason for the notification (e.g., new subscription, receiver added).

1. Questions on subscription lifecycle management:

   - The draft has already removed the suspend/resume logic and associated messages.

   - The draft has already removed subscription-modified, and just kept subscription-created.

   - Should subscription-started notification include a fingerprint of the schema that is covered by the subscription that would guaranteed to change if the subscription changes?  Would this also be impacted by a change to access control? (Rob: Probably not)

   - If a subscription references a filter, then should that be included inline in the subscription started notification (as per the RFC 8641 text), or should it indicate that it is a referenced filter?

   - When a subscription is terminated, should it be MUST NOT send any more notifications after the terminated message, or SHOULD NOT?  For a dynamic subscription, should the RPC be synchronous and not reply until it knows that all queues have been drained?

   - Is a publisher allowed to arbitrarily send a sync-on-start resync, e.g., if it detects data loss, or should it always just terminate and reinitialize the susbcription?

1. Questions/comments on the notification message format:

   - periodic and on-change notifications use the same new single update message.

   - We also mandate use of the new envelope draft for encapsulating the notifications.  Do we want to REQUIRE use of hostname & sequence-number? (I think we do).

   - We allow a subscription to be decomposed into more specific subscriptions which are then used for the notification.

   - We allow multiple updates within a single message (primary use case is for the on-change case).  What about the timestamp, which is still just once per message (like gNMI)?  Should message bundling be optional/configurable to implement (if they all use a single shared timestamp)?

   - on-change deletes are implicit by an update that replaces an existing entry witha empty data node (e.g., "{}" in JSON)

   - The update message also currently includes a path-prefix to allow (like gNMI) so that they don't necessarily need to be encoded from root, specifically, I think that this makes on-change messages nicer, since the on-change is rooted to the thing that is changing rather than the root of the tree.  We need to define semantics of how this works, e.g., this should probably be controlled via configuration.

      - This prefix path should use something like the NACM node-instance-identifier {{RFC6536}} but JSONified (i.e., closer to the JSON style encoding of instance identifier {{RFC7951}}), which should perhaps be done as separate draft, so it can be discussed independently)

1. Questions related to terminology:

   - Should we use the object terminology?  This may be better than data node subtree, or the equivalent, and could be better than introducing *bags*?

1. Questions/issues related to the configuration model:

   - Note some of these apply or impact dynamic subscriptions as well.

   - YP Lite is somewhat different (separate namespace, separate receivers, no event filters, some config has moved to a separate receivers list.)  See the data model and {{DifferencesFromYangPush}}.

   - We should allow devices to limit which datastores subscriptions can be made against (e.g., not candidate or factory-default as some obvious examples).  Should these be advertised in the capabilities?

   - Some other changes/proposed changes:

      - I want to reduce the number of features.  Also, in some cases we have both features are capability flags in oper.  Is is okay to have both.

      - I've renamed the encodings (e.g., from "encode-json" to just "json")

      - Probably want to get rid of the reset RPC.

      - Maybe further simplification of the receivers list under the subscription.  E.g., do we need stats per subscription per receiver, or just per subscription?  Do want stats across all subscriptions to a given receiver?

      - Subscription-ids are currently numeric values with the space split between configured and dynamic subscriptions, but I think that the config model would be cleaner if we used names for the configured subscriptions (and we could reserve a prefix "dyn-" for dynamic subscriptions).

      - Should DSCP marking be configured under the receiver or the subscription?  Or perhaps in both places with DSCP marking at the subscription overriding a default set on the receiver?

1. Questions/issues related to dynamic subscriptions:

   - In YP Lite, dynamic subscriptions are designed to be closer to configured subscriptions and share more of the data model and lifecycle handling.  I.e., the primary differentiator is meant to be how they are instantiated.

   - Do we want to change how RPC errors are reported?  E.g., change the RPC ok response to indicate whether the subscription was successfully created or not, or included extra error information.  Note NETCONF and RESTCONF already define how errors are encoded in XML and JSON (for RESTCONF only).

   - Do we need to allow a dynamic subscription to be modified?  If we do, then it would be better to change the establish-subscription RPC to have an optional existing subscription-id rather than define a separate RPC.  However, my preference is that the existing subscription is deleted and recreated (or if we allow the client to specify the subscription-id then they could just overwrite the subscription)

1. Questions related to implementation conformance:

   - Should we mandate that all implementations MUST support configured subscriptions? Currently the text indicates that both configured and dynamic are optional and only one must be implemented.

   - For on-change, should a subscription be rejected (or not brought up) if there are no on-change notifiable nodes?  Alternative is to offer implementation flexibility between these two approaches.

1. Issues/questions related to path filtering:

   - I've introduced a new basic path filter for selecting filter paths for a subscription.  I.e., intended to represent the actual cut down version of xpaths that implementations use rather than full Xpath.  Do we want to mandate support for this?

   - I've currently retained subtree filtering as the more advance form of filtering.

   - I would propose that we remove support for XPath filtering (or otherwise, it should definitely go under a feature statement), and the draft should be clear that it is optional, and may have heavy performance issues, and implementations may end up not supporting the full subset of XPath.

1. Issues/questions related to security and NACM filtering:

   - Need to consider how NACM applies to YANG Push Lite, which may differ for dynamic vs configured subscription, but generally we want the permissions to be checked when the subscription is created rather than each time a path is accessed.

   - Where should this be in the document (current it in the )

1. Issues/questions related to operational data:

   - Should we define some additional operational data to help operators check that the telemetry infrastructure is performing correctly, to get an approximation of the load, etc. (Rob: probably, but lower priority)

1. Issues/questions related to capabilties:

   - Should this use a separate capabilities subtree from Yang Push (Rob: Probably)

   - Should we define a base model in this draft for the capabilities (Rob: Probably, based on the existing capabilities but in a new namespace/path).

1. Process related issues/questions:

   - If this work progresses we want to create bis versions of the transports so that they augment into the new data model paths.  Drafts that would need to be updated:

       - {{I-D.draft-ietf-netconf-udp-notif}} - only to augment new receiver path (and capabilities?)
       - {{I-D.draft-ietf-netconf-http-client-server}} - only to augment new receiver path (and capabilities?)

   - Do we need to fold in any text from RFC 8640? and RESTCONF.  I.e., there was this text in the one of the previous docs:   Bindings for subscribed event record delivery for NETCONF and RESTCONF are defined in {{RFC8640}} and [RESTCONF-Notif], respectively.

1. Examples related issues/questions:

   - We need to update the examples to reflect changes in the models.

# Conventions {#conventions}

{::boilerplate bcp14-tagged}

All *YANG tree diagrams* used in this document follow the notation
defined in {{RFC8340}}.

# Introduction

{{I-D.ietf-nmop-yang-message-broker-integration}} describes an architecture for how YANG datastore telemetry, e.g., {{RFC8641}}, can be integrated effectively with message brokers, e.g., {{Kafka}}, that forms part of a wider architecture for a *Network Anomaly Detection Framework*, specified in {{I-D.ietf-nmop-network-anomaly-architecture}}.

This document specifies "YANG Push Lite", an lightweight alternative to Subscribed Notifications {{RFC8639}} and YANG Push {{RFC8641}}. YANG Push Lite is a separate YANG datastore telemetry solution, which can be implemented independently or, if desired, alongside {{RFC8639}} and {{RFC8641}}.

At a high level, YANG Push Lite is designed to solve a similar set of requirements as YANG Push, and it reuses a significant subset of the ideas and base solution from YANG Push.  YANG Push Lite defines a separate data model to allow concurrent implementation of both protocols and to facilitate more significant changes in behavior, but many of the data nodes are taken from YANG Push and have the same, or very similar definitions.

The following sections give the background for the solution, and highlight the key ways that this specification differs from the specifications that it is derived from.

## Background and Motivation for YANG Push Lite

A push based telemetry solution, as described both in this document and also the YANG Push solution described by {{RFC8639}} and {{RFC8641}}, is beneficial because it allows operational data to be exported by publishers more immediately and efficiently compared to legacy poll based mechanisms, such as SNMP {{RFC3411}}.  Some further background information on the general motivations for push based telemetry, which equally apply here, can be found in the *Motivation* (section 1.1) of {{RFC8639}} and the Introduction (section 1) of {{RFC8641}}.  The remainder of this section is focused on the reasons why a new lightweight version of YANG Push has been specified, and what problems is aims to solve.

Early implementation efforts of the {{I-D.ietf-nmop-yang-message-broker-integration}} architecture hit issues with using either of the two common YANG datastore telemetry solutions that have been specified, i.e., {{gNMI}} or YANG Push {{RFC8641}}.

gNMI is specified by the OpenConfig Industry Consortium.  It is more widely implemented, but operators report that some inter-operability issues between device implementations cause problems.  Many of the OpenConfig protocols and data models are also expected to evolve more rapidly than IETF protocols and models - that are expected to have a more gradual pace of evolution once an RFC has been published.

YANG Push {{RFC8641}} was standardized by the IETF in 2019, but market adoption has been rather slow.  During 2023/2024, when vendors started implementing, or considering implementing, YANG Push, it was seen that some design choices for how particular features have been specified in the solution make it expensive and difficult to write performant implementations, particularly when considering the complexities and distributed nature of operational data.  In addition, some design choices of how the data is encoded (e.g., YANG Patch {{RFC8072}}) make more sense when considering changes in configuration data but less sense when the goal is to export a subset of the operational data off the device in an efficient fashion for both devices (publishers) and clients (receivers).

Hence, during 2024, the vendors and operators working towards YANG telemetry solutions agreed to a plan to implement a subset of {{RFC8639}} and {{RFC8641}}, including common agreements of features that are not needed and would not be implemented, and deviations from the standards for some aspects of encoding YANG data.  In addition, the implementation efforts identified the minimal subset of functionality needed to support the initial telemetry use cases, and areas of potential improvement and optimization to the overall YANG Push telemetry solution (which has been written up as a set of small internet drafts that augment or extend the base YANG Push solution).

Out of this work, consensus was building to specify a cut down version of Subscribed Notifications {{RFC8639}} and YANG Push {{RFC8641}} that is both more focussed on the operational telemetry use case, and is also easier to implement, by relaxing some of the constraints on consistency on the device, and removing, or simplifying some of the operational features.  This has resulted in this specification, YANG Push Lite.

The implementation efforts also gave arise to potential improvements to the protocol and encoding of notification messages.

## Complexities in Modelling the Operational State Datastore {#OperationalModellingComplexities}

The YANG abstraction of a single datastore of related consistent data works very well for configuration that has a strong requirement to be self consistent, and that is always updated, and validated, in a transactional way.  But for producers of telemetry data, the YANG abstraction of a single operational datastore is not really possible for devices managing a non-trivial quantity of operational data.

Some systems may store their operational data in a single logical database, yet it is less likely that the operational data can always be updated in a transactional way, and often for memory efficiency reasons such a database does not store individual leaves, but instead semi-consistent records of data at a container or list entry level.

For other systems, the operational information may be distributed across multiple internal nodes (e.g., linecards), and potentially many different process daemons within those distributed nodes.  Such systems generally do not, and cannot, exhibit full consistency {{Consistency}} of the operational data (which would require transactional semantics across all daemons and internal nodes), only offering an eventually consistent {{EventualConsistency}} view of the data instead.

In practice, many network devices will manage their operational data as a combination of some data being stored in a central operational datastore, and other, higher scale, and potentially more frequently changing data (e.g., statistics or FIB information) being stored elsewhere in a more memory efficient and performant way.

## Complexities for Consumers of YANG Push Data

For the consumer of the telemetry data, there is a requirement to associate a schema with the instance-data that will be provided by a subscription.  One approach is to fetch and build the entire schema for the device, e.g., by fetching YANG library, and then use the subscription XPath to select the relevant subtree of the schema that applies only to the subscription.  The problem with this approach is that if the schema ever changes, e.g., after a software update, then it is reasonably likely of some changes occurring with the global device schema even if there are no changes to the schema subtree under the subscription path.  Hence, it would be helpful to identify and version the schema associated with a particular subscription path, and also to encoded the instance data relatively to the subscription path rather than as an absolute path from the root of the operational datastore.

**TODO More needs to be added here, e.g., encoding, on-change considerations.  Splitting subscriptions up.**

This document proposes a new opt-in YANG-Push encoding format to use instead of the "push-update" and "push-change-update" notifications defined in {{RFC8641}}.

1. To allow the device to split a subscription into smaller child subscriptions for more efficient independent and concurrent processing.  I.e., reusing the ideas from {{?I-D.ietf-netconf-distributed-notif}}.  However, all child subscriptions are still encoded from the same subscription point.

### Combined periodic and on-change subscription

Sometimes it is helpful to have a single subscription that covers both periodic and on-change notifications (perhaps with dampening).

There are two ways in which this may be useful:

1. For generally slow changing data (e.g., a device's physical inventory), then on-change notifications may be most appropriate.  However, in case there is any lost notification that isn't always detected, for any reason, then it may also be helpful to have a slow cadence periodic backup notification of the data (e.g., once every 24 hours), to ensure that the management systems should always eventually converge on the current state in the network.

1. For data that is generally polled on a periodic basis (e.g., once every 10 minutes) and put into a time series database, then it may be helpful for some data trees to also get more immediate notifications that the data has changed.  Hence, a combined periodic and on-change subscription, potentially with some dampening, would facilitate more frequent notifications of changes of the state, to reduce the need of having to always wait for the next periodic event.

Hence, this document introduces the fairly intuitive "periodic-and-on-change" update trigger that creates a combined periodic and on-change subscription, and allows the same parameters to be configured.  For some use cases, e.g., where a time-series database is being updated, the new encoding format proposed previously may be most useful.

## Functional changes between YANG Push Lite and YANG Push {#DifferencesFromYangPush}

This non-normative section highlights the significant functional changes where the YANG Push Lite implementation differs from YANG Push.  However, in all cases, the text in this document, from {{overview}} onwards, provides the normative definition of the YANG Push Lite specification, except for any text or sections that explicitly indicate that they are informative rather being normative.

*Note to reviews (RFC editor please remove this section before publication): If you notice mistakes in this section during development of the document and solution then please point them out to the authors and the working group.*

### Changed Functionality

This section documents behavior that exist in both YANG Push and YANG Push Lite, but the behavior differs between the two:

- All YANG Push Lite notifications messages use {{I-D.draft-netana-netconf-notif-envelope}} rather than {{RFC5277}} used by YANG Push.  This does not affect other message streams generated by the device (e.g., YANG Push), that still generate {{RFC5277}} compliant messages.

- Changes to handling receivers:

  - Receivers are always configured separately from the subscription and are referenced.

  - Transport and Encoding parameters are configured as part of a receiver definition, and are used by all subscriptions directed towards a given receiver.

  - If a subscription uses multiple receivers then:

    - proposal: all updates and lifecycle events are sent to all receivers (to preserve sequence numbers)

    - all receivers must be configured with the same encoding

    - **TODO, perhaps all receivers use transport and DSCP marking?**

- Periodic and on-change message uses a common *update* notification message format, allowing for the messages to be processed by clients in a similar fashion and to support combined periodic and on-change subscriptions.

- On-change dampening.  Rather than dampening being controlled by the client, the publisher is allows to rate-limit how frequently on-change events may be delivered for a particular data node that is changing rapidly.  In addition, if the state of a data node changes and then reverts back to the previous state within a dampening period, then the publisher is not required to notify the client of the change, it can be entirely suppressed. See {{OnChangeConsiderations}} for further details.

- Dynamic subscriptions are no longer mandatory to implement, either or both of Configured and Dynamic Subscriptions may be implemented in YANG Push Lite.

- The solution focuses solely on datastore subscriptions that each have their own event stream.  Filters cannot be applied to the event stream, only to the set of datastore data nodes that are monitored by the subscription.

- The lifecycle events of when a subscription-started or subscription-terminated may be sent differs from RFC 8639/RFC 8649:

  - Subscription-started notifications are also sent for dynamic subscriptions.

- Some of the requirements on transport have been relaxed.

- The encoding identities have been extended with CBOR encodings, and the "encoding-" prefix has been removed (so that there is a better on the wire representation).

### Removed Functionality

This section lists functionality specified in {{RFC8639}} and YANG Push which is not specified in YANG Push Lite.

- Negotiation and hints of failed subscriptions.

- The RPC to modify an existing dynamic subscription, instead the subscription must be terminated and re-established.

- The ability to suspend and resume a dynamic subscription.  Instead a dynamic subscription is terminated if the device cannot reliably fulfill the subscription or a receiver is too slow causing the subscription to be back pressured.

- Specifying a subscription stop time, and the corresponding subscription-completed notification have been removed.

- Replaying of buffered event records are not supported.  The nearest equivalent is requesting a sync-on-start replay when the subscription transport session comes up which will reply the current state.

- QoS weighting and dependency between subscriptions has been removed due to the complexity of implementation.

- Support for reporting subscription error hints has been removed.  The device SHOULD reject subscriptions that are likely to overload the device, but more onus is placed on the operator configuring the subscriptions or setting up the dynamic subscriptions to ensure that subscriptions are reasonable, as they would be expected to do for any other configuration.

- The "subscription-state-notif" extension has been removed.

- The YANG Patch format {{RFC8072}} is no longer used for on-change subscriptions.

### Added Functionality

- Device capabilities are reported via XXX and additional models that augment that data model.

- A new *update* message:

  - Covers both on-change and periodic events.

  - Allows multiple updates to be sent in a single message (e.g., for on-change).

  - Allows for a common path prefix to be specified, with any paths and encoded YANG data to be encoded relative to the common path prefix.

- TODO - More operational data on the subscription load and performance.

# YANG Push Lite Overview {#overview}

This document specifies a lightweight telemetry solution that provides a subscription service for updates to the state and changes in state from a chosen datastore.

Subscriptions specify when notification messages (also referred to as *updates*) should be sent, what data to include in the update records, and where those notifications should be sent.

A YANG Push lite subscription comprises:

- a target datastore for the subscription, as per {{RFC8342}}.

- a set of selection filters to choose which datastore nodes the subscription is monitoring or sampling, as described in {{pathsAndFilters}}.

- a choice of how update event notifications for a datastore's data nodes are triggered.  I.e., either periodic sampling of the current state, on-change event-driven, or both.  These are described in **TODO, add reference**.

- a chosen encoding of the messages, e.g., JSON, CBOR.

- a set of one or more receivers for which datastore updates and subscription notifications are sent, as described in {{receivers}};
  - for configured subscriptions, the receivers parameters are configured, and specify transport, receiver, and encoding parameters.
  - for dynamic subscriptions, the receiver uses the same transport session on which the dynamic subscription has been created.

If a subscription is valid and acceptable to the publisher, and if a suitable connection can be made to one or more receivers associated with a subscription, then the publisher will enact the subscription, periodically sampling or monitoring changes to the chosen datastore's data nodes that match the selection filter.  Push updates are subsequently sent by the publisher to the receivers, as per the terms of the subscription.

Subscriptions may be set up in two ways: either through configuration - or YANG RPCs to create and manage dynamic subscriptions.  These two mechanisms are described in {{ConfiguredAndDynamic}}.

Changes to the state of subscription are notified to receivers as subscription lifecycle notifications.  These are described in {{LifecycleNotifications}}.

NACM {{RFC8341}} based access control is used to ensure the receivers only get access to the information for which they are allowed.  This is further described in {{security}}.

While the functionality defined in this document is transport agnostic, transports like the Network Configuration Protocol (NETCONF) {{RFC6241}} or RESTCONF {{RFC8040}} can be used to configure or dynamically signal subscriptions.  In the case of configured subscription, the transport used for carrying the subscription notifications is entirely independent from the protocol used to configure the subscription, and other transports, e.g., {{I-D.draft-ietf-netconf-udp-notif}} defines a simple UDP based transport for Push notifications. Transport considerations are described in {{transports}}.

**TODO Introduce capabilities and operational monitoring**

This document defines a YANG data model, that includes RPCs and notifications, for configuring and managing subscriptions and associated configuration, and to define the format of a *update* notification message.  The YANG model is defined in {{yp-lite-yang-module}} and associated tree view in {{yp-lite-tree}}.  The YANG data model defined in this document conforms to the Network Management Datastore Architecture defined in [RFC8342].

## Relationship to RFC 5277

All of the notifications defined in this specification, i.e., both the datastore update message and subscription lifecycle update notifications ({{LifecycleNotifications}}) use the notification envelope format defined in {{I-D.draft-netana-netconf-notif-envelope}}.

As such, this specification does not make use of the notification format defined in {{RFC5277}}, but devices may also use {{RFC5277}} notifications for other YANG notifications, e.g., for the "NETCONF" event stream defined in {{RFC5277}}.

# Definitions {#terminology}

This document reuses the terminology defined in {{RFC7950}}, {{RFC8341}}, {{RFC8342}}, {{RFC8639}} and {{RFC8641}}.

**TODO, if this document progresses, we should check which of this terminology we actually use/re-use, and what new terminology should be introduced in this document.  And also an action to check that it is used consistently.**

The following terms are taken from {{RFC8342}}:

- *Datastore*: A conceptual place to store and access information.  A datastore might be implemented, for example, using files, a database, flash memory locations, or combinations thereof.  A datastore maps to an instantiated YANG data tree.

- *Client*: An entity that can access YANG-defined data on a server, over some network management protocol.

- *Configuration*: Data that is required to get a device from its initial default state into a desired operational state.  This data is modeled in YANG using "config true" nodes.  Configuration can originate from different sources.

- *Configuration datastore*: A datastore holding configuration.

The following terms are taken from {{RFC8639}}:

- *Configured subscription*: A subscription installed via configuration into a configuration datastore.

- *Dynamic subscription*: A subscription created dynamically by a subscriber via a Remote Procedure Call (RPC).

- *Event*: An occurrence of something that may be of interest.  Examples include a configuration change, a fault, a change in status, crossing a threshold, or an external input to the system.

- *Event occurrence time*: A timestamp matching the time an originating process identified as when an event happened.

- *Event record*: A set of information detailing an event.

- *Event stream*: A continuous, chronologically ordered set of events aggregated under some context.

- *Event stream filter*: Evaluation criteria that may be applied against event records in an event stream.  Event records pass the filter when specified criteria are met.

- *Notification message*: Information intended for a receiver indicating that one or more events have occurred.

- *Publisher*: An entity responsible for streaming notification messages per the terms of a subscription.

- *Receiver*: A target to which a publisher pushes subscribed event records.  For dynamic subscriptions, the receiver and subscriber are the same entity.

- *Subscriber*: A client able to request and negotiate a contract for the generation and push of event records from a publisher.  For dynamic subscriptions, the receiver and subscriber are the same entity.

The following terms are taken from {{RFC8641}}:

- *Datastore node*: A node in the instantiated YANG data tree associated with a datastore.  In this document, datastore nodes are often also simply referred to as "objects".

- *Datastore node update*: A data item containing the current value of a datastore node at the time the datastore node update was created, as well as the path to the datastore node.

- *Datastore subscription*: A subscription to a stream of datastore node updates.

- *Datastore subtree*: A datastore node and all its descendant datastore nodes.

- *On-change subscription*: A datastore subscription with updates that are triggered when changes in subscribed datastore nodes are detected.

- *Periodic subscription*: A datastore subscription with updates that are triggered periodically according to some time interval.

- *Selection filter*: Evaluation and/or selection criteria that may be applied against a targeted set of objects.

- *Update record*: A representation of one or more datastore node updates.  In addition, an update record may contain which type of update led to the datastore node update (e.g., whether the datastore node was added, changed, or deleted).  Also included in the update record may be other metadata, such as a subscription ID of the subscription for which the update record was generated.  In this document, update records are often also simply referred to as "updates".

- *Update trigger*: A mechanism that determines when an update record needs to be generated.

- *YANG-Push*: The subscription and push mechanism for datastore updates that is specified in {{RFC8641}}.

This document introduces the following terms:

- *Subscription*: A registration with a publisher, stipulating the information that one or more receivers wish to have pushed from the publisher without the need for further solicitation.

- *Subscription Identifier*: A numerical identifier for a configured or dynamic subscription.  Also referred to as the subscription-id.

- *YANG-Push-Lite*: The light weight subscription and push mechanism for datastore updates that is specified in this document.

All *YANG tree diagrams* used in this document follow the notation defined in {{RFC8340}}.

# Sensor paths and selection filters {#pathsAndFilters}

A key part of a subscription is to select which data nodes should be monitored, and so a subscription must specify both the selection filters and the datastore against which these selection filters will be applied.  This information is used to choose, and subsequently push, *update* notifications from the publisher's datastore(s) to the subscription's receiver(s).

Filters can either be defined inline within a configured subscription ({{SubscriptionYangTree}}), a dynamic subscription's *establish-subscription* RPC ({{EstablishSubscriptionYangTree}}), or as part of the *datastore-telemetry/filters* container ({{FilterContainerYangTree}}) which can then be referenced from a configured or dynamic subscription.

The following selection filter types are included in the YANG Push Lite data model and may be applied against a datastore:

- *paths*: A list of basic path selection filters that, as a very restricted subset of an XPath filter, defines a path to a subtree of data nodes in the schema.  For each YANG list element on the path, the list keys may either be: (i) wildcarded to allow any value, (ii) specify a value as an exactly match for a key, or (iii) have a regex expression to match a subset of list entries. **TODO, do we also want a basic form of inclusion/exclusion of elements at the end of the filter?**

- *subtree*: A subtree selection filter identifies one or more datastore subtrees.  When specified, *update* records will only include the datastore nodes of selected datastore subtree(s).  The syntax and semantics correspond to those specified in {{RFC6241}}, Section 6.

- *xpaths*: A list of *xpath* selection filter is a full XPath expression that returns a node set.  (XPath is a query language for selecting nodes in an XML document; see {{XPATH}} for details.)  When specified, updates will only come from the selected datastore nodes that match the XPath expression.

These filters are used as selectors that define which data nodes fall within the scope of a subscription.  A publisher MUST support basic path filters, and MAY also support subtree or xpath filters.  **TODO, do we really need full XPath filters, does anyone intend to support this?  I thought that many implements only support a limited subset much closer to the new 'paths' definition above.**

XPath itself provides powerful filtering constructs, and care must be used in filter definition.  Consider an XPath filter that only passes a datastore node when an interface is up.  It is up to the receiver to understand the implications of the presence or absence of objects in each update.

For both path and xpath based filters, each filter may define a list of paths/xpaths.  Each of these filter paths is processed by the publisher independently, and if two or more filter paths end up selecting overlapping data nodes then the publisher MAY notify duplicate values for those data nodes, but the encoded data that is returned MUST always be syntactically valid, i.e., as per section 5.3 of {{RFC8342}}.

## The "filters" Container

The "filters" container maintains a list of all datastore subscription filters that persist outside the lifecycle of a single subscription.  This enables predefined filters that may be referenced by more than one configured or dynamic subscription.

Below is a tree diagram for the "filters" container.  All objects contained in this tree are described in the YANG module in {{ietf-yp-lite-yang}}.

~~~~ yangtree
{::include tree-output/filters.txt}
~~~~
{: align="left" title="'datastore-telemetry/filters' container", #FilterContainerYangTree }

## Decomposing Subscription Filters

In order to address the issues described in {{OperationalModellingComplexities}}, YANG Push Lite allows for publishers to send subtrees of data nodes in separate *update* notifications, rather than requiring that the subscription data be returned as a single datastore update covering all data nodes matched by the subscription filter.  This better facilitates publishers that internally group some of their operational data fields together into larger structures for efficiency (referred to as an *object*), and avoids the publishers or receivers having to consume potentially very large notification messages.  For example, each entry in the */ietf-interfaces:interface/interface* list could be represents as an object of data internally within the publisher.  In essence, a client specified subscription filter can be decomposed by a publisher into more specific, non-overlapping, filters, that are then used to return the data.

In particular:

1. A Publisher MAY decompose a client specified subscription filter path into a set of non-overlapping subscription filter paths that collectively cover the same data.  The publisher is allowed to return data for each of these decomposed subscription filter paths in separate update messages, and with separate, perhaps more precise, timestamps.

1. A Publisher MAY split large lists into multiple separate update messages, each with separate timestamps.  E.g., if a device has 10,000 entries in a list, it may return them in a single response, or it may split them into multiple smaller messages, perhaps for 500 interfaces at a time. **TODO We need a mechanism so that the client knows all list entries have been returned, and hence it can delete stale entries?  E.g., something like a *more_data* flag.**

1. A Publisher is allowed to generate on-change notifications at an *object* level, which hence may contain other associated fields that may not have changed state, rather than restricting the on-change notifications strictly to only those specific fields that have changed state.  E.g., if a subscribers registers on the path */ietf-interfaces:interfaces/interface\[name = \*\]/oper-status*, and if interface *eth1* had a change in the *oper-status* leaf state, then rather than just publishing the updated *oper-status* leaf, the publisher may instead publish all the data associated with that interface entry object, i.e., everything under */ietf-interfaces:interface/interface\[name = eth1\]*.  **TODO Does it have to be the entire subtree that is published?  Do we need to add a capability annotation to indicate the object publication paths?**

To ensure that clients can reasonably process data returned via decomposed filters then:

1. *update* notifications MUST indicate the precise subtree of data that the update message is updating or replacing, i.e., so a receiver can infer that data nodes no longer being notified by the publisher have been deleted:

   - if we support splitting list entries in multiple updates, then something like a *more_data* flag is needed to indicate that the given update message is not complete.

**TODO We should consider adding a *update-complete* message (potentially including an incrementing collection counter) to indicate when a periodic update has completed for a subscription.**

# Datastore Event Streams {#events}

In YANG Push Lite, a subscription, based on the selected filters, will generate a ordered stream of datastore *update* records that is referred to as an event stream.  Each subscription logically has a different event stream of update records, even if multiple subscriptions use the same filters to select datastore nodes.

As YANG-defined event records are created by a system, they may be assigned to one or more streams.  The event record is distributed to a subscription's receiver(s) where (1) a subscription includes the identified stream and (2) subscription filtering does not exclude the event record from that receiver.

Access control permissions may be used to silently exclude event records from an event stream for which the receiver has no read access.  See [RFC8341], Section 3.4.6 for an example of how this might be accomplished.  Note that per Section 2.7 of this document, subscription state change notifications are never filtered out. **TODO, filtering and NACM filtering should be dependent on whether it is a configured or dynamic subscription.**

If subscriber permissions change during the lifecycle of a subscription and event stream access is no longer permitted, then the subscription MUST be terminated. **TODO, check this**

Event records SHALL be delivered to a receiver in the order in which they were generated.

## Event Records {#EventRecords}

A single *update* record is used for all notifications.  It is used to report the current state of a set of data nodes at a given target path for either periodic, on-change, or resync notifications, and also for on-change notifications to indicate that the data node at the given target path has been deleted.

The update notification is encoded using {{I-D.draft-netana-netconf-notif-envelope}} to wrap the notification message, instead of {{RFC5277}}, that is normally used for NETCONF {{RFC8641}} and RESTCONF {{RFC8040}} YANG notifications.

The schema for this notifications is given in the following tree diagram:

~~~~ yangtree
{::include tree-output/update-notification.txt}
~~~~
{: align="left" title="'update' notification"}

The normative definitions for the notifications fields are given in the YANG module in {{ietf-yp-lite-yang}}.  The fields can be informatively summarized as:

- *id* - identifies the subscription the notification relates to.

- *path-prefix* - identifies the absolute instance-data path to which all target-paths are data are encoded relative to.

- *snapshot-type* - this indicates what type of event causes the update message to be sent.  I.e., a periodic collection, an on-change event, or a resync collection.

- *observation-time* - the time that the data was sampled, or when the on-change event occurred that caused the message to be published.

- *target-path* - identifies the data node that is being acted on, either providing the replacement data for, or that data node that is being deleted.

- *data* - the full replacement data subtree for the content at the target-path, encoded from the path-prefix.

- *incomplete* - indicates that the message is incomplete for any reason.  For example, perhaps a periodic subscription expects to retrieve data from multiple data sources, but one of those data sources is unavailable.  Normally, a receiver can use the absence of a field in an update message to implicitly indicate that the field has been deleted, but that should not be inferred if the incomplete-update leaf is present because not all changes that have occurred since the last update are actually included with this update.

As per the structure of the *update* notification, a single notification MAY provide updates for multiple target-paths.

## Types of subscription event monitoring

Subscription can either be based on sampling the requested data on a periodic cadence or being notified when the requested data changes.  In addition, this specification allows for subscriptions that both notify on-change and also with a periodic cadence, which can help ensure that the system eventually converges on the right state, even if on-change notification were somehow lost or mis-processed anywhere in the data processing pipeline.

The schema for the update-trigger container is given in the following tree diagram:

~~~~ yangtree
{::include tree-output/update-trigger.txt}
~~~~
{: align="left" title="'update-trigger' container"}

**TODO Minor - is providing the structure from root helpful, or should this just report the update-trigger container.**

The normative definitions for the update-trigger fields are given in the *ietf-yp-lite* YANG module in {{ietf-yp-lite-yang}}.  They are also described in the following sections.

## Periodic events

In a periodic subscription, the data included as part of an update record corresponds to data that could have been read using a retrieval operation.  Only the state that exists in the system at the time that it is being read is reported, periodic updates never explicitly indicate whether any data-nodes or list entries have been deleted.  Instead, receivers must infer deletions by the absence of data during a particular collection event.

For periodic subscriptions, triggered updates will occur at the boundaries of a specified time interval.  These boundaries can be calculated from the periodic parameters:

- a *period* that defines the duration between push updates.

- an *anchor-time*; update intervals fall on the points in time that are a multiple of a *period* from an *anchor-time*.  If an *anchor-time* is not provided, then the publisher chooses a suitable anchor-time, e.g., perhaps the time that the subscription was first instantiated by the publisher.

The anchor time and period are particularly useful, in fact required, for when the collected telemetry data is being stored in a time-series database and the subscription is setup to ensure that each collection is placed in a separate time-interval bucket.

Periodic update notifications are expected, but not required, to use a single *target-path* per *update* notification.

## On-Change events

In an on-change subscription, *update* records indicate updated values or when a monitored data node or list node has been deleted.  *update* records SHOULD be generated at the same subtree as equivalent periodic subscription rather than only the specific data node that is on-change notifiable.  The goal is to ensure that the *update* message contains a consistent set of data on the subscription path.

Each entry in the *updates* list identifies a data node (i.e., list entry, container, leaf or leaf-list), via the *target-path* that either has changes is state or has been deleted.

A delete of a specific individual data node or subtree may be notified in two different ways:

- if the data that is being deleted is below the *target-path* then the delete is implicit by the publisher returning the current data node subtree with the delete data nodes missing.  I.e., the receiver must implicitly infer deletion.

- if the data node is being deleted at the target path.  E.g., if an interface is deleted then an entire list entry related to that interface may be removed.  In this case, the *target path* identifies the list entry that is being deleted, but the data returned is just an empty object ```{}```, which replaces all the existing data for that object in the receiver.

For on-change subscriptions, an update trigger occurs whenever a change in the subscribed information is detected.  The following additional parameters are included:

- *sync-on-start* defines whether or not a complete snapshot of all subscribed data is sent at the beginning of a subscription.  Such early synchronization establishes the frame of reference for subsequent updates.

### On-Change Notifiable Datastore Nodes {#OnChangeConsiderations}

Publishers are not required to support on-change notifications for all data nodes, and they may not be able to generate on-change updates for some data nodes.  Possible reasons for this include:

- the value of the datastore node changes frequently (e.g., the in-octets counter as defined in {{RFC8343}}),

- small object changes that are frequent and meaningless (e.g., a temperature gauge changing 0.1 degrees),

- or no implementation is available to generate a notification when the source variable for a particular data node has changed.

In addition, publishers are not required to notify every change or value for an on-change monitored data node.  Instead, publishers MAY limit the rate at which changes are reported for a given data node, suppressing further updates for a short time interval.  If a data node changes value and then reverts back to the original value then the publisher MAY suppress reporting the change entirely.  However, if the data node changes to a new value for a longer period than any internal dampening interval, then the change and latest state MUST be reported to the receiver.

To give an example, if the interface link state reported by hardware is changing state hundreds of times per second, then it would be entirely reasonable to limit those interface state changes to a much lower cadence, e.g., perhaps every 100 milliseconds.  In the particular case of interfaces, there may also be data model specific forms of more advanced dampening that are more appropriate, e.g., that notify interface down events immediately, but rate limit how quickly the interface is allowed to transition to up state, which overall acts as a limit on the rate at which the interface state may change, and hence also act as a limit on the rate at which on-change notifications could be generated.

The information about what nodes support on-change notifications is reported using capabilities operational data model.  This is further described in {{ConformanceAndCapabilities}}.

### On-Change Considerations

On-change subscriptions allow receivers to receive updates whenever changes to targeted objects occur.  As such, on-change subscriptions are particularly effective for data that changes infrequently but for which applications need to be quickly notified, with minimal delay, whenever a change does occur.

On-change subscriptions tend to be more difficult to implement than periodic subscriptions.  Accordingly, on-change subscriptions may not be supported by all implementations or for every object.

Whether or not to accept or reject on-change subscription requests when the scope of the subscription contains objects for which on-change is not supported is up to the publisher implementation.  A publisher MAY accept an on-change subscription even when the scope of the subscription contains objects for which on-change is not supported.  In that case, updates are sent only for those objects within the scope of the subscription that do support on-change updates, whereas other objects are excluded from update records, even if their values change.  In order for a subscriber to determine whether objects support on-change subscriptions, objects are marked accordingly on a publisher.  Accordingly, when subscribing, it is the responsibility of the subscriber to ensure that it is aware of which objects support on-change and which do not.  For more on how objects are so marked, see Section 3.10. **TODO Is this paragraph and the one below still the right choice for YANG Push Lite?**

Alternatively, a publisher MAY decide to simply reject an on-change subscription if the scope of the subscription contains objects for which on-change is not supported.  In the case of a configured subscription, the publisher MAY suspend the subscription.

## Combined period and on-change subscriptions

A single subscription may created to generate notifications both when changes occur and when changes occur.  Such subscriptions are equivalent to having separate periodic and on-change subscriptions on the same path, except that they share the same subscription-id and filter paths.

## Streaming Update Examples

**TODO, Generate new JSON based example of a periodic, and delete messages.  Current placeholders are the existing YANG Push Notifications.**

Figure XXX provides an example of a notification message for a subscription tracking the operational status of a single Ethernet interface (per {{RFC8343}}).  This notification message is encoded XML *W3C.REC-xml-20081126* over the Network Configuration Protocol
(NETCONF) as per {{RFC8640}}.

~~~~
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
~~~~
{: align="left" title="Example 'update' periodic notification"}

Figure XXX provides an example of an on-change notification message for
the same subscription.

~~~~
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
~~~~
{: align="left" title="Example 'update' on-change notification"}


# Receivers, Transports, and Encodings {#receivers}

Every subscription is associated with one or more receivers, which each identify the destination host where all the subscription notifications are sent.  Each receiver has the following associated properties:

- a *name* to identify and reference the receiver in configured subscriptions.

- a *transport*, which identifies the transport protocol to use to
  connect with all subscription receivers.

  - Optional transport-specific related parameters, e.g., DSCP.  There are likely to be various data nodes related to establishing appropriate security and encryption.

- an *encoding* to encode all YANG notification messages to the receiver, i.e., see {{Encodings}}.

- optional parameters to identify where traffic should egress the publisher:

  - a *source-interface*, which identifies the egress interface to use from the publisher.

  - a *source-address* address, which identifies the IP address to stamp on notification messages destined for the receiver.

  - a *source-vrf*, which identifies the Virtual Routing and Forwarding (VRF) instance on which to reach receivers.  This VRF is a network instance as defined in {{RFC8529}}.  Publisher support for VRFs is optional and advertised using the *supports-vrf* feature. **TODO - do we also support inferring the VRF from the source interface?**

  If none of the above parameters are set, notification messages egress the publisher's default interface.

### Receivers for Configured Subscriptions

For configured subscriptions, receivers are configured independently from the subscriptions and then referenced from the subscription.

Configured subscriptions MAY have multiple receivers, but they MUST have the same encoding.  Multiple receivers facilitate redundancy at the receivers.  **TODO, is there a diagram from one of Thomas's drafts that we can reference here to show the end-to-end picture**

Below is a tree diagram for *datastore-telemetry/receivers* container.  All objects contained in this tree are described in the YANG module in {{yp-lite-yang-module}}.

~~~~ yangtree
{::include tree-output/receivers.txt}
~~~~
{: align="left" title="datastore-telemetry/receivers container" #ReceiversYangTree }

Because there is no explicit association with an existing transport session, configuration operations include additional parameters beyond those of dynamic subscriptions.  These parameters identify each receiver, how to connect with that receiver, and possibly whether the notification messages need to come from a specific egress interface on the publisher.  Receiver-specific transport connectivity parameters MUST be configured via transport-specific augmentations to this specification.  See Section 2.5.7 for details.

This specification is transport independent.  However, supporting a configured subscription will normally require the establishment of transport connectivity.  Some parameters used for this transport connectivity establishment are transport specific.  As a result, the
YANG module defined in {{yp-lite-yang-module}} is not able to directly define and expose these transport parameters.

It is necessary for an implementation to support the connection establishment process.  To support this function, the YANG data model defined in this document includes a YANG choice node where transport-specific parameters for a particular receiver may be augmented.  This node is */datastore-telemetry/receivers/receiver/transport-type*.

A publisher supporting configured subscriptions must obviously support at least one YANG data model that augments transport connectivity parameters on
*/datastore-telemetry/receivers/receiver/transport-type*.  For an example of such an augmentation, see {{I-D.draft-ietf-netconf-udp-notif}} **TODO, this isn't quite the right reference, since we will need a variant that augments into the YP Lite receiver container.**

### Receivers for Dynamic Subscriptions

For dynamic subscriptions, each subscription has a single receiver that is implicit from the host that initiated the *establish-subscription* RPC, reusing the same transport session for all the subscription notifications.

Hence most receiver parameters for a dynamic subscription are implicitly determined, and cannot be explicitly controlled.

Dynamic subscriptions MUST specify an encoding (see {{Encodings}}) and MAY specify DSCP Marking (see {{DSCP}}) for the telemetry notifications in the *establish-subscription* RPC (see {{EstablishSubscriptionYangTree}}).

## Transports {#transports}

This document describes a transport-agnostic mechanism for subscribing to YANG datastore telemetry.  Hence, separate specifications are required to define transports that support YANG Push Lite.  The requirements for these transport specifications are documented in the following section:

### Requirements for Yang Push Lite Transport Specifications

This section provides requirements for any transport specifications supporting the YANG Push Lite solution presented in this document.

The transport MUST provide a YANG module (to be implemented by the receiver) that *datastore-telemetry/receivers/transport-type* choice statement with a container the identifies the transport and contains all the transport specific parameters.

The transport selected by the subscriber to reach the publisher SHOULD be able to support multiple "establish-subscription" requests made in the same transport session.

For both configured and dynamic subscriptions, the publisher SHOULD authenticate a receiver via some transport-level mechanism before sending any event records that the receiver is authorized to see.  In addition, the receiver SHOULD authenticate the publisher at the transport level.  The result is mutual authentication between the two. **TODO, do we want this text, I have already weakened this from the MUST in Yang Push.**

A secure transport is RECOMMENDED.  For dynamic subscriptions, the publisher MUST ensure that the receiver has sufficient authorization to perform the function it is requesting against the specific subset of content involved. **TODO, is this transport level authorization, or NACM based authorization (in which case, it should not be stated here, since it is application specific rather than being transport specific).**

A specification for a transport built upon this document can choose whether to use of the same logical channel for the RPCs and the event records.  However, the *update* records and the subscription state change notifications MUST be sent on the same transport session to ensure properly ordered delivery.

If a transport can only support some encodings, then it MUST identify what encodings are supported.  If a configured subscription's transport allows different encodings, the specification MUST identify the default encoding. **TODO, would it be easier to always require the encoding to be specified?**

Any transport specific impacts to the lifecycle of configured or dynamic subscriptions MUST be documented.  E.g., the point at which a subscription can be determined as being established. **TODO, do we need this paragraph, if the behavior is meant to be transport specific that why does anything need to be said at all?  And if the transport wants to change the behavior then, by definition, that must be documented in the transport specification anyway.**

Additional transport requirements may be dictated by the choice of transport used with a subscription.  For an example of such requirements, see {{RFC8640}}.

### DSCP Marking {#DSCP}

YANG Push Lite supports "dscp" marking to differentiate prioritization of notification messages during network transit.

If the publisher supports the "dscp" feature, then a subscription with a "dscp" leaf results in a corresponding Differentiated Services Code Point (DSCP) marking [RFC2474] being placed in the IP header of any resulting notification messages and subscription state change notifications.  A publisher MUST respect the DSCP markings for subscription traffic egressing that publisher.

**TODO, do we want to keep "dscp" as a feature, or to simplify, just assume implementations will support it or otherwise deviate the leaf?**

Different DSCP code points require different transport connections.  As a result, where TCP is used, a publisher that supports the "dscp" feature must ensure that a subscription's notification messages are returned in a single TCP transport session where all traffic shares the subscription's "dscp" leaf value.  If this cannot be guaranteed, any "establish-subscription" RPC request SHOULD be rejected with a "dscp-unavailable" error.  **TODO - Is this text still relevant?**

## Encodings {#Encodings}

The *update* notification({{EventRecords}}) and subscription lifecycle notifications ({{LifecycleNotifications}}) can be encoded in any format that has a definition for encoding YANG data.  For a given subscription, all notification messages are encoded using the same encoding.

Some IETF standards for YANG encodings known at the time of publication are:

- JSON, defined in {{RFC7951}}
- CBOR, defined in {{RFC9254}}, and {{RFC9595}} for using compressed schema identifiers (YANG SIDs)
- XML, defined in {{RFC7950}}

To maximize interoperability, all implementations are RECOMMENDED to support JSON and CBOR encodings. Also supporting XML and CBOR using YANG SIDs is OPTIONAL.

Encodings are defined in the *ietf-yp-lite.yang* as YANG identities that derive from the *encoding* base identity.  Additional encodings can be defined by defining and implementing new identities that derive from the *encoding* base identity, and also advertising those identities as part of the capabilities YANG model. **TODO, given more details of which leaf this is in the capabilities model.**

**TODO, the YP data model use if-feature statements for each of the encodings.  Should we preserve these feature statements, or rely only on capabilities information to indicate what encoding are supported?  Currently they are commented out with an intention to remove them.**

For configured subscriptions, the encoding is configured as part of the receiver configuration ({{ReceiversYangTree}}).

For dynamic subscriptions, the encoding is selected as part of the establish-subscription RPC ({{EstablishSubscriptionYangTree}}).

**TODO For dynamic subscriptions, Yang Push will infer the encoding from incoming RPC if not provided.  Do we want to preserve the existing behavior or just be explicit and enforce that an encoding must always be specified?**

# Setting up and Managing Subscriptions {#ConfiguredAndDynamic}

Subscriptions can be set up and managed in two ways:

1. Configured Subscriptions - a subscription created and controlled solely by configuration.
2. Dynamic Subscriptions - a subscription created and controlled via a YANG RPC from a telemetry receiver.

Both configured and dynamic subscriptions are represented in the list *datastore-telemetry/subscriptions/subscription*, and most of the functionality and behavior of configured and dynamic subscriptions described in this document is specified to be the same or very similar.  However, they differ in how they are created and in the associated lifecycle management, described in the following sections:

Additional characteristics differentiating configured from dynamic subscriptions include the following:

- The lifetime of a dynamic subscription is bound by the transport session used to establish it.  For connection-oriented stateful transports like NETCONF, the loss of the transport session will result in the immediate termination of any associated dynamic subscriptions.  For connectionless or stateless transports like HTTP, a lack of receipt acknowledgment of a sequential set of notification messages and/or keep-alives can be used to trigger a termination of a dynamic subscription.  Contrast this to the lifetime of a configured subscription.  This lifetime is driven by relevant configuration being present in the publisher's applied configuration.  Being tied to configuration operations implies that (1) configured subscriptions can be configured to persist across reboots and (2) a configured subscription can persist even when its publisher is fully disconnected from any network.

- Configured subscriptions can be modified by any configuration client with write permission on the configuration of the subscription.  Dynamic subscriptions can only be modified via an RPC request made by the original subscriber or by a change to configuration data referenced by the subscription.

Note that there is no mixing and matching of dynamic and configured operations on a single subscription.  Specifically, a configured subscription cannot be modified or deleted using RPCs defined in this document.  Similarly, a dynamic subscription cannot be directly modified or deleted by configuration operations.  It is, however, possible to perform a configuration operation that indirectly impacts a dynamic subscription.  By changing the value of a preconfigured filter referenced by an existing dynamic subscription, the selected event records passed to a receiver might change.

A publisher MAY terminate a dynamic subscription at any time. Similarly, it MAY decide to temporarily suspend the sending of notification messages for any dynamic subscription, or for one or more receivers of a configured subscription.  Such termination or suspension is driven by internal considerations of the publisher.

## Configured Subscriptions

Configured subscriptions allow the management of subscriptions via configuration so that a publisher can send notification messages to a receiver.  Support for configured subscriptions is optional, with its availability advertised via the *configured* YANG feature in the ietf-yp-lite YANG model (**TODO and also in the capabilities model?**).

A configured subscription comprises:

- the target datastore for the subscription, as per {{RFC8342}}.

- a set of selection filters to choose which datastore nodes the subscription is monitoring or sampling, as described in {{pathsAndFilters}}

- configuration for how update notifications for the data nodes are triggered.  I.e., either periodic sampling, on-change event-driven, or both. (**TODO add section reference**)

- a set of associated receivers (as described in {{receivers}}) that specify transport, receiver, and encoding parameters.

Configured subscriptions have several characteristics distinguishing them from dynamic subscriptions:

- persistence across publisher reboots,

- a reference to receiver, is explicitly configured rather than being implicitly associated with the transport session, as would be the case for a dynamic subscription.

- an ability to send notification messages to more than one receiver.  All receivers for a given subscription must use the same encoding and type of transport (**TODO What about DSCP settings?**).  *Note that receivers are unaware of the existence of any other receivers.*

- persistence even when transport or receiver is unavailable.  In this scenario, the publishers will terminate a subscription that it cannot keep active, but it will periodically attempt to restablish connection to the receiver and re-activate the configured subscription.

Multiple configured subscriptions MUST be supportable over a single transport session.

Below is a tree diagram for the "subscriptions" container.  All objects contained in this tree are described in the YANG module in {{yp-lite-yang-module}}.  In the operational datastore {{RFC8342}}, the "subscription" list contains entries both for configured and dynamic subscriptions.

~~~~ yangtree
{::include tree-output/subscriptions.txt}
~~~~
{: title="subscriptions container Tree Diagram" #SubscriptionYangTree }

### Configured Subscription State Machine

Below is the state machine for a configured subscription on the publisher.  This state machine describes the three states (*valid*, *invalid*, and *concluded*) as well as the transitions between these states.  Start and end states are depicted to reflect configured subscription creation and deletion events.  The creation or modification of a configured subscription, referenced filter or receiver initiates an evaluation by the publisher to determine if the subscription is in the *valid* state or the *invalid* state.  The publisher uses its own criteria in making this determination.  If in the *valid* state, the subscription becomes operational.  See (1) in the diagram below.

**TODO - Add a new 'Active state' to the subscription state machine.  I.e., a subscription is active as long as it has at least one valid receiver (in some cases this would mean that negotiation with the receiver is complete, for others, such as simple UDP, is just requires configuration to be valid.)

~~~~
.........
: start :-.
:.......: |
     create  .---modify----<-----.
          |  |                   |
          V  V               .-------.         .......
 .----[evaluate]--no-------->|invalid|-delete->: end :
 |                           '-------'         :.....:
 |-[re-evaluate]--no--(2)-.      ^                ^
 |        ^               |      |                |
yes       |               '->unsupportable      delete
 |      modify             (subscription-   (subscription-
 |        |                 terminated*)     terminated*)
 |        |                      |                |
(1)       |                     (3)              (4)
 |   .---------------------------------------------------.
 '-->|                     valid                         |
     '---------------------------------------------------'

Legend:
  Dotted boxes: subscription added or removed via configuration
  Dashed boxes: states for a subscription
  [evaluate]: decision point on whether the subscription
              is supportable
  (*): resulting subscription state change notification
~~~~
{: title="Publisher's State Machine for a Configured Subscription"}

A subscription in the *valid* state may move to the *invalid* state in one of two ways.  First, it may be modified in a way that fails a re-evaluation.  See (2) in the diagram.  Second, the publisher might determine that the subscription is no longer supportable.  This could be because of an unexpected but sustained increase in an event stream's event records, degraded CPU capacity, a more complex referenced filter, or other subscriptions that have usurped resources.  See (3) in the diagram.  No matter the case, a *subscription-terminated* notification is sent to any receivers in the *active* or state.  Finally, a subscription may be deleted by configuration (4).

When a subscription is in the *valid* state, a publisher will attempt to connect with all receivers of a configured subscription and deliver notification messages.  Below is the state machine for each receiver of a configured subscription.  This receiver state machine is fully contained in the state machine of the configured subscription and is only relevant when the configured subscription is in the *valid* state.

~~~~
.-----------------------------------------------------------.
|                         valid                             |
|   .----------.                           .------------.   |
|   | receiver |---timeout---------------->|  receiver  |   |
|   |connecting|<----------------reset--(c)|disconnected|   |
|   |          |<-transport                '------------'   |
|   '----------'  loss,reset                                |
|      (a)          |                                       |
|  subscription-   (b)                                      |
|  started*    .--------.                                   |
|       '----->|        |                                   |
|              |receiver|                                   |
| subscription-| active |                                   |
|   modified*  |        |                                   |
|        '---->'--------'                                   |
'-----------------------------------------------------------'

Legend:
  Dashed boxes that include the word *receiver* show the possible
  states for an individual receiver of a valid configured
  subscription.

* indicates a subscription state change notification
~~~~
{: title="Receiver State Machine for a Configured Subscription on a Publisher"}

When a configured subscription first moves to the *valid* state, the *state* leaf of each receiver is initialized to the *connecting* state.  If transport connectivity is not available to any receivers and there are any notification messages to deliver, a transport session is established (e.g., per {{RFC8071}}).  Individual receivers are moved to the *active* state when a *subscription-started* subscription state change notification is successfully passed to that receiver (a).  Event records are only sent to active receivers. Receivers of a configured subscription remain active on the publisher if both (1) transport connectivity to the receiver is active and (2) event records are not being dropped due to a publisher's sending capacity being reached.  In addition, a configured subscription's receiver MUST be moved to the "connecting" state if the receiver is reset via the "reset" action (b), (c).  For more on the "reset" action, see Section 2.5.5.  If transport connectivity cannot be achieved while in the "connecting" state, the receiver MAY be moved to the "disconnected" state.

A configured subscription's receiver MUST be moved to the "suspended" state if there is transport connectivity between the publisher and receiver but (1) delivery of notification messages is failing due to a publisher's buffer capacity being reached or (2) notification messages cannot be generated for that receiver due to insufficient CPU (d).  This is indicated to the receiver by the "subscription-suspended" subscription state change notification.

A configured subscription's receiver MUST be returned to the "active" state from the "suspended" state when notification messages can be generated, bandwidth is sufficient to handle the notification messages, and a receiver has successfully been sent a "subscription-resumed" or "subscription-modified" subscription state change notification (e).  The choice as to which of these two subscription state change notifications is sent is determined by whether the subscription was modified during the period of suspension.

Modification of a configured subscription is possible at any time.  A "subscription-modified" subscription state change notification will be sent to all active receivers, immediately followed by notification messages conforming to the new parameters.  Suspended receivers will
also be informed of the modification.  However, this notification will await the end of the suspension for that receiver (e).

### Creating a Configured Subscription

Configured subscriptions are created using configuration operations against the top-level *subscriptions* subtree.

After a subscription is successfully established, the publisher immediately sends a "subscription-started" subscription state change notification to each receiver.  It is quite possible that upon configuration, reboot, or even steady-state operations, a transport session may not be currently available to the receiver.  In this case, when there is something to transport for an active subscription, transport-specific "call home" operations {{RFC8071}} will be used to establish the connection.  When transport connectivity is available, notification messages may then be pushed.

With active configured subscriptions, it is allowable to buffer event records even after a *subscription-started* has been sent.  However, if events are lost (rather than just delayed) due to buffer capacity being reached, a *subscription-terminated* notification must be sent, followed by a new subscription-started" notification. These notifications indicate an event record discontinuity has occurred.

**TODO, to see an example of subscription creation using configuration operations over NETCONF, see Appendix A.**

### Modifying a Configured Subscription

Configured subscriptions may end up being modified due to configuration changes in the *datastore-telemetry* container.

If the modification involves adding receivers, then those receivers are placed in the *connecting* state.  If a receiver is removed, the subscription state change notification *subscription-terminated* is sent to that receiver if that receiver is active or suspended.

### Deleting a Configured Subscription

Configured subscriptions can be deleted via configuration.  After a subscription has been removed from configuration, the publisher MAY complete their current collection if one is in progress, then the publisher sends *subscription-terminated* notification to all of the subscription's receivers to indicate that the subscription is no longer active.

###  Resetting a Configured Subscription's Receiver

**TODO: Is this RPC needed?  It may possibly be useful for a UDP based receiver.**

It is possible that a configured subscription to a receiver needs to be reset.  This is accomplished via the *reset* action in the YANG module at */subscriptions/subscription/receivers/receiver/reset*.  This action may be useful in cases where a publisher has timed out trying to reach a receiver.  When such a reset occurs, a transport session will be initiated if necessary, and a new *subscription-started* notification will be sent.  This action does not have any effect on transport connectivity if the needed connectivity already exists.

## Dynamic Subscriptions

Dynamic subscriptions, where a subscriber initiates a subscription negotiation with a publisher via an RPC.  If the publisher is able to serve this request, it accepts it and then starts pushing notification messages back to the subscriber.  If the publisher is not able to serve it as requested, then an error response is returned.

Dynamic subscriptions are managed via protocol operations (in the form of RPCs, per [RFC7950], Section 7.14) made against targets located in the publisher.  These RPCs have been designed extensibly so that they may be augmented for subscription targets beyond event streams.  For examples of such augmentations, see the RPC augmentations in the YANG data model provided in [RFC8641].

### Dynamic Subscription State Machine

Below is the publisher's state machine for a dynamic subscription. Each state is shown in its own box.  It is important to note that such a subscription doesn't exist at the publisher until an *establish-subscription* RPC is accepted.  The mere request by a subscriber to establish a subscription is not sufficient for that subscription to be externally visible.  Start and end states are depicted to reflect subscription creation and deletion events.

~~~~
                  .........
                  : start :
                  :.......:
                      |
              establish-subscription
                      |
                      |
                      v
                 .-----------.
                 | receiver  |
                 |  active   |
                 |           |
                 '-----------'
                      |
            delete/kill-subscription
                      |
                      v
                  .........
                  :  end  :
                  :.......:
~~~~
{: title="Publisher's State Machine for a Dynamic Subscription"}

Of interest in this state machine are the following:

- Successful "establish-subscription" RPCs move the subscription to the "active" state.

- A "delete-subscription" or "kill-subscription" RPC will end the subscription.

- A publisher may choose to end a subscription when there is not sufficient CPU or bandwidth available to service the subscription.  This is announced to the subscriber via the *subscription-terminated* subscription state change notification.  The receiver will need to establish a new subscription.

### Establishing a Dynamic Subscription {#EstablishDynamic}

The "establish-subscription" RPC allows a subscriber to request the creation of a subscription.

The input parameters of the operation are:

o  A "stream" name, which identifies the targeted event stream against which the subscription is applied.

o  An event stream filter, which may reduce the set of event records pushed.

o  If the transport used by the RPC supports multiple encodings, an optional "encoding" for the event records pushed.  If no "encoding" is included, the encoding of the RPC MUST be used.

If the publisher can satisfy the "establish-subscription" request, it replies with an identifier for the subscription and then immediately starts streaming notification messages.

Below is a tree diagram for "establish-subscription".  All objects contained in this tree are described in the YANG module in {{yp-lite-yang-module}}.

~~~~ yangtree
{::include tree-output/establish-subscription.txt}
~~~~
{: align="left" title="establish-subscription YANG RPC" #EstablishSubscriptionYangTree }

A publisher MAY reject the "establish-subscription" RPC for many reasons, as described in Section 2.4.6.

Below is a tree diagram for "establish-subscription-stream-error-info" RPC yang-data.  All objects contained in this tree are described in the YANG module in Section 4.

~~~~
    yang-data establish-subscription-stream-error-info
      +--ro establish-subscription-stream-error-info
        +--ro reason?                   identityref
        +--ro filter-failure-hint?      string

        Figure 3: "establish-subscription-stream-error-info"
                    RPC yang-data Tree Diagram
~~~~
{: align="left" title="\"establish-subscription-stream-error-info\" Tree Diagram"}

#### Negotiation of Subscription Policies

A dynamic subscription request SHOULD be declined if a publisher determines that it may be unable to provide update records meeting the terms of an "establish-subscription" RPC request.

### Deleting a Dynamic Subscription

The *delete-subscription* operation permits canceling an existing dynamic subscription that was established on the same transport session connecting to the subscriber.

If the publisher accepts the request, which it MUST, if the subscription-id matches a dynamic subscription established in the same transport session, then it should stop the subscription and send a *subscription-terminated* notification.

The publisher MAY reply back to the client before the subscription has been terminated, i.e., it may act asynchronously with respect to the request.  The publisher SHOULD NOT send any further events related to the subscription after the *subscription-terminated* notification and

**TODO, I think that we should relax this to a SHOULD**  If the publisher accepts the request and the publisher has indicated success, the publisher MUST NOT send any more notification messages for this subscription.

### Killing a Dynamic Subscription

The "kill-subscription" RPC operation permits a client to forcibly end any arbitrary dynamic subscription, identified by subscription-id, including those not associated with the transport session used for the RPC.  Note, configured subscriptions cannot be killed using this RPC, and requests to do MUST be rejected.

### RPC Failures

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

  delete-subscription            kill-subscription
  ----------------------         ----------------------
  no-such-subscription           no-such-subscription
~~~~~~~~~~

To see a NETCONF-based example of an error response from the list
above, see the "no-such-subscription" error response illustrated in
{{RFC8640}}, Figure 10.

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

## Implementation Considerations (from RFC 8639)

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

## Event Record Delivery

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

# Subscription Lifecycle Notifications {#LifecycleNotifications}

In addition to sending event records to receivers, a publisher also sends subscription lifecycle state change notifications when lifecycle events related to subscription management occur.

Subscription state change notifications are generated per receiver, and are injected into the event stream of datastore update notifications for that receiver.  These notifications MUST NOT be dropped or filtered.  **TODO, would it be better for lifecycle events to be generated per subscription (i.e., to preserve sequence numbers if there are multiple receivers).**

Future extensions, or implementations MAY provide additional details in the notifications through the use of YANG augmentations to add data nodes into the notification structures.

The complete set of subscription state change notifications is described in the following subsections:

## "subscription-started"

The subscription started notification is sent to a receiver to indicate that a subscription is active and they may start to receive *update* records from the publisher.

The subscription started notification may be sent to a receiver for any of these reasons:

1. A new subscription has been configured.

1. A receiver has been added to a configured subscription.

1. The configuration for a configured subscription has been changed, in which case a *subscription-terminated* notification should be sent, followed by a *subscription-started* notification if the new configuration is valid.

1. A configured subscription previously failed, and was terminated.  After the publisher has successfully re-established a connection to the receiver and is starting to send datastore event records again.

1. A dynamic subscription has been established.

<!--
Included in this
subscription state change notification are all the parameters of the
subscription, except for (1) transport connection information for one
or more receivers and (2) origin information indicating where
notification messages will egress the publisher.  Note that if a
referenced filter from the "filters" container has been used in the
subscription, the notification still provides the contents of that
referenced filter under the "within-subscription" subtree.
-->

Below is the tree diagram for "subscription-started".  All objects contained in this tree diagram are described in the YANG module in {{yp-lite-yang-module}}.

~~~~ yangtree
{::include tree-output/subscription-started.txt}
~~~~
{: align="left" title="subscription-started Notification Tree Diagram"}

**TODO, Should the subscription-started notification report decomposed subscription paths?**

##  "subscription-terminated"

For a receiver, this notification indicates that no further event records for an active subscription should be expected from the publisher.

A *subscription-terminated* notification SHOULD only be sent by a publisher to a receiver if a *subscription-started* notification was previously sent.

A publisher SHOULD NOT send any further event records after the *subscription-terminated* notification.

The subscription terminated notification may be sent to a receiver for any of these reasons:

1. A receiver has been removed from a configured subscription.

1. A configured subscription has been removed.

1. The configuration for a configured subscription has been changed, in which case a *subscription-terminated* notification should be sent, followed by a *subscription-started* notification if the new configuration is valid.

1. A dynamic subscription was deleted via a "delete-subscription* or *kill-subscription* RPC.

1. A subscription has failed for any reason, e.g.,:

    - The publisher is no longer able to honor the subscription, due to resource constraints, or the filter is no longer valid.

    - Any transport level buffer to the receiver has become full, and the hence the publisher is dropping *update* notifications.

Below is a tree diagram for "subscription-terminated".  All objects contained in this tree are described in the YANG module in {{yp-lite-yang-module}}.

~~~~ yangtree
{::include tree-output/subscription-terminated.txt}
~~~~
{: align="left" title="subscription-terminated Notification Tree Diagram"}

**TODO Augmenting extra fields is better for clients?**  The *reason* datanode identifyref indicates why a subcription has been terminated, and could be extended with further reasons in future.

##  "replay-completed"

**TODO: Need to consider how this works when notifications are split up.  Possibly need to replace this with an opt-in message for a per collection complete message.  I.e., a notification that would be sent whenever every periodic collection is complete.**

This notification indicates that all of the event records prior to the current time have been passed to a receiver.  It is sent before any notification messages containing an event record with a timestamp later than (1) the subscription's start time.

After the "replay-completed" notification has been sent, additional event records will be sent in sequence as they arise naturally on the publisher.

Below is a tree diagram for "replay-completed".  All objects contained in this tree are described in the YANG module in Section 4.

~~~~ yangtree
{::include tree-output/replay-completed.txt}
~~~~
{: align="left" title="replay-completed Notification Tree Diagram"}

# Performance, Reliability, and Subscription Monitoring

**TODO.  Needs updating.  Not sure if this text doesn't end up elsewhere?**

A subscription to updates from a datastore is intended to obviate the need for polling.  However, in order to do so, it is critical that subscribers can rely on the subscription and have confidence that they will indeed receive the subscribed updates without having to worry about updates being silently dropped.  In other words, a subscription constitutes a promise on the side of the publisher to provide the receivers with updates per the terms of the subscription, or otherwise notify the receiver if t

Now, there are many reasons why a publisher may at some point no longer be able to fulfill the terms of the subscription, even if the subscription had been initiated in good faith.  For example, the volume of datastore nodes may be larger than anticipated, the interval may prove too short to send full updates in rapid succession, or an internal problem may prevent objects from being collected.  For this reason, the solution defined in this document (1) mandates that a publisher notify receivers immediately and reliably whenever it encounters a situation in which it is unable to keep the terms of the subscription and (2) provides the publisher with the option to suspend the subscription in such a case.  This includes indicating the fact that an update is incomplete as part of a "push-update" or "push-change-update" notification, as well as emitting a "subscription-suspended" notification as applicable.  This is described further in Section 3.11.1.

A publisher SHOULD reject a request for a subscription if it is unlikely that the publisher will be able to fulfill the terms of that subscription request.  In such cases, it is preferable to have a subscriber request a less resource-intensive subscription than to deal with frequently degraded behavior.

The solution builds on [RFC8639].  As defined therein, any loss of an underlying transport connection will be detected and result in subscription termination (in the case of dynamic subscriptions) or suspension (in the case of configured subscriptions), ensuring that situations where the loss of update notifications would go unnoticed will not occur.

## Subscription Monitoring

In the operational state datastore, the *datastore-telemetry* container maintains operational state for all configured and dynamic subscriptions.

Dynamic subscriptions are only present in the *datastore-telemetry/subscriptions/subscription* list when they are active, and are removed as soon as they are terminated.  Whereas configured subscriptions are present if the list if they are configured, regardless of whether they are active.

**TODO, should dynamic receivers be listed?  Do we need to report per-receiver stats for dynamic subscriptions?**

The operational state is important for monitoring the health of subscriptions, receivers, and the overall telemetry subsystem.

This includes:

**TODO, update the YANG model with more useful operational data, and mostly this section should briefly summarize and refer to the YANG model.  We should also consider what indications to include from filters that cause a larger amount of internal work but don't generate a large number of transmitted notifications.**

- per subscription status and counters

- per receiver status and counters

- maybe some indication of the overall load on the telemetry subsystem, but we need to consider how useful that actually is, and whether just monitoring the device CPU load and general performance would be a better indication.

<!-- TODO - Consider incorporating some aspects of this text.
Each subscription in the operational state datastore is represented as a list element.  Included in this list are event counters for each receiver, the state of each receiver, and the subscription parameters currently in effect.  The appearance of the leaf "configured-subscription-state" indicates that a particular subscription came into being via configuration.  This leaf also indicates whether the current state of that subscription is "valid", "invalid", or "concluded".

To understand the flow of event records in a subscription, there are two counters available for each receiver.  The first counter is "sent-event-records", which shows the number of events identified for sending to a receiver.  The second counter is "excluded-event-records", which shows the number of event records not sent to a receiver.  "excluded-event-records" shows the combined results of both access control and per-subscription filtering.  For configured subscriptions, counters are reset whenever the subscription's state is evaluated as "valid" (see (1) in Figure 8).
-->

## Robustness and Reliability

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

## Publisher Capacity

It is far preferable to decline a subscription request than to accept
such a request when it cannot be met.

Whether or not a subscription can be supported will be determined by
a combination of several factors, such as the subscription update
trigger (on-change or periodic), the period in which to report
changes (one-second periods will consume more resources than one-hour
periods), the amount of data in the datastore subtree that is being
subscribed to, and the number and combination of other subscriptions
that are concurrently being serviced.

# Conformance and Capabilities {#ConformanceAndCapabilities}

The capabilities model (documented at XXX) should be used by devices to advertise likely subscription capabilities.  In addition, the YANG Push Lite operational data gives an indication of the overall telemetry load on the device and hence gives an indication to whether a particular telemetry request is likely to be accepted, and honored.

**TODO Text needs updating, taken from on-change section**

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

**TODO  Do we need to add capabilities to indicate:

1. Which fields are on-change notifiable.

1. At which level *bags* exist internally (for performance reasons).

1. The points at which subscriptions are decomposed to.**

## Subscription Content Schema Identification

YANG Module Synchronization

To make subscription requests, the subscriber needs to know the YANG datastore schemas used by the publisher.  These schemas are available in the YANG library module ietf-yang-library.yang as defined in {{RFC8525}}.  The receiver is expected to know the YANG library information before starting a subscription.

The set of modules, revisions, features, and deviations can change at runtime (if supported by the publisher implementation).  For this purpose, the YANG library provides a simple "yang-library-change" notification that informs the subscriber that the library has changed.  In this case, a subscription may need to be updated to take the updates into account.  The receiver may also need to be informed of module changes in order to process updates regarding datastore

**TODO, this section should be updated so that a subscription is restarted if the schema that it is using changes, and to incorporate ideas to fingerprint the subscription schema in the subscription-started notification.**

# YANG {#ietf-yp-lite-yang}

## ietf-yp-lite YANG tree {#yp-lite-tree}

This section shows the full tree output for ietf-yp-lite YANG module.

Note, this output does not include support for any transport configuration, and at least one would expect to be configurable.

**TODO What about capabilities?  Perhaps further explanation is needed here?**

~~~~ yangtree
{::include tree-output/ietf-yp-lite-tree.txt}
~~~~
{: align="left" title="YANG tree for YANG Push Lite Module Tree Output "}

## ietf-yp-lite YANG Model {#yp-lite-yang-module}

This module imports typedefs from {{RFC6991}}, {{RFC8343}}, {{RFC8341}}, {{RFC8529}}, and {{RFC8342}}.  It references {{RFC6241}}, {{XPATH}} ("XML Path Language (XPath) Version 1.0"), {{RFC7049}}, {{RFC8259}}, {{RFC7950}}, {{RFC7951}}, and {{RFC7540}}.

This YANG module imports typedefs from {{RFC6991}}, identities from
[RFC8342], and the "sx:structure" extension from {{RFC8791}}. It also references {{RFC6241}}, {{XPATH}}, and {{RFC7950}}.

~~~~ yang
{::include yang/ietf-yp-lite.yang}
~~~~
{: align="left" sourcecode-markers="true"
sourcecode-name="ietf-yp-lite.yang#0.1.0" title="YANG module ietf-yp-lite"}

# Security Considerations {#security}

With configured subscriptions, one or more publishers could be used to overwhelm a receiver.  To counter this, notification messages SHOULD NOT be sent to any receiver that does not support this specification.  Receivers that do not want notification messages need only terminate or refuse any transport sessions from the publisher.

When a receiver of a configured subscription gets a new "subscription-started" message for a known subscription where it is already consuming events, it may indicate that an attacker has done something that has momentarily disrupted receiver connectivity. **TODO - Do we still want this paragraph?**.

For dynamic subscriptions, implementations need to protect against malicious or buggy subscribers that may send a large number of "establish-subscription" requests and thereby use up system resources.  To cover this possibility, operators SHOULD monitor for such cases and, if discovered, take remedial action to limit the resources used, such as suspending or terminating a subset of the subscriptions or, if the underlying transport is session based, terminating the underlying transport session.

Using DNS names for configured subscription's receiver "name" lookups can cause situations where the name resolves differently than expected on the publisher, so the recipient would be different than expected.

## Receiver Authorization

**TODO Relax when access control must be checked.**

**TODO Consider if this is the best place in the document, but this text needs to be updated regardless.**

A receiver of subscription data MUST only be sent updates for which it has proper authorization.  A publisher MUST ensure that no unauthorized data is included in push updates.  To do so, it needs to apply all corresponding checks applicable at the time of a specific pushed update and, if necessary, silently remove any unauthorized data from datastore subtrees.  This enables YANG data that is pushed based on subscriptions to be authorized in a way that is equivalent to a regular data retrieval ("get") operation.

Each "push-update" and "push-change-update" MUST have access control applied, as depicted in Figure 5.  This includes validating that read access is permitted for any new objects selected since the last notification message was sent to a particular receiver.  A publisher MUST silently omit data nodes from the results that the client is not authorized to see.  To accomplish this, implementations SHOULD apply the conceptual authorization model of {{RFC8341}}, specifically Section 3.2.4, extended to apply analogously to data nodes included in notifications, not just \<rpc-reply\> messages sent in response to
\<get\> and \<get-config\> requests.

~~~~~~~~~~
                      +-----------------+      +--------------------+
  push-update or -->  | datastore node  |  yes | add datastore node |
 push-change-update   | access allowed? | ---> | to update record   |
                      +-----------------+      +--------------------+
~~~~~~~~~~
{: align="left" title="Access Control for Push Updates"}


A publisher MUST allow for the possibility that a subscription's selection filter references nonexistent data or data that a receiver is not allowed to access.  Such support permits a receiver the ability to monitor the entire lifecycle of some datastore tree without needing to explicitly enumerate every individual datastore node.  If, after access control has been applied, there are no objects remaining in an update record, then the effect varies given if the subscription is a periodic or on-change subscription.  For a periodic subscription, an empty "push-update" notification MUST be sent, so that clients do not get confused into thinking that an update was lost.  For an on-change subscription, a "push-update" notification MUST NOT be sent, so that clients remain unaware of changes made to nodes they don't have read-access for.  By the same token, changes to objects that are filtered MUST NOT affect any dampening intervals.

A publisher MAY choose to reject an "establish-subscription" request that selects nonexistent data or data that a receiver is not allowed to access.  The error identity "unchanging-selection" SHOULD be returned as the reason for the rejection.  In addition, a publisher MAY choose to terminate a dynamic subscription or suspend a configured receiver when the authorization privileges of a receiver change or the access controls for subscribed objects change.  In that case, the publisher SHOULD include the error identity "unchanging-selection" as the reason when sending the "subscription-terminated" or "subscription-suspended" notification, respectively.  Such a capability enables the publisher to avoid having to support
continuous and total filtering of a subscription's content for every update record.  It also reduces the possibility of leakage of access-controlled objects.

If read access into previously accessible nodes has been lost due to a receiver permissions change, this SHOULD be reported as a patch "delete" operation for on-change subscriptions.  If not capable of handling such receiver permission changes with such a "delete", publisher implementations MUST force dynamic subscription re-establishment or configured subscription reinitialization so that appropriate filtering is installed.

## YANG Module Security Considerations

**TODO - Check that this section is still correct at WG LC, and before/after IESG Evaluation, if the YANG data model changes at all**.

This section is modeled after the template described in Section 3.7.1 of {{I-D.draft-ietf-netmod-rfc8407bis}}.

The "ietf-yp-lite" YANG module defines a data model that is designed to be accessed via YANG-based management protocols, such as NETCONF {{RFC6241}} and RESTCONF {{RFC8040}}. These protocols have to use a secure transport layer (e.g., SSH {{RFC4252}}, TLS {{RFC8446}}, and QUIC {{RFC9000}}) and have to use mutual authentication.

The Network Configuration Access Control Model (NACM) {{RFC8341}} provides the means to restrict access for particular NETCONF or RESTCONF users to a preconfigured subset of all available NETCONF or RESTCONF protocol operations and content.

There are a number of data nodes defined in this YANG module that are writable/creatable/deletable (i.e., "config true", which is the default).  All writable data nodes are likely to be reasonably sensitive or vulnerable in some network environments.  Write operations (e.g., edit-config) and delete operations to these data nodes without proper protection or authentication can have a negative effect on network operations.  The following subtrees and data nodes have particular sensitivities/vulnerabilities:

- There are no particularly sensitive writable data nodes.

Some of the readable data nodes in this YANG module may be considered sensitive or vulnerable in some network environments.  It is thus important to control read access (e.g., via get, get-config, or notification) to these data nodes. Specifically, the following subtrees and data nodes have particular sensitivities/vulnerabilities:

- There are no particularly sensitive readable data nodes.

Some of the RPC or action operations in this YANG module may be considered sensitive or vulnerable in some network environments. It is thus important to control access to these operations. Specifically, the following operations have particular sensitivities/vulnerabilities:

- kill-subscription - this RPC operation allows the caller to kill any dynamic subscription, even those created via other users, or other transport sessions.

**TODO - As per the template in {{I-D.draft-ietf-netmod-rfc8407bis}}, we would need to add text for groupings if we add any groupings from elsewhere, or the modules define groupings that are expected to be used by other modules.**

# IANA Considerations

This document registers the following namespace URI in the "IETF XML Registry" {{RFC3688}}:

URI: urn:ietf:params:xml:ns:yang:ietf-yp-lite

Registrant Contact: The IESG.

XML: N/A; the requested URI is an XML namespace.

This document registers the following YANG module in the "YANG Module Names" registry {{RFC6020}}:

Name: ietf-yp-lite

Namespace: urn:ietf:params:xml:ns:yang:ietf-yp-lite

Prefix: ypl

Reference: RFC XXXX

# Acknowledgments
{:numbered="false"}

This inital draft is early work is based on discussions with various folk, particularly Thomas Graf, Holger Keller, Dan Voyer, Nils Warnke, and Alex Huang Feng; but also wider conversations that include: Benoit Claise, Pierre Francois, Paolo Lucente, Jean Quilbeuf, among others.

--- back

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

## Subscription RPC examples (from RFC 8641)

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
{{RFC8640}}.  For example, for the following NETCONF request:

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
