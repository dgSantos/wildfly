/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.jboss.as.clustering.jgroups.LogFactory;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jgroups.Global;

/**
 * Registers the JGroups subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jgroups";
    public static final String RESOURCE_NAME = JGroupsExtension.class.getPackage().getName() + "." +"LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 3;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    // Temporary workaround for JGRP-1475
    // Configure JGroups to use jboss-logging.
    static {
        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (System.getProperty(Global.CUSTOM_LOG_FACTORY) == null) {
                    System.setProperty(Global.CUSTOM_LOG_FACTORY, LogFactory.class.getName());
                }
                return null;
            }
        };
        AccessController.doPrivileged(action);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
           prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JGroupsExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {

        // IMPORTANT: Management API version != xsd version! Not all Management API changes result in XSD changes
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION, MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        ManagementResourceRegistration subsystem = registration.registerSubsystemModel(new JGroupsSubsystemResourceDefinition(context.isRuntimeOnlyRegistrationValid()));
        subsystem.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, new JGroupsSubsystemDescribeHandler());

        registration.registerXMLElementWriter(new JGroupsSubsystemXMLWriter());

        if (context.isRegisterTransformers()) {
            // Register the model transformers
            registerTransformers(registration);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }

    // Transformation

    /**
     * Register the transformers for older model versions.
     *
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers(final SubsystemRegistration subsystem) {
        registerTransformers_1_1_0(subsystem);
        registerTransformers_1_2_0(subsystem);
    }

    /*
     * Register transformer to transform from current model version to model version 1.1.0
     */
    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1,1,0);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        final ResourceTransformationDescriptionBuilder stackBuilder = subsystemBuilder.addChildResource(StackResourceDefinition.WILDCARD_PATH);

        // reject expressions in certain transport usages
        final ResourceTransformationDescriptionBuilder transportBuilder = stackBuilder.addChildResource(TransportResourceDefinition.PATH);
        transportBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, TransportResourceDefinition.SHARED)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, TransportResourceDefinition.PROPERTIES)
                .end();

        final ResourceTransformationDescriptionBuilder transportPropertyBuilder = transportBuilder.addChildResource(PropertyResourceDefinition.WILDCARD_PATH);
        transportPropertyBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, PropertyResourceDefinition.VALUE)
                .end();

        final ResourceTransformationDescriptionBuilder protocolBuilder = stackBuilder.addChildResource(ProtocolResourceDefinition.WILDCARD_PATH);
        protocolBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ProtocolResourceDefinition.PROPERTIES)
                .end();
        final ResourceTransformationDescriptionBuilder protocolPropertyBuilder = protocolBuilder.addChildResource(PropertyResourceDefinition.WILDCARD_PATH);
        protocolPropertyBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, PropertyResourceDefinition.VALUE)
                .end();

        // reject relay
        stackBuilder.rejectChildResource(RelayResourceDefinition.PATH);

        // now register the completed transform
        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);
    }

    /*
     * Register transformer to transform from current model version to model version 1.2.0
     */
    private static void registerTransformers_1_2_0(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1,2,0);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        final ResourceTransformationDescriptionBuilder stackBuilder = subsystemBuilder.addChildResource(StackResourceDefinition.WILDCARD_PATH);

        // reject relay
        stackBuilder.rejectChildResource(RelayResourceDefinition.PATH);

        // now register the completed transform
        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);
    }
}
