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

import dk.dbc.opensearch.cql.CQLException.Position;
import java.io.Serializable;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Modifier implements Serializable{

    private static final long serialVersionUID = -559329766805007245L;

    private final Position pos;
    private final String name;
    private final String symbol;
    private final String value;
    private final boolean flag;

    public Modifier(Position pos, String name) {
        this(pos, name, null, null, true);
    }

    public Modifier(Position pos, String name, String symbol, String value) {
        this(pos, name, symbol, value, false);
    }

    private Modifier(Position pos, String name, String symbol, String value, boolean flag) {
        this.pos = pos;
        this.name = name;
        this.symbol = symbol;
        this.value = value;
        this.flag = flag;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getValue() {
        return value;
    }

    public boolean isFlag() {
        return flag;
    }

    public Position getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return flag ? "[" + name + "]" : "[" + name + ", " + symbol + ", " + value + "]";
    }

}
