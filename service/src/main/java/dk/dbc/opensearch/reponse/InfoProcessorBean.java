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
package dk.dbc.opensearch.reponse;

import dk.dbc.opensearch.input.InfoRequest;
import dk.dbc.opensearch.output.InfoNameSpaces;
import dk.dbc.opensearch.output.InfoObjectFormats;
import dk.dbc.opensearch.output.InfoRepositories;
import dk.dbc.opensearch.output.InfoResponse;
import dk.dbc.opensearch.output.InfoSearchProfile;
import dk.dbc.opensearch.output.InfoSorts;
import dk.dbc.opensearch.output.Root;
import dk.dbc.opensearch.setup.Settings;
import dk.dbc.opensearch.utils.MDCLog;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.EMPTY_MAP;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class InfoProcessorBean {

    private static final Logger log = LoggerFactory.getLogger(InfoProcessorBean.class);

    @Inject
    Settings settings;

    private InfoRequest request;
    private StatisticsRecorder statistics;
    private MDCLog mdc;

    public Root.Scope<Root.EntryPoint> builder(InfoRequest request, StatisticsRecorder statistics, MDCLog mdc) {
        this.request = request;
        this.statistics = statistics;
        this.mdc = mdc;
        mdc.withAgencyId(request.getAgency())
                .withProfiles(request.getProfilesOrDefault())
                .withTrackingId(request.getTrackingId());
        return (scope) -> {
            scope.infoResponse(this::process);
        };
    }

    private void compute() {
    }

    private void process(InfoResponse response) throws XMLStreamException, IOException {
        try {
            compute();
        } catch (UserMessageException ex) {
            throw new IOException("INTERNAL SERVER ERROR");
        } catch (RuntimeException ex) {
            log.error("Error processing info request: {}", ex.getMessage());
            log.info("{}", request);
            log.debug("Error processing info request: ", ex);
            throw new IOException("INTERNAL SERVER ERROR");
        }
        outputResponse(response);
    }

    private void outputResponse(InfoResponse response) throws IOException, XMLStreamException {
        try (Timing timerOutput = statistics.timer("output")) {
            response.infoGeneral(infoGeneral -> infoGeneral
                    .defaultRepository(settings.getDefaultRepository()))
                    .infoRepositories(this::infoRepositories)
                    .infoObjectFormats(this::infoObjectFormats)
                    .infoSearchProfile(this::infoSearchProfile)
                    .infoSorts(this::infoSorts)
                    .infoNameSpaces(this::infoNameSpaces);
        }
    }

    private  InfoRepositories infoRepositories(InfoRepositories infoRepositories) throws IOException, XMLStreamException {
        return infoRepositories._skipInfoRepository();
    }

    private  InfoObjectFormats infoObjectFormats(InfoObjectFormats infoObjectFormats) throws XMLStreamException, IOException {
        return infoObjectFormats._skipObjectFormat();
    }

    private  InfoSearchProfile infoSearchProfile(InfoSearchProfile infoSearchProfile) throws IOException, XMLStreamException {
        return infoSearchProfile._skipSearchCollection();
    }

    private  InfoSorts infoSorts(InfoSorts infoSorts) throws XMLStreamException, IOException {
        return infoSorts._skipInfoSort();
    }

    private void infoNameSpaces(InfoNameSpaces infoNameSpaces) throws XMLStreamException, IOException {
        List<String> prefixes = settings.getDefaultNamespaces().keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        for (String prefix : prefixes) {
            infoNameSpaces.infoNameSpace(infoNameSpace -> infoNameSpace
                    .prefix(prefix)
                    .uri(settings.getDefaultNamespaces().get(prefix)));
        }
    }

}
