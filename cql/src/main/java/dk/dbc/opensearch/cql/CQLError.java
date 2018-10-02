/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-cql
 *
 * opensearch-cql is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-cql is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.cql;

/**
 * From: http://www.loc.gov/standards/sru/diagnostics/diagnosticsList.html
 *
 * @author DBC {@literal <dbc.dk>}
 */
public enum CQLError {

    GENERAL_SYSTEM_ERROR(1, "General system error"),
    SYSTEM_TEMPORARILY_UNAVAILABLE(2, "System temporarily unavailable"),
    AUTHENTICATION_ERROR(3, "Authentication error"),
    UNSUPPORTED_OPERATION(4, "Unsupported operation"),
    UNSUPPORTED_VERSION(5, "Unsupported version"),
    UNSUPPORTED_PARAMETER_VALUE(6, "Unsupported parameter value"),
    MANDATORY_PARAMETER_NOT_SUPPLIED(7, "Mandatory parameter not supplied"),
    UNSUPPORTED_PARAMETER(8, "Unsupported Parameter"),
    QUERY_SYNTAX_ERROR(10, "Query syntax error"),
    TOO_MANY_CHARACTERS_IN_QUERY(12, "Too many characters in query"),
    INVALID_OR_UNSUPPORTED_USE_OF_PARENTHESES(13, "Invalid or unsupported use of parentheses"),
    INVALID_OR_UNSUPPORTED_USE_OF_QUOTES(14, "Invalid or unsupported use of quotes"),
    UNSUPPORTED_CONTEXT_SET(15, "Unsupported context set"),
    UNSUPPORTED_INDEX(16, "Unsupported index"),
    UNSUPPORTED_COMBINATION_OF_INDEXES(18, "Unsupported combination of indexes"),
    UNSUPPORTED_RELATION(19, "Unsupported relation"),
    UNSUPPORTED_RELATION_MODIFIER(20, "Unsupported relation modifier"),
    UNSUPPORTED_COMBINATION_OF_RELATION_MODIFERS(21, "Unsupported combination of relation modifers"),
    UNSUPPORTED_COMBINATION_OF_RELATION_AND_INDEX(22, "Unsupported combination of relation and index"),
    TOO_MANY_CHARACTERS_IN_TERM(23, "Too many characters in term"),
    UNSUPPORTED_COMBINATION_OF_RELATION_AND_TERM(24, "Unsupported combination of relation and term"),
    NON_SPECIAL_CHARACTER_ESCAPED_IN_TERM(26, "Non special character escaped in term"),
    EMPTY_TERM_UNSUPPORTED(27, "Empty term unsupported"),
    MASKING_CHARACTER_NOT_SUPPORTED(28, "Masking character not supported"),
    MASKED_WORDS_TOO_SHORT(29, "Masked words too short"),
    TOO_MANY_MASKING_CHARACTERS_IN_TERM(30, "Too many masking characters in term"),
    ANCHORING_CHARACTER_NOT_SUPPORTED(31, "Anchoring character not supported"),
    ANCHORING_CHARACTER_IN_UNSUPPORTED_POSITION(32, "Anchoring character in unsupported position"),
    COMBINATION_OF_PROXIMITY_ADJACENCY_AND_MASKING_CHARACTERS_NOT_SUPPORTED(33, "Combination of proximity/adjacency and masking characters not supported"),
    COMBINATION_OF_PROXIMITY_ADJACENCY_AND_ANCHORING_CHARACTERS_NOT_SUPPORTED(34, "Combination of proximity/adjacency and anchoring characters not supported"),
    TERM_CONTAINS_ONLY_STOPWORDS(35, "Term contains only stopwords"),
    TERM_IN_INVALID_FORMAT_FOR_INDEX_OR_RELATION(36, "Term in invalid format for index or relation"),
    UNSUPPORTED_BOOLEAN_OPERATOR(37, "Unsupported boolean operator"),
    TOO_MANY_BOOLEAN_OPERATORS_IN_QUERY(38, "Too many boolean operators in query"),
    PROXIMITY_NOT_SUPPORTED(39, "Proximity not supported"),
    UNSUPPORTED_PROXIMITY_RELATION(40, "Unsupported proximity relation"),
    UNSUPPORTED_PROXIMITY_DISTANCE(41, "Unsupported proximity distance"),
    UNSUPPORTED_PROXIMITY_UNIT(42, "Unsupported proximity unit"),
    UNSUPPORTED_PROXIMITY_ORDERING(43, "Unsupported proximity ordering"),
    UNSUPPORTED_COMBINATION_OF_PROXIMITY_MODIFIERS(44, "Unsupported combination of proximity modifiers"),
    UNSUPPORTED_BOOLEAN_MODIFIER(46, "Unsupported boolean modifier"),
    CANNOT_PROCESS_QUERY_REASON_UNKNOWN(47, "Cannot process query, reason unknown"),
    QUERY_FEATURE_UNSUPPORTED(48, "Query feature unsupported"),
    MASKING_CHARACTER_IN_UNSUPPORTED_POSITION(49, "Masking character in unsupported position"),
    RESULT_SETS_NOT_SUPPORTED(50, "Result sets not supported"),
    RESULT_SET_DOES_NOT_EXIST(51, "Result set does not exist"),
    RESULT_SET_TEMPORARILY_UNAVAILABLE(52, "Result set temporarily unavailable"),
    RESULT_SETS_ONLY_SUPPORTED_FOR_RETRIEVAL(53, "Result sets only supported for retrieval"),
    COMBINATION_OF_RESULT_SETS_WITH_SEARCH_TERMS_NOT_SUPPORTED(55, "Combination of result sets with search terms not supported"),
    RESULT_SET_CREATED_WITH_UNPREDICTABLE_PARTIAL_RESULTS_AVAILABLE(58, "Result set created with unpredictable partial results available"),
    RESULT_SET_CREATED_WITH_VALID_PARTIAL_RESULTS_AVAILABLE(59, "Result set created with valid partial results available"),
    RESULT_SET_NOT_CREATED_TOO_MANY_MATCHING_RECORDS(60, "Result set not created: too many matching records"),
    FIRST_RECORD_POSITION_OUT_OF_RANGE(61, "First record position out of range"),
    RECORD_TEMPORARILY_UNAVAILABLE(64, "Record temporarily unavailable"),
    RECORD_DOES_NOT_EXIST(65, "Record does not exist"),
    UNKNOWN_SCHEMA_FOR_RETRIEVAL(66, "Unknown schema for retrieval"),
    RECORD_NOT_AVAILABLE_IN_THIS_SCHEMA(67, "Record not available in this schema"),
    NOT_AUTHORISED_TO_SEND_RECORD(68, "Not authorised to send record"),
    NOT_AUTHORISED_TO_SEND_RECORD_IN_THIS_SCHEMA(69, "Not authorised to send record in this schema"),
    RECORD_TOO_LARGE_TO_SEND(70, "Record too large to send"),
    UNSUPPORTED_RECORD_PACKING(71, "Unsupported record packing"),
    XPATH_RETRIEVAL_UNSUPPORTED(72, "XPath retrieval unsupported"),
    XPATH_EXPRESSION_CONTAINS_UNSUPPORTED_FEATURE(73, "XPath expression contains unsupported feature"),
    UNABLE_TO_EVALUATE_XPATH_EXPRESSION(74, "Unable to evaluate XPath expression"),
    SORT_NOT_SUPPORTED(80, "Sort not supported"),
    UNSUPPORTED_SORT_SEQUENCE(82, "Unsupported sort sequence"),
    TOO_MANY_RECORDS_TO_SORT(83, "Too many records to sort"),
    TOO_MANY_SORT_KEYS_TO_SORT(84, "Too many sort keys to sort"),
    CANNOT_SORT_INCOMPATIBLE_RECORD_FORMATS(86, "Cannot sort: incompatible record formats"),
    UNSUPPORTED_SCHEMA_FOR_SORT(87, "Unsupported schema for sort"),
    UNSUPPORTED_PATH_FOR_SORT(88, "Unsupported path for sort"),
    PATH_UNSUPPORTED_FOR_SCHEMA(89, "Path unsupported for schema"),
    UNSUPPORTED_DIRECTION(90, "Unsupported direction"),
    UNSUPPORTED_CASE(91, "Unsupported case"),
    UNSUPPORTED_MISSING_VALUE_ACTION(92, "Unsupported missing value action"),
    SORT_ENDED_DUE_TO_MISSING_VALUE(93, "Sort ended due to missing value"),
    SORT_SPEC_INCLUDED_BOTH_IN_QUERY_AND_PROTOCOL_QUERY_PREVAILS(94, "Sort spec included both in query and protocol: query prevails"),
    SORT_SPEC_INCLUDED_BOTH_IN_QUERY_AND_PROTOCOL_PROTOCOL_PREVAILS(95, "Sort spec included both in query and protocol: protocol prevails"),
    SORT_SPEC_INCLUDED_BOTH_IN_QUERY_AND_PROTOCOL_ERROR(96, "Sort spec included both in query and protocol: error"),
    DATABASE_DOES_NOT_EXIST(235, "Database does not exist");

    private final int no;
    private final String msg;

    private CQLError(int no, String msg) {
        this.no = no;
        this.msg = msg;
    }

    public int getNo() {
        return no;
    }

    public String getMsg() {
        return msg;
    }
}
