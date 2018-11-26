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

import dk.dbc.opensearch.input.GetObjectRequest;
import dk.dbc.opensearch.output.Root;
import dk.dbc.opensearch.output.SearchResponse;
import dk.dbc.opensearch.setup.Settings;
import dk.dbc.opensearch.utils.MDCLog;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import java.io.IOException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class GetObjectProcessorBean {

    private static final Logger log = LoggerFactory.getLogger(GetObjectProcessorBean.class);

    @Inject
    Settings settings;

    private GetObjectRequest request;
    private StatisticsRecorder statistics;
    private MDCLog mdc;

    public Root.Scope<Root.EntryPoint> builder(GetObjectRequest request, StatisticsRecorder statistics, MDCLog mdc) {
        this.request = request;
        this.statistics = statistics;
        this.mdc = mdc;
        mdc.withAgencyId(request.getAgency())
                .withProfiles(request.getProfilesOrDefault())
                .withTrackingId(request.getTrackingId());
        return (scope) -> {
            scope.searchResponse(this::process);
            statistics.log();
        };
    }

    private void process(SearchResponse response) throws XMLStreamException, IOException {
        try {
            compute();
        } catch (UserMessageException ex) {
            response.error(ex.getUserMessage(settings.getUserMessages()));
            return;
        } catch (RuntimeException ex) {
            log.error("Error processing getObject request: {}", ex.getMessage());
            log.info("{}", request);
            log.debug("Error processing getObject request: ", ex);
            response.error(new UserMessageException(UserMessage.INTERNAL_SERVER_ERROR)
                    .getUserMessage(settings.getUserMessages()));
            return;
        }
        outputResponse(response);
    }

    private void compute() {
        if (request.getProfilesOrDefault().contains("bad")) {
            throw new RuntimeException("Bad profile");
        }
    }

    private void outputResponse(SearchResponse response) throws IOException, XMLStreamException {
        try (Timing timerOutput = statistics.timer("output")) {
            response.error("NOT IMPLEMENTED");
        }
    }

}
