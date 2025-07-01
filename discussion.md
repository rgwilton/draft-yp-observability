Document updates:
- support multiple receivers, must have the same encoding.


#Closed issues
The following issues have been discussed and closed.

- Rename subscription-terminated to subscription-stopped
   - Rejected 21 Feb 25, unnecessary renaming.

- MUST use envelope, hostname and sequence-number (and event-time).

- Don't mandate configured or dynamic subscriptions, allow implementations to implement one or both of them.


Issue discussion:

  - Should we support configurable subscription-level keepalives?
     - Holger, yes, this will be needed.
     - Rob, should this be solved in the transport layer rather than application layer.
     - Thomas, no thinking of a subscription level keepalive.  If we do keepalives then do it at the application level and do it as far as the collection process.  Only thinking for on-change subscriptions.
     - Thomas thinking of doing this per publisher-id.
     - Benoit: Who is going to receive this keepalive
     - Thomas: Message broker.
     - Thomas: at a minimum joint period and on-change subscription.  A keepalive is one further optimization.
     - Ebben: Is this just a small message, Rob: yes.
     - Benoit: Is this the right way of solving this (i.e., in the protocol itself)
     - Open for further discussion.

James examples of Nokia JsonPaths for subscription paths:
       - James example: /nokia-state:state/router[router-name="Base"]/bgp/neighbor[ip-address="1.1.1.1"]
       - /openconfig-interfaces:interfaces/interface[name="1/1/c2/1"]/subinterfaces/subinterface[index=0]/openconfig-if-ip:ipv4/addresses

Transports:

