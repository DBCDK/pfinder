# MavenPlugin for generating XMLEvents from XSD

This is a maven plugin that given a number of parameters generates classed,
that when used, can generate output confirming to a XSD.

## Usage

In a `pom.xml`have a block like this:

```
    <plugin>
        <groupId>dk.dbc</groupId>
        <artifactId>opensearch-xsd-maven-plugin</artifactId>
        <version>SOME_VERSION</version>
        <executions>
            <execution>
                <id>some-identifier</id>
                <goals>
                    <goal>xsd-to-source</goal>
                </goals>
                <phase>generate-sources</phase>
                <configuration>
                    <sourceFile>${basedir}/src/main/resources/myservice.xsd</sourceFile>
                    <targetFolder>${project.build.directory}/generated-sources/from-xsd</targetFolder>
                    <package>my.package.name</package>
                    <rootClass>root</rootClass>
                    <skipNamespaces>
                        <skipNamespace>http://www.w3.org/2001/XMLSchema</skipNamespace>
                    </skipNamespaces>
                    <elements>
                        <element>responseType</element>
                    </elements>
                </configuration>
            </execution>
        </executions>
    </plugin>
```

This ensures that a class `my.package.name.Root` has methods to generate `responseType` XML.

The code is put under `generated-sources/from-xsd`, and conforms to `myservice.xsd`

## Design

An entrypoint in `Root` takes a `Function` like type. That is given a scope exposing
all possible outputs from this state. When calling a method that adds content to the
output, a new state is returned exposing all possible content after the method has
generated output.

Methods can take a simple argument, and output that as a simple tag. Or they can
take a `Function` like argument, that exposes the possible values of a nested
structure.

If an output is optional, and not terminal (no elements after this) in the scope,
a `_skip*` method is produced too. this allows for skipping optional elements,
yet holding a strict output order dealing with every tag.

Any is exposed as the method `_any` unless an element only has any as output, then
it is considered a simple method with content. Any structures are read from an
`XMLEventReader`.

There are to `_delegate` methods. Each takes a function that outputs data from
this point in the XML structure, allowing to delegate outputting to a method.
A return value is optional and needs to be a scope. This in conjunction with
`_skip*` can simplify the output of optional that are dependent of a state.

for instance

- foo is optional
- bar is mandatory

```
    state._delegate(s -> { if(x == null)
                               return s._skipFoo();
                           return s.foo(x);
                         })
    .bar(...)
```

All non terminal methos are annotated with `@CheckReturnValue` from
[jsr-305](https://jcp.org/en/jsr/detail?id=305). It is required by the project
to depend on one the implementations, that expose this annotation, since more
than one project does that.
