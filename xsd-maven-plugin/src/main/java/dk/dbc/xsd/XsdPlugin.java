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
package dk.dbc.xsd;

import dk.dbc.xsd.codegenerator.Generator;
import java.io.File;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Mojo(threadSafe = true, name = "xsd-to-source", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true)
public class XsdPlugin extends AbstractMojo {

    /**
     * Folder to contain "bin" and "db" directories
     */
    @Parameter(property = "xsd.source.file")
    protected File sourceFile;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/dbc", property = "xsd.target.folder")
    protected File targetFolder;

    @Parameter(alias = "package", required = true, property = "xsd.package")
    protected String packageName;

    @Parameter(alias = "elements", required = true)
    protected List<String> elements;

    @Parameter(defaultValue = "Root", property = "xsd.root.class")
    protected String rootClass;

    @Parameter(alias = "skipNamespaces", defaultValue = "http://www.w3.org/2001/XMLSchema")
    protected List<String> skipNamespaces;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("src: " + sourceFile);
        log.info("dest: " + targetFolder);
        log.info("package: " + packageName);
        log.info("rootClass: " + rootClass);
        log.info("elements: " + elements);
        log.info("skipNamespaces: " + skipNamespaces);

        try {
            Generator generator = new Generator(sourceFile, packageName, elements, targetFolder, rootClass, skipNamespaces);
            generator.run();
            project.addCompileSourceRoot(targetFolder.getAbsolutePath());
        } catch (Exception ex) {
            throw new MojoExecutionException("Error bulding java classes", ex);
        }
    }

}
