# AIMS

The purpose of this tool is to be able to generate sample JSON output that conforms to the YANG Push Lite specification.

In particular it should conform the JSON encoding for YANG , RFC 7951, and make use of the new envelope header defined in https://datatracker.ietf.org/doc/html/draft-ietf-netconf-notif-envelope-03.

For sample schema for the data that is returned we make use of the YANG data models for ietf-interfaces.yang and ietf-ip.yang that are defined in RFC 8343, and RFC 8342.

# Design Considerations (phase 1)

The solution should:

- Be written using Scala 3, using the indentation aware format without curly braces:
  - The tool should be runnable as a CLI command with options as to what data to return.
  - The output format of the tool should be well-formatted JSON data.
    - As an example, see examples/full-notification.json.txt
- Define scala case classes that mimic the structure of the ietf-interfaces and ietf-ip YANG modules.
  - These modules and their dependencies are in the publisher/resources folder.
  - Each container or list element should be specified as a Scala case class
  - Child elements of a container or list element are represented as elements of the Scala case class.
  - Build the case classes assuming that all augmentations are in effect, i.e., each augmented YANG module should be an entry in the augmented case class structure to hold the details.
  - Don't use Option for optional fields (since more fields are optional), instead we will use null.
  - Each case class should have a module-name def that defines the module name associated with elements in that case class.
  - YANG Lists should use the Scala Seq type.
- Use the uPickle library to generate automatic convertors to and from JSON.
- The generated JSON should include the module name for each element:
  - This will require a custom ReadWrite to be defined that takes into account the module name when generating the output in the form "<module-name>:<data-node>".
- A subsequent step should filter out redundant module namespaces, as per the rules in RFC 7951:
  - the top level module name should always the module name, if the client element has the same module name then it can be elided, and only needs to be included if the child element has a different module namespace to the parent.
- It should generate reasonable sample data for the data models, perhaps covering 4 Ethernet interfaces.
  - The Ethernet interfaces should be named Eth0 through to Eth3
  - Sample IP data fitting ietf-ip should be included, this should use IPv4 and IPv6 from the documentation address ranges.
  - For other data in the structure they should be modelled as 3 interfaces that are up with traffic flowing and one interface that is admin-down and hence has 0 counters.

# Design Considerations (phase 2)

- Bugfixes:
  - the target-path in the example should start with a '/' to indicate that it starts from the root of the data tree.

## New features:

### optional envelope header
It should be optional as to whether the envelope header is included, or whether the output is just returned from what is contained in contents.

### splitting up the output
For larger messages, it is more efficient to split the message up in various ways:

1. The data for a list element (e.g., the interfaces list) may be split into separate messages.  Add an option to allow the interface data to be split into 2 separate messages.  Also add an option to allow the user to specify how many interfaces worth of output should be generated (defaulting to 4).

1. On a real publisher, different parts of the data may be produced by differnet publishers.  E.g., in the example that we are using, we might have 3 different publishers:
  - a publisher responsible for returning the interface statistics
  - a publisher responsible for returning the IP information
  - a publisher responsible for returning the rest of the interface information.
In all cases, the data must be encoded from the root of the tree, and also include any list keys, otherwise the data is just returned from the given publisher.

