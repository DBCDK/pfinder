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

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
class SearchTerm {

    static class Words implements Iterator<Iterator<String>> {

        private final PushbackReader reader;
        private boolean done = false;

        Words(String content) {
            this.reader = new PushbackReader(new StringReader(content));
            try {
                for (;;) {
                    int c = reader.read();
                    if (c == -1) {
                        done = true;
                        break;
                    } else if (!Character.isWhitespace(c)) {
                        reader.unread(c);
                        break;
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public Iterator<String> next() {
            return new Parts(reader) {
                @Override
                boolean isDone(int c) throws IOException {
                    if (c == -1) {
                        done = true; // Done all
                        return true;
                    }
                    if (Character.isWhitespace(c)) {
                        for (;;) {
                            c = reader.read();
                            if (c == -1) {
                                done = true; // Done all
                                return true;
                            }
                            if (!Character.isWhitespace(c)) {
                                reader.unread(c);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            };
        }
    }

    static class Parts implements Iterator<String> {

        protected final PushbackReader reader;
        private boolean done = false;
        private String masking = null;

        private Parts(PushbackReader reader) {
            this.reader = reader;
            try {
                int c = reader.read();
                if (c == -1) {
                    done = true;
                } else {
                    reader.unread(c);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        Parts(String content) {
            this(new PushbackReader(new StringReader(content)));
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public String next() {
            String ret = masking;
            if (masking != null) {
                masking = null;
            } else {
                ret = readString();
            }
            return ret;
        }

        boolean isDone(int c) throws IOException {
            return c == -1;
        }

        private String readString() {
            try (StringWriter writer = new StringWriter()) {
                for (;;) {
                    int c = reader.read();
                    if (isDone(c)) {
                        done = true;
                        break;
                    } else if (c == '\\') {
                        c = reader.read(); // No check - checking is done in tokenizer
                    } else if (c == '?' || c == '*') {
                        try (StringWriter wildcards = new StringWriter()) {
                            wildcards.write(c);
                            for (;;) {
                                c = reader.read();
                                if (c == -1) {
                                    break;
                                } else if (c == '?' || c == '*') {
                                    wildcards.write(c);
                                } else {
                                    reader.unread(c);
                                    break;
                                }
                            }
                            masking = wildcards.getBuffer().toString();
                        }
                        break;
                    }
                    writer.write(c);
                }
                return writer.getBuffer().toString();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
