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