# XML / BadgerFish JSON for XSD

This module is using (xsd-maven-plugin) generating output POJOs for OpenSearch
requests.

A handful of input input POJOs are mapped manually (legacy code requires that
the XSD in followed strictly, the orders of parameters are not always in XSD
order), The endpoint is required to map both plain XML and SOAP to requests, and
produce SOAP, XML or JSON output regardless of input type.

## Input Mapping

This is made from the `dk.dbc.opensearch.input.RequestParser` that using an
`InputStream` produces an `XMLStreamException` if there's input problems, like
invalid XML, missing required properties or multiple properties from XSD choices.

## Output Mapping

This is from the `dk.dbc.opensearch.output.Root` class, together with the
optional `dk.dbc.opensearch.output.badgerfish.BadgerFishWriter`.
`Root` takes an `XMLEventWriter` that it outputs data to. `BadgerFishWriter`
implements `XMLEventWriter`, and uses a list of none repeatable tags denominated
by tagname and the enclosing tag by namespace and name.

That is currently manually maintained, could optionally be generated from the XSD,
however given `##any` tags that are not enclosed, fields can be repeated that are
listed as single instance in the XSD. So for the time being manually is the way
to go.

The classes supplied by `Root` represents a tag, and methods on the object
corresponds to the nested tags. They all conform to this interface:

  - A method is visible if its corresponding tag is possible to output from
    this state
  - A method returns the state (visibility) after the corresponding tag is
    outputted
  - A return value from a method is tagged with `@CheckReturnValue`, if there's
    requires values after it.
  - A method takes (according to the tag content type) one of these types
    - A primitive - if a single value is allowed (null is generally not valid)
    - A consumer of an object representing class structure of nested tags - if
      nested tags are expected
    - An `XMLEventReader` - if content of tag is `##any`
  - Is a tag is optional a `_skip{tagname}` is also supplied, that allows for
    making methods that output the value of the field if applicable, but always
    return the next stage in the XML output
  - A `_delegate` method is available in every stage that sends this stage object
    an an argument to a method allowing a method to output the rest of the scope
