# Publisher validation notes

The v2 alignment checks cover these generated outputs:

| Mode | Command arguments after `publisher.run generate` | Expected result |
| --- | --- | --- |
| Default | `-i 4 -p` | One v2 envelope; publisher ID 0 omitted |
| List split | `-i 4 -s 2 -p` | Two independently enveloped messages |
| Provider split | `-i 2 --split-provider --publisher-id 4 -p` | Six messages, each with publisher ID 4 |
| Payload only | `-i 2 -e -p` | Top-level `ietf-yang-push-2:update`; no envelope |
| Debug prefixes | `-i 1 -n -p` | V2 names retained with redundant prefixes |

For enveloped modes, check the mandatory event time, hostname, sequence
number, and exactly one notification in `contents`. Check string subscription
IDs, the `updates/update` operation, unique target paths per notification,
shared collection event time, increasing sequence numbers, and `complete`
only on the final message.

Split sample output is a JSON array whose elements are individual notification
messages. Payload-only output remains an explicitly requested inspection and
validation mode; it is not a full wire notification.

The NETCONF request and reply helpers should use the `ietf-yang-push-2`
namespace and string IDs. The request should select XML encoding.

Run `./mill --no-server all` after changes to rebuild the draft and validate
the maintained YANG notification payload examples. Compile the publisher with
`./mill --no-server publisher.compile` and exercise the modes above separately.