1. {{transports}} lists quite a lot of rules on what are valid transports and what negotation/etc is required.  I think we need to check whether we can weaken some of these (although it is possible that these were imposed during a transport directorate review).
    - James: Authorization should be done at the application layer.
    - Benoit: Should have text about running this in a controlled environment.
    - Thomas: Any transport protocol MUST have the option for securing the protocol, but MAY offer a solution that runs in a uncontrolled environment (e.g., in a lab). *Consensus on 21 Feb 25 was that we should have text like this*.
    - Thomas: In an operational environment the security might be done at another layer.  Could also refer to base transport draft.  Thomas to provide RFC (best link: The best I could find is https://www.rfc-editor.org/rfc/rfc8085.html#section-3.6).
    - **Rob: Authors, I've updated the transport section, {{transports}}, please can you re-review.**

Do we need to be able to reset configured subscriptions/receivers:
   - Holger yes.
   - Benoit: What is the advantage here?
   - Ebben: Like clearing a BGP neighbour.
   - Thomas: agnostic to this.
   - Probably keep this in for the moment.
   - Further discussion on Tuesday:
      - Thomas: Prefers reset on subscription.
      - James: Would also like to see a reset on the receiver.  Would this affect all subscriptions on the receiver or just impacts that receiver?
      - James: Resetting a subscription would effectively reset all receivers tied to that subscription (Rob is concerned that this would impact other subscriptions).  Benoit raised the issue that this would impact counters if counters were also reset.
   - Does the reset RPC need rate limiting at all?

Notes from encodings discussion text:

   - Do we mandate that all implementations SHOULD support particular encodings (e.g., JSON, or JSON and CBOR)?
      - Holger: MUST for JSON, SHOULD t.b.d.,
      - James: MAY for all.
      - Rob: SHOULD for JSON & CBOR (because JSON may not be popular in future ...)
      - Thomas: Already has been discussed and let the industry.  Must be discoverable.  Also need to consider the errata.  https://www.rfc-editor.org/errata/eid6211

   - Also need to consider how CBOR SIDs would be managed.  Open issue for this for future discussion.


------------------

High level points for discussion:

- Receiver configuration, are transport sessions per configured subscription.
- Referenced filter policies, or always inline (for simplification).
- Simplifed JSON filter path.  What about regex matching on the keys, what about simplified filtering.  I.e., something simpler than a full subtree filter.
- Identifying the schema associated with a subscription (complicated by support multiple sensor paths, or a subtree filter).


Notes from filtering discussion:
     - This path should support at least support exact match and wildcard match of keys (perhaps with some restrictions).

     - Regex matching of keys is probably also a good idea (but would this be YANG's Regex or the new IETF draft for a basic regex language).

     - Do we want to allow any more filtering (e.g., single level inclusion/exclusion list), or do we punt that subtree filters (which can end up being arbitrarily complex). 
        - Not doing this for now.

   - I've currently retained subtree filtering as the more advance form of filtering.

   - I would propose that we remove support for XPath filtering from this draft (since an implementation could always augment it back in, or it could in an extension draft), and vendor implementations generally don't implement XPath filtering consistently or fully, and it has the potential to allow for subscriptions that would be very hard to implement in a performant way, and hard to police the performance impact.  If we do retain it, then it should go under a feature statement and be entirely optional to implement.
     - Consensus: Leave this as an optional to implement (MAY).


Decomposing subscriptions:
 - Every subscription has a name (either provided by the user or dynamically allocated for dynamic subscriptions):
   - This is the primary key for the subscription.

   - Periodic subscription data does not need to be sent it a single message, it can be sent in separate updates.
   - If split the should send a single update complete message on the subscription to indicate when all data in a periodic update has been sent.
   - Allow the data to be encoded from the root, or the subscription bind point.
   - All data should either be encoded from the root or relative to the subscription.


 - A subscription is named and to one path (not a set of paths)
   - Name is either explicitly provided by the user or allocated dynamically for a dynamic subscription
   - May include filters on the keys (wildcard, regex or exact match)
   - Can exclude subtrees of data to be returned relative to the subscription point.
   - This is the path that the data is encoded relative to.
   - Want both on-change and periodic to both encode from the same point.
   - TODO - Subscription may be decomposed into child subscriptions, each of which gets a different numeric subscription id.

// The subscription started messages should indicate:
- The subscription path
- Any relative subpath/filter for the child subscription.

    +---n update
       +--ro id?                 subscription-id
       +--ro path-prefix?        string
       +--ro snapshot-type?      enumeration
       +--ro observation-time?   yang:date-and-time
       +--ro updates* [target-path]
       |  +--ro target-path    string
       |  +--ro data?          <anydata>
       +--ro incomplete?         empty

- The notification data itself (i.e., in the data leaf node above), can be encoded relative to 3 separate points in the schema tree:
    - Encode from the root of the data tree (e.g., as is done an RFC 8641 periodic subscription)
    - Encode relative to the data node identified by the target-path, i.e., how RFC 8641 encodes for on-change subscriptions.
    - Encode relative to the data node subtree in the subscription path (i.e., for both for periodic and on-change)

For periodic subscriptions:

- The target-path, relative to the path-prefix, *ALWAYS* identifies the object that is being replaced (excluding filtered information) (or deleted).  This could be lower than the subscription point (e.g., for a decomposed subscription), but would never be higher than the subscription point, i.e., all data returned MUST be contained within the subscription subtree.
  - By default, it makes most sense for the data to be encoded relative to the target path (but it could be encoded relative to other points as well)
    - Not for a decomposed subscription, even for periodic, this target path would end up being below the subscription target.
  - If you are updating a datastore snapshot of the data, then you would logically replace the 

- The path-prefix contains the static part of the subscription path that doesn't contain any wildcard keys.
- T
Returned data (ignoring keys) must always be returned at or below the subscription point.
  - If there are any wildcard keys in the subscription path (i.e., anything that matches more than one list entry), then those keys will need to be expressed somehow, either in a path expression or within the JSON data itself, which would naturally require the encoding to be higher.
  - For the Kafka use case there is a strong desire to encode the data relative to the subscription point, since this would most naturally map on to a sane Kafka schema per YANG Push message.
  - Different types of subscriptions (e.g., periodic, on-change, combined) have different encodings that make sense.
  - Child subscriptions mean that only a subset of the data is being returned.

PROPOSAL for YP Lite:
- Data is always encoded relative to the subscription path (what about lists at the end)?


 - Specify the relative subscription point (e.g, where data is encoded from)

- There are three sensible points that data can be encoded from:


  - Logically the encoding is always split into two parts:
    - A path (containing keys) that identifies the object that is being updated.  [TODO - How would this be encoded for CBOR]
    - The update itself (encoded at JSON or CBOR).


If the data is being validated 

  - Schema of notifications is relative to the object identified in the subscription path.
  - A notification message contains a list of tree updates.

- Choices of where the data is encoded from:


- A path to identify what data is being updated/replaced.

 - Same encoding for both on-change and periodic:
   - Update message is either a replace, or a delete (on-change only), with a delete represented as an empty {}.
