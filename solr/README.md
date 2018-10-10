
# OpenSeacrh-SolR

The module encapsulating all SolR related operations needed by OpenSearch

## FlatQuery

This package `dk.dbc.opensearch.solr.flatquery` is in charge of taking a CQL-Query
and flattening the query in a way that all operators with same precedence are
converted into lists of queries.

i.e. (one AND (two AND three) NOT four AND five) should become
(AND: one, two, three, five NOT: four) since AND/NOT has same precedence

This is basically done by  traversing the CQL tree, whenever a node is encountered
all lower nodes with same precedence or search-terms are collected into a structure,
if a node of different precedence is encountered then said node is treated in same way.

The main purpose for this is to eliminate the number of parenthesis needed for
sending the query to SolR, and simplify the extraction of fields, when searching
nested documents.

