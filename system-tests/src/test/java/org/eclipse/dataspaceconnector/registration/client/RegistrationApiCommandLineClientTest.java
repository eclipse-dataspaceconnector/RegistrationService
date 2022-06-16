/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.registration.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.registration.cli.RegistrationServiceCli;
import org.eclipse.dataspaceconnector.registration.client.models.Participant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.client.IntegrationTestUtils.createParticipant;



@ExtendWith(EdcExtension.class)
public class RegistrationApiCommandLineClientTest {
    static final String API_URL = "http://localhost:8181/api";

    //    ApiClient apiClient = ApiClientFactory.createApiClient(API_URL);
//    RegistryApi api = new RegistryApi(apiClient);
    static final ObjectMapper MAPPER = new ObjectMapper();
    Participant participant = createParticipant();

    @Test
    void listParticipants() throws Exception {
        CommandLine cmd = RegistrationServiceCli.getCommandLine();
        StringWriter writer = new StringWriter();
        cmd.setOut(new PrintWriter(writer));

        var request = MAPPER.writeValueAsString(participant);

        int addCmdExitCode = cmd.execute("participants", "add", "--request=" + request);
        assertThat(addCmdExitCode).isEqualTo(0);

        int listCmdExitCode = cmd.execute("participants", "list");
        assertThat(listCmdExitCode).isEqualTo(0);

        String output = writer.toString();
        var participants = MAPPER.readValue(output, new TypeReference<List<Participant>>() {
        });

        assertThat(participants).contains(participant);
    }
}
