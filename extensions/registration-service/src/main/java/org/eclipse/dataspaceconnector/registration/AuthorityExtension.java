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

package org.eclipse.dataspaceconnector.registration;

import org.eclipse.dataspaceconnector.extension.jersey.mapper.EdcApiExceptionMapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.registration.api.RegistrationApiController;
import org.eclipse.dataspaceconnector.registration.api.RegistrationService;
import org.eclipse.dataspaceconnector.registration.auth.DidJwtAuthenticationFilter;
import org.eclipse.dataspaceconnector.registration.authority.DummyCredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.authority.spi.CredentialsVerifier;
import org.eclipse.dataspaceconnector.registration.manager.ParticipantManager;
import org.eclipse.dataspaceconnector.registration.store.InMemoryParticipantStore;
import org.eclipse.dataspaceconnector.registration.store.spi.ParticipantStore;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

import static java.lang.String.format;

/**
 * EDC extension to boot the services used by the Authority Service.
 */
public class AuthorityExtension implements ServiceExtension {

    public static final String CONTEXT_ALIAS = "authority";

    @EdcSetting
    private static final String JWT_AUDIENCE_SETTING = "jwt.audience";
    @EdcSetting
    public static final String ERROR_RESPONSE_VERBOSE_SETTING = "edc.error.response.verbose";

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Inject
    private Monitor monitor;

    @Inject
    private WebService webService;

    @Inject
    private ParticipantStore participantStore;

    @Inject
    private CredentialsVerifier credentialsVerifier;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private ParticipantManager participantManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var audience = Objects.requireNonNull(context.getSetting(JWT_AUDIENCE_SETTING, null),
                () -> format("Missing setting %s", JWT_AUDIENCE_SETTING));
        var errorResponseVerbose = context.getSetting(ERROR_RESPONSE_VERBOSE_SETTING, false);
        var authenticationService = new DidJwtAuthenticationFilter(monitor, didPublicKeyResolver, audience);

        participantManager = new ParticipantManager(monitor, participantStore, credentialsVerifier, executorInstrumentation);

        var registrationService = new RegistrationService(monitor, participantStore);
        webService.registerResource(CONTEXT_ALIAS, new RegistrationApiController(registrationService));

        webService.registerResource(CONTEXT_ALIAS, authenticationService);
        webService.registerResource(CONTEXT_ALIAS, new EdcApiExceptionMapper(errorResponseVerbose));
    }

    @Override
    public void start() {
        participantManager.start();
    }

    @Override
    public void shutdown() {
        participantManager.stop();
    }

    @Provider(isDefault = true)
    public ParticipantStore participantStore() {
        return new InMemoryParticipantStore();
    }

    @Provider(isDefault = true)
    public CredentialsVerifier credentialsVerifier() {
        return new DummyCredentialsVerifier();
    }
}
