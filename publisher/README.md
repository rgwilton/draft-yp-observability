# YANG Push Lite Publisher

A Scala-based tool to generate sample JSON output conforming to the YANG Push Lite specification.

## Features

- Generates JSON compliant with RFC 7951 (JSON encoding for YANG)
- Uses the notification envelope from draft-ietf-netconf-notif-envelope-03
- Models data based on ietf-interfaces (RFC 8343) and ietf-ip (RFC 8342)
- Implements RFC 7951 namespace filtering by default (redundant module prefixes are removed)
- Configurable number of sample Ethernet interfaces
- Pretty-print option for human-readable output

## Usage

### Build and Run

Using Mill build tool:

```bash
# Compile
./mill publisher.compile

# Run with default settings (4 interfaces, RFC 7951 filtering applied)
./mill publisher.run

# Run with options
./mill publisher.run -i 2 -p
```

### Command Line Options

- `-i, --interfaces <int>` - Number of interfaces to generate (default: 4)
- `-p, --pretty` - Pretty print output with indentation
- `-n, --no-filter` - Disable RFC 7951 namespace filtering (keep all module prefixes)

### Examples

Generate 4 interfaces with RFC 7951 filtering (default):
```bash
./mill publisher.run -p
```

Generate 2 interfaces without filtering (shows all module prefixes):
```bash
./mill publisher.run -i 2 -p -n
```

Generate output for piping to a file:
```bash
./mill publisher.run -i 4 > output.json
```

## Sample Data

The tool generates:
- **4 Ethernet interfaces** (Eth0-Eth3) by default
- **3 interfaces "up"** with traffic flowing (non-zero counters)
- **1 interface "admin-down"** with zero counters (Eth3)
- **IPv4 addresses** from RFC 5737 TEST-NET-1 range (192.0.2.0/24)
- **IPv6 addresses** from RFC 3849 documentation range (2001:db8::/32)

## RFC 7951 Namespace Filtering

By default, the tool applies RFC 7951 rules for namespace filtering:

- Top-level elements always include module prefix (e.g., `ietf-yp-notification:envelope`)
- Child elements omit the prefix if they belong to the same module as parent
- Child elements include the prefix if they belong to a different module (e.g., `ietf-ip:ipv4` under an `ietf-interfaces:interface`)

### Example with filtering (default):
```json
{
  "ietf-yp-notification:envelope": {
    "event-time": "2025-10-21T10:00:00Z",
    "contents": {
      "ietf-yp-lite:update": {
        "updates": [{
          "data": {
            "ietf-interfaces:interfaces": {
              "interface": [{
                "name": "Eth0",
                "admin-status": "up",
                "ietf-ip:ipv4": {
                  "enabled": true
                }
              }]
            }
          }
        }]
      }
    }
  }
}
```

### Example without filtering (`-n` flag):
```json
{
  "ietf-yp-notification:envelope": {
    "event-time": "2025-10-21T10:00:00Z",
    "contents": {
      "ietf-yp-lite:update": {
        "updates": [{
          "data": {
            "ietf-interfaces:interfaces": {
              "ietf-interfaces:interface": [{
                "name": "Eth0",
                "ietf-interfaces:admin-status": "up",
                "ietf-ip:ipv4": {
                  "enabled": true
                }
              }]
            }
          }
        }]
      }
    }
  }
}
```

## Implementation Details

- Written in Scala 3 with indentation-based syntax
- Uses uPickle for JSON serialization
- Case classes model YANG containers and lists
- Augmentations from ietf-ip are included as fields in interface case classes
- Custom JSON building to support module-prefixed keys

## YANG Modules

The tool models the following YANG modules (located in `publisher/resources/`):
- `ietf-interfaces@2018-02-20.yang` (RFC 8343)
- `ietf-ip@2018-02-22.yang` (RFC 8342)
- `ietf-yp-notification@2025-01-27.yang`
- Supporting modules: ietf-inet-types, ietf-yang-types
