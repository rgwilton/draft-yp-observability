---
<<<<<<< HEAD
###
# Internet-Draft Markdown Template
#
# Rename this file from draft-todo-yourname-protocol.md to get started.
# Draft name format is "draft-<yourname>-<workgroup>-<name>.md".
#
# For initial setup, you only need to edit the first block of fields.
# Only "title" needs to be changed; delete "abbrev" if your title is short.
# Any other content can be edited, but be careful not to introduce errors.
# Some fields will be set automatically during setup if they are unchanged.
#
# Don't include "-00" or "-latest" in the filename.
# Labels in the form draft-<yourname>-<workgroup>-<name>-latest are used by
# the tools to refer to the current version; see "docname" for example.
#
# This template uses kramdown-rfc: https://github.com/cabo/kramdown-rfc
# You can replace the entire file if you prefer a different format.
# Change the file extension to match the format (.xml for XML, etc...)
#
###
=======
>>>>>>> main
title: "YANG Push Operational Data Observability Enhancements"
abbrev: "Yang Push Observability"
category: info

docname: draft-wilton-netconf-yp-observability-latest
submissiontype: IETF
number:
date:
consensus: true
v: 3
<<<<<<< HEAD
area: OPS
workgroup: NETCONF
=======
area: "Operations and Management"
workgroup: "Network Configuration"
>>>>>>> main
keyword:
 - YANG Push
 - Observability
 - Network Telemetry
 - Operational Data
venue:
<<<<<<< HEAD
  group: WG
  type: Working Group
  mail: WG@example.com
  arch: https://example.com/WG
  github: USER/REPO
  latest: https://example.com/LATEST
=======
  group: "Network Configuration"
  type: "Working Group"
  mail: "netconf@ietf.org"
  arch: "https://mailarchive.ietf.org/arch/browse/netconf/"
  github: "rgwilton/draft-yp-observability"
  latest: "https://rgwilton.github.io/draft-yp-observability/draft-wilton-netconf-yp-observability.html"
>>>>>>> main

author:
 -
    fullname: Robert Wilton
    organization: Cisco Systems
    email: rwilton@cisco.com

normative:
  RFC8641:

informative:
  I-D.ietf-nmop-network-anomaly-architecture:
  I-D.ietf-nmop-yang-message-broker-integration:

--- abstract

TODO Abstract


--- middle

# Introduction

{{I-D.ietf-nmop-yang-message-broker-integration}} describes an architecture for how YANG Push {{RFC8641}} can be integrated effectively with message brokers (e.g., Apache Kafka), that is part of a wider architecture for a *Network Anomaly Detection Framework*, specified in {{I-D.ietf-nmop-network-anomaly-architecture}}.

YANG-Push is a key part of these architectures, but through experience of implementing YANG Push specifically for the use cases described in the above architecture documents, it became clear that there are aspects of YANG Push that are not optimal for these use cases, particular as they relate to operational data, both neither producer or consumer.

For the consumer of the telemetry data, there is a requirement to associate a schema with the provided data.  It is much more helpful for the schema to be associated with the individual messages rather than at the root of the operational datastore.  As such, it is helpful for the encoded instance data to be rooted at subscription path rather than at the root of the operational datastore.

The YANG abstraction of a single datastore of related consistent data works well for configuration that has a strong requirement to be self consistent, and that is always updated in a transactional way.  But for producers of telemetry data, the YANG abstraction of a single operational datastore is not really possible for non trivial devices.  Some systems may store the operational data in a single logical database, it is less likely that data can always be updated in a transactional way, and often for memory efficiency reasons such a database does not store individual leaves, but instead semi-consistent records of data.  For other systems, the operational information may be distributed across multiple internal nodes (e.g., linecards), and potentially many different daemons within those nodes.  Such systems generally cannot exhibit full consistency of the operational data, only offering an eventually consistent view.  In reality, many devices will manage their operational data as a combination of some data being stored in a central operational datastore, and other, higher scale, and more frequently changing data (e.g., statistics or FIB information) being stored elsewhere in a more memory efficient and performant way.

Hence, this document defines some minor extensions to YANG Push that are designed to make YANG Push work better both for producers and consumers of YANG telemetry data.

# YANG Push enhancements

This document currently:

- Defines a new yang push encoding format that can be used for both on-change and periodic subscriptions that reports the data from the subscription filter point.

- Defines a combined period and on-change subscription that reports events both on a periodic cadence and also if changes to the date have occurred.

## New encoding format

// Should consider scope of XPath, i.e., what is allowed.
// Could we consider use JsonPath instead?  https://datatracker.ietf.org/doc/html/rfc9535

## Combined periodic and on-change subscription

## Open Issues & Other Potential Enhancements/Changes

This section lists some other potential issues and enhancements that should be considered as part of this work.  If there is working group interest in progressing this work, then the issues in this section could potentially be better managed as github issues.

1. Should we consider a version of the JSON encoding that excludes module prefixes (either everywhere, or perhaps only include the top module prefix).  The reasoning for considering this is to potentially better align the JSON data with how the schema data may be modeled in other data systems (e.g., Kafka).  Obviously, this requires that there be no duplicate data node names in different module namespaces.

1. Do we make use of the new notification-envelope format as the mandatory and only required notification format for these new forms of subscriptions.  I.e., reducing complexity by removing unnecessary options?

1. Document how sub-subscriptions can be used to split up a higher level subscription into smaller more efficient subscriptions for the device (that can be handled concurrently).

1. The document's current focus is on configured subscriptions, aligned to the proposed initial deployment requirements, but the solution should probably be extended to support dynamic subscriptions, presuming that it is not hard to do so.

1. Some of the YANG Push behavior is more complex and expensive to implement (e.g., the SHOULD requirement to suggest suitable alternative subscription parameters if a subscription is rejected, subscription dependencies).  Should this document update that RFC 8639 or RFC 8641 to indicate that those requirements do not apply to these new extended subscriptions?  The goal of this work should be to specify the minimal required functionality to meet the requirements.

1. What document format should this work take?  The currently proposed approach is to add extra extensions to YANG Push to cover the required functionality.  An alternative approach could be to write a RFC 8641-bis, although it is unclear exactly what format that should take.

1. Currently the encoding and transport parameters are per subscription, but it may make more sense for these to be per receiver definition.  I.e., if you want to use different transports and encodings to the same receiver this should still be possible, but would require a second receiver to be defined with the same destination IP address, but a different name.  Currently, the newly proposed encoding format is configured per receiver, but alternatively it could be configured per subscription (that would better mirror the existing per-subscription transport and encoding configuration leaves).

<!--
# Should the appendix contain a list of features in subscriptions notifications and YANG Push
# that are probably not needed?
#
# E.g., is subscription modified useful (as opposed to just tearing down the subscription and starting again, which clients may handle from a robustness perspective anyway).
-->

## YANG Extensions Data Model

~~~~ yang
{::include yang/ietf-yp-ext.yang}
~~~~
{: align="left" sourcecode-markers="true" sourcecode-name="ietf-yp-ext@2024-10-18.yang" title="YANG module ietf-yp-ext"}

# Conventions and Definitions

{::boilerplate bcp14-tagged}


# Security Considerations

TODO.  New YANG models will be defined that need to document their security considerations, but otherwise the security considerations in YANG Push should be sufficient.
TODO Security


# IANA Considerations

TODO - This document will need to register new YANG models with IANA.


--- back

# Acknowledgments
{:numbered="false"}

This is early work is based on discussions with various folk, particularly Thomas Graf, Holger Keller, Dan Voyer, Nils Warnke, and Alex Huang Feng; but also wider conversations that include: Benoit Claise, Pierre Francois, Paolo Lucente, Jean Quilbeuf, and others.

