/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-service
 *
 * opensearch-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.setup.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class ExpandingDeserializer<T> extends StdDeserializer<T> {

    private static final long serialVersionUID = -541297105892977595L;
    private final Expander expander;

    public ExpandingDeserializer(Class<T> vc, Expander expander) {
        super(vc);
        this.expander = expander;
    }

    /**
     * Map string content to a type
     *
     * @param value as a string
     * @return value of type T
     */
    protected abstract T from(String value);

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        JsonToken token = parser.currentToken();
        if (!token.isScalarValue())
            throw new IOException("Cannot map non scalar into " + handledType().getName());
        return from(expandText(parser.getText()));
    }

    /**
     * Parse strings for $-constructions, and substitute them
     *
     * @param s input string
     * @return substituted string
     * @throws IOException in case of memory errors
     */
    private String expandText(String s) throws IOException {
        PushbackReader r = new PushbackReader(new StringReader(s), 1);
        StringWriter w = new StringWriter();
        for (;;) {
            int c = r.read();
            if (c == -1)
                break;
            switch (c) {
                case '$': {
                    String var = readVariableName(r, s);
                    String value = expander.resolveVariable(var);
                    w.write(value);
                    break;
                }
                case '\\':
                    c = r.read();
                    if (c == -1)
                        throw new IOException("Dangling backslash in: `" + s + "'");
                    w.write(c);
                    break;
                default:
                    w.write(c);
                    break;
            }
        }
        return w.getBuffer().toString();
    }

    /**
     * Read a variable name from a PushBackReader
     *
     * @param r reader
     * @param s entire string for errors
     * @return variable name that was optionally surrounded by {}
     * @throws IOException in case of memory errors
     */
    private String readVariableName(PushbackReader r, String s) throws IOException {
        StringWriter w = new StringWriter();
        int c = r.read();
        if (c == -1)
            throw new IOException("Expected variable name in: `" + s + "'");
        if (c == '{') {
            String name = readVariableName(r, s);
            c = r.read();
            if (c == -1)
                throw new IOException("Unexpected EOL in: `" + s + "'");
            if (c != '}')
                throw new IOException("Expected closing bracket in: `" + s + "'");
            return name;
        } else {
            if (!expander.isVariableFirstCharacter(c))
                throw new IOException("Expected variable name in: `" + s + "'");
            w.write(c);
            for (;;) {
                c = r.read();
                if (c == -1) {
                    return w.getBuffer().toString();
                }
                if (expander.isVariableCharacter(c)) {
                    w.write(c);
                } else {
                    r.unread(c);
                    return w.getBuffer().toString();
                }
            }
        }
    }

    public static <T extends ObjectMapper> T objectMapperOf(EnvExpander instance, T mapper) {
        SimpleModule module = new SimpleModule("env-expanding");
        module.addDeserializer(String.class, new StringExpander(instance));
        module.addDeserializer(Boolean.class, new BooleanExpander(instance));
        module.addDeserializer(Integer.class, new IntegerExpander(instance));
        module.addDeserializer(Long.class, new LongExpander(instance));
        module.addDeserializer(Short.class, new ShortExpander(instance));
        module.addDeserializer(Character.class, new CharacterExpander(instance));
        mapper.registerModule(module);
        return mapper;
    }

    public static class StringExpander extends ExpandingDeserializer<String> {

        private static final long serialVersionUID = 7608229941452414834L;

        public StringExpander(Expander expander) {
            super(String.class, expander);
        }

        @Override
        protected String from(String value) {
            return value;
        }
    }

    public static class BooleanExpander extends ExpandingDeserializer<Boolean> {

        private static final long serialVersionUID = -6411302567919567254L;

        public BooleanExpander(Expander expander) {
            super(Boolean.class, expander);
        }

        @Override
        protected Boolean from(String value) {
            if (value == null)
                return null;
            switch (value.toLowerCase(Locale.ROOT)) {
                case "":
                case "no":
                    return false;
                case "yes":
                    return true;
                case "1":
                case "0":
                    return Long.parseLong(value, 10) != 0;
                default:
                    return Boolean.parseBoolean(value);
            }
        }
    }

    public static class IntegerExpander extends ExpandingDeserializer<Integer> {

        private static final long serialVersionUID = -5823936567464625656L;

        public IntegerExpander(Expander expander) {
            super(Integer.class, expander);
        }

        @Override
        protected Integer from(String value) {
            if (value == null || value.isEmpty())
                return null;
            return Integer.parseInt(value, 10);
        }
    }

    public static class LongExpander extends ExpandingDeserializer<Long> {

        private static final long serialVersionUID = -1961545240507956318L;

        public LongExpander(Expander expander) {
            super(Long.class, expander);
        }

        @Override
        protected Long from(String value) {
            if (value == null || value.isEmpty())
                return null;
            return Long.parseLong(value, 10);
        }
    }

    public static class ShortExpander extends ExpandingDeserializer<Short> {

        private static final long serialVersionUID = -6159851476974746062L;

        public ShortExpander(Expander expander) {
            super(Short.class, expander);
        }

        @Override
        protected Short from(String value) {
            if (value == null || value.isEmpty())
                return null;
            return Short.parseShort(value, 10);
        }
    }

    public static class CharacterExpander extends ExpandingDeserializer<Character> {

        private static final long serialVersionUID = -6159851476974746062L;

        public CharacterExpander(Expander expander) {
            super(Character.class, expander);
        }

        @Override
        protected Character from(String value) {
            if (value == null || value.isEmpty())
                return null;
            return Character.valueOf((char) Integer.parseInt(value, 10));
        }
    }
}
