# YANG Push v2 Publisher

A Scala tool that generates sample YANG Push v2 JSON notifications using the
`envelope` structure defined in `yang/ietf-yang-push-2.yang`.

## Usage

Run from the repository root:

```bash
./mill --no-server publisher.compile
./mill --no-server publisher.run generate -i 2 -p
```

The default output is one notification with an `ietf-yang-push-2:envelope`
wrapper, `event-time`, `hostname`, `sequence-number`, and `contents`.
Subscription IDs are strings. The payload uses the v2 `updates/update`
field and reports collection completion with `complete`.

## Options

- `-i, --interfaces <int>`: number of interfaces (default: 4).
- `-p, --pretty`: indent the sample JSON.
- `-n, --no-filter`: retain redundant module prefixes for debugging.
- `-e, --no-envelope`: output notification payloads for inspection or validation.
  These are fragments, not complete notifications for transmission.
- `-s, --splits <int>`: split the interface list across separate notifications.
- `--split-provider`: split interface, statistics, and IP data into separate
  notifications.
- `--publisher-id <uint32>`: publisher process ID (default: 0). The header omits
  the default ID and includes nonzero IDs.
- `-o, --output <path>`: write JSON directly to a file, without build output.

Splitting produces a JSON array of sample notifications. The array is a file
format for collecting examples, not an additional protocol wrapper. Each
array element has its own envelope and increasing sequence number. Messages
from the same collection share an event time; only the last sets `complete`
to true. Each message has one update entry, avoiding duplicate target paths
within a notification.

```bash
./mill --no-server publisher.run generate -i 4 -s 2 -p
./mill --no-server publisher.run generate -i 2 --split-provider --publisher-id 4 -p
./mill --no-server publisher.run generate -i 2 -e -p
./mill --no-server publisher.run generate -i 4 -p -o publisher/sample-output.json
```

## Envelope and namespaces

RFC 7951 namespace filtering is applied by default. The top-level name is
`ietf-yang-push-2:envelope`. Because the envelope and notification are now in
the same module, the nested notification name is `update`. When `-e` omits
the envelope, the top-level notification name is `ietf-yang-push-2:update`.
Nodes from other modules retain their module names where required.

For example, a complete notification can contain:

```json
{
  "ietf-yang-push-2:envelope": {
    "event-time": "2025-10-21T10:00:00Z",
    "hostname": "example-router",
    "sequence-number": 3219,
    "contents": {
      "update": {
        "id": "interfaces",
        "snapshot-type": "periodic",
        "observation-time": "2025-10-21T10:00:00Z",
        "updates": [{
          "target": "/ietf-interfaces:interfaces/interface[name='Eth0']",
          "update": {"name": "Eth0", "enabled": true}
        }],
        "complete": true
      }
    }
  }
}
```

## NETCONF examples

The RPC helpers use the `ietf-yang-push-2` namespace and string subscription
IDs. NETCONF examples request XML encoding.

```bash
./mill --no-server publisher.run netconfExample -o /tmp/establish-subscription.xml
./mill --no-server publisher.run netconfReply -i dyn-52 -o /tmp/subscription-reply.xml
```

## Models and sample data

The canonical notification model is `yang/ietf-yang-push-2.yang`; the publisher
does not bundle a separate envelope module. Supporting interface and IP
models are in `publisher/resources/`:

- `ietf-interfaces@2018-02-20.yang` (RFC 8343).
- `ietf-ip@2018-02-22.yang` (RFC 8344).
- `ietf-inet-types` and `ietf-yang-types`.

Sample interfaces are named Eth0 through Eth3 by default and use documentation
IPv4 and IPv6 addresses. Partial interface, statistics, and IP records can be
produced with `--split-provider`.
