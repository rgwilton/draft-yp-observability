---
title: "YANG Push Lite: Operational Data Observability Enhancements"
abbrev: "YANG Push Lite"
category: std

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
  - fullname: Robert Wilton
    organization: Cisco Systems
    email: rwilton@cisco.com
  - fullname: James Cumming
    organization: Nokia
    email: james.cumming@nokia.com
  - fullname: Ebben Aries
    organization: Juniper Networks
    email: exa@juniper.net

normative:
  RFC8641:
  RFC8639:
  RFC7951:
  RFC9254:
  RFC7950:

informative:
  I-D.ietf-nmop-network-anomaly-architecture:
  I-D.ietf-nmop-yang-message-broker-integration:
  I-D.draft-netana-netconf-notif-envelope:
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
  Eventual Consistency:
    target: https://www.techopedia.com/definition/29165/eventual-consistency
    title: Eventual Consistency
    author:
      - name: Margaret Rouse
    date: false

--- abstract

This early draft proposes a new, streamlined version of YANG-Push called YANG push lite, in order to optimize its behavior for operational data telemetry.  It also addresses some additional issues and potential resolutions.

--- middle

# Conventions and Definitions

{::boilerplate bcp14-tagged}

# Introduction

{{I-D.ietf-nmop-yang-message-broker-integration}} describes an architecture for how YANG-Push {{RFC8641}} can be integrated effectively with message brokers, e.g., {{Kafka}}, that is part of a wider architecture for a **Network Anomaly Detection Framework**, specified in {{I-D.ietf-nmop-network-anomaly-architecture}}.

YANG-Push is a key part of these architectures, but through experience of implementing YANG-Push specifically for the use cases described in the above architecture documents, it became clear that there are aspects of YANG-Push that are not optimal for these use cases for neither producer or consumer, particular as they relate to operational data.

## Complexities in Modelling the Operational State Datastore

The YANG abstraction of a single datastore of related consistent data works very well for a configuration that has a strong requirement to be self consistent, and that is always updated, and validated, in a transactional way.  But for producers of telemetry data, the YANG abstraction of a single operational datastore is not really possible for devices managing a non-trivial quantity of operational data.

Some systems may store their operational data in a single logical database, yet it is less likely that the operational data can always be updated in a transactional way, and often for memory efficiency reasons such a database does not store individual leaves, but instead semi-consistent records of data at a container or list entry level.

For other systems, the operational information may be distributed across multiple internal nodes (e.g., linecards), and potentially many different process daemons within those distributed nodes.  Such systems generally cannot exhibit full consistency {{Consistency}} of the operational data (which would require transactional semantics across all daemons and internal nodes), only offering an eventually consistent {{Eventual Consistency}} view of the data instead.

In practice, many network devices will manage their operational data as a combination of some data being stored in a central operational datastore, and other, higher scale, and potentially more frequently changing data (e.g., statistics or FIB information) being stored elsewhere in a more memory efficient and performant way.


# YANG push lite

To address the needs described in the introduction and architecture documents, this document defines a new protocol 'YANG push lite'
that is designed to provide a more efficient protocol for both producers and consumers of YANG telemetry data.

Currently, it:

- Uses {{RFC8639}} to define what a subscription is and how to create it.  This includes the terminology defined in this RFC such as  configured subscriptions and dynamic subscriptions [TBD]
- Replaces {{RFC8641}} [TBD]
- Defines a single message type for both 'on change' and periodic updates
- Provides an extensible option for data encoding
- Provides the concept of a heartbeat interval to 'on change' subscripitons
- Adds a new subscripiton type named periodic-on-change and describes the differences between this and on-change with heartbeat
- Provides an end-of-sync marker
- Clarifies the behaviour when authorization changes (local and remote)
- Removes the YANG patch format
- Mandates the use of the YANG push envelope header as defined in {{I-D.draft-netana-netconf-notif-envelope}}
- Provides a subscription-path option in addition to a path option
- Removes the negotiation phase when setting up subscriptions replacing it with a simple accept/reject at that instantanous point in time
- Describes that only "change" and "delete" updates are sent


## Single message type for on-change and periodic updates

The original YANG push drafts define two message types for updates ("push-update" and "push-change-update").

Collectors do not need to identify update messages between stream updates and on-change updates, as such a unified
message type called "push-update-notification" is defined that is provided for all updates no matter what the
subscription type is.


## Provides an extensible option for data encoding

NETCONF is an XML based protocol and all NETCONF operations and notifications are defined in XML.  Inside NETCONF
operations and notifications is encoded data.  This encoded data can be delivered in multiple encodings, there need
not necessarily be a limitation on what encoding this data takes as long as the server/producer can encode it and the
client/receiver can decode it.

There are a number of common data encodings in use today (and already standardised by the IETF), however, there may
be new encodings in the future.  This draft, thereforem, will provide an extensible encoding selection to allow
for future additions.

The three data encodings provided initially in this draft are:

- XML as defined in {{RFC7950}}
- JSON_IETF as defined in {{RFC7951}}
- CBOR as defined in {{RFC9254}}

This draft does not mandate any specific data encoding nor does it define the default encoding, instead, leaving the
encoding selection of choice to the consumer (or producer in the case of a configured subscription).

The establish-subscription operation is extended with an "encoding" option to determine the encoding type.  This "encoding" leaf is an enumeration with the following options: xml, json_ietf, cbor


## Provides the concept of a heartbeat interval to on-change subscripitons

An on-change subscripiton delivers notification update messages when a change is determined in a specific node.  Often
there are no changes over a period of time and it is important for a management system to be able to identify whether
the session is still operating correctly and to ensure that their current view of the environment is correct.

The heartbeat option is provided (with a value provided in seconds) along with an on-change subscription.  Upon the
expiry of this timer the current state of all paths in the subscriptions is sent to the consumer regardless of whether
it has changed or not.

## Adds a new subscripiton type named periodic-on-change and describes the differences between this and on-change with heartbeat

Sometimes it is helpful to have a single subscription that covers both periodic and on-change notifications.


There are two ways in which this may be useful:

1. For generally slow changing data (e.g., a device's physical inventory), then on-change notifications may be most appropriate.  However, in case there is any lost notification that isn't always detected, for any reason, then it may also be helpful to have a slow cadence periodic backup notification of the data (e.g., once every 24 hours), to ensure that the management systems would should always eventually converge on the current state in the network.

1. For data that is generally polled on a periodic basis (e.g., once every 10 minutes) and put into a time series database, then it may be helpful for some data trees to also get more immediate notifications that the data has changed.  Hence, a combined periodic and on-change subscription, potentially with some dampening, would facilitate more frequent notifications of changes of the state, to reduce the need of having to always wait for the next periodic event.

Hence, this document introduces the fairly intuitive "periodic-and-on-change" update trigger that creates a combined periodic and on-change subscription, and allows the same parameters to be configured.  For some use cases, e.g., where a time-series database is being updated, the new encoding format proposed previously may be most useful.





These are detailed in the following sections:

## New encoding format

This document proposes a new opt-in YANG-Push encoding format to use instead of the "push-update" and "push-change-update" notifications defined in {{RFC8641}}.

There are a few reasons for specifying a new encoding format:

1. To use the same encoding format for both periodic and on-change messages, allowing the same messages to be easily received and stored in a time-series database, making use of the same message schema when traversing message buses, such as Apache Kafka.

1. To allow the schema of the notifications to be rooted to the subscription point rather than always being to the root of the operational datastore schema.  This allows messages to be slightly less indented, and makes it easier to convert from a YANG schema to an equivalent message bus schema, where each message is defined with its own schema, rather than a single datastore schema.

1. To move away from the somewhat verbose YANG Patch format {{RFC8072}}, that is not really a great fit for encoding changes of operational data.  Many systems cannot necessarily distinguish between create versus update events (particularly for new subscriptions or after recovering from internal failures within the system), and hence cannot faithfully implement the full YANG Patch semantics defined in {{RFC8641}}.

1. To allow the device to split a subscription into smaller child subscriptions for more efficient independent and concurrent processing.  I.e., reusing the ideas from {{?I-D.ietf-netconf-distributed-notif}}.  However, all child subscriptions are still encoded from the same subscription point.

The practical differences in the encodings may be better illustrated via the examples in {{Examples}}.


## Combined periodic and on-change subscription



## Open Issues & Other Potential Enhancements/Changes

This section lists some other potential issues and enhancements that should be considered as part of this work.  If there is working group interest in progressing this work, then the issues in this section could potentially be better managed as github issues.

1. Should we consider a version of the JSON encoding that excludes module prefixes (either everywhere, or perhaps only include the top module prefix).  The reasoning for considering this is to potentially better align the JSON data with how the schema data may be modeled in other data systems, e.g., Kafka.  Obviously, this requires that there be no duplicate data node names in different module namespaces, but most sane device schemas would avoid this anyway.

1. Do we make use of the new notification-envelope format {{?I-D.netana-netconf-notif-envelope}} as the mandatory and only required notification format for these new forms of subscriptions.  I.e., reducing complexity by removing unnecessary flexibility and options?

1. Document how sub-subscriptions can be used to split a higher level subscription into multiple smaller more efficient subscriptions for the device (that can be handled concurrently).

1. The document's current focus is on configured subscriptions, aligned to the proposed initial deployment requirements, but the solution should probably be extended to support dynamic subscriptions, presuming that it is not hard to do so.

1. Some of the YANG-Push behavior is more complex and expensive to implement (e.g., the SHOULD requirement to suggest suitable alternative subscription parameters if a subscription is rejected, subscription dependencies).  Should this document update RFC 8639 or RFC 8641 to indicate that those requirements do not apply to these new extended subscriptions?  The goal of this work should be to specify the minimal required functionality to meet the requirements.

1. What document format should this work take?  The currently proposed approach is to add extra extensions to YANG-Push to cover the required functionality.  An alternative approach could be to write a RFC 8641-bis, or a 'YANG-Push lite'.

1. Currently the encoding and transport parameters are per subscription, but it may make more sense for these to be per receiver definition.  I.e., if you want to use different transports and encodings to the same receiver this should still be possible, but would require a second receiver to be defined with the same destination IP address, but a different name.  Currently, the newly proposed encoding format is configured per subscription (mirroring equivalent transport and encoding configuration), but alternatively it could be configured per receiver.

1. We should consider how a subscription could support multiple subscription paths.  The potential tricky aspect of this to consider the subscription bind point.  Related to this is whether XPath 1.0 is the best way of specifying these bind points, or whether it should something closer to the NACM node-instance-identifier {{?RFC6536}}, but perhaps using something closer to the JSON style encoding of instance identifier {{RFC7951}}, section 6.11; or JSON PATH {{?RFC9535}}.

1. What level of subscription filtering do we need and want to support?  For example, I doubt that anyone allows for full XPath filtering of operational data subscriptions because they are likely to be very computationally expensive to implement.  Is there an easier way of expressing the filter requirements rather than using subtree filtering.  Note, this could be added in a future release.

<!--
# Should the appendix contain a list of features in subscriptions notifications and YANG-Push
# that are probably not needed?
#
# E.g., is subscription modified useful (as opposed to just tearing down the subscription and starting again, which clients may handle from a robustness perspective anyway).
-->

## YANG Extensions Data Model

This sections shows the raw YANG tree output just for the ietf-yp-ext.yang module, and the proposed YANG module.

### YANG Tree
~~~~ yangtree
{::include yang/ietf-yp-ext.tree.txt}
~~~~
{: align="left" title="YANG tree for ietf-yp-ext.yang"}

### YANG file
~~~~ yang
{::include yang/ietf-yp-ext.yang}
~~~~
{: align="left" sourcecode-markers="true" sourcecode-name="ietf-yp-ext@2024-10-18.yang" title="YANG module ietf-yp-ext"}

# Security Considerations

TODO.  New YANG models will be defined that need to document their security considerations, but otherwise the security considerations in YANG-Push should be sufficient.

# IANA Considerations

TODO - This document will need to register new YANG models with IANA.

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

# Examples {#Examples}

Notes on examples:

- These examples have been given using a JSON encoding of the regular YANG-Push notification format, i.e., encoded using {{!RFC5277}}, but it is anticipated that these notifications could be defined to exclusive use the new format proposed by {{?I-D.netana-netconf-notif-envelope}}.

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

## Example of periodic updates using the new style update message.

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


## Example of an on-change-update notification using the new style update message.

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

## Example of an on-change-delete notification using the new style update message.


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
