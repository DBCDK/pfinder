/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-xsd-maven-plugin
 *
 * opensearch-xsd-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-xsd-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.xsd.codegenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class JavaFileOutputStream extends FileOutputStream {

    public JavaFileOutputStream(Context cxt, String className) throws IOException {
        super(makeFile(cxt, className));
    }

    private static File makeFile(Context cxt, String className) throws IOException {
        Path p = cxt.getTargetFolder().toPath().toAbsolutePath();
        for (String d : cxt.getPackageName().split("\\.")) {
            p = p.resolve(d);
        }
        File dir = p.toFile();
        if (!dir.exists())
            dir.mkdirs();
        File file = p.resolve(className + ".java").toFile();
        return file;
    }

}
