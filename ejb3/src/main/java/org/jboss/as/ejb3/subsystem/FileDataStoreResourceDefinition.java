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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the file data store
 */
public class FileDataStoreResourceDefinition extends SimpleResourceDefinition {
    public static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PATH, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setValidator(new ModelTypeValidator(ModelType.STRING, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.RELATIVE_TO, ModelType.STRING, true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    private final PathManager pathManager;

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { PATH, RELATIVE_TO };
    private static final FileDataStoreAdd ADD_HANDLER = new FileDataStoreAdd(ATTRIBUTES);

    public FileDataStoreResourceDefinition(final PathManager pathManager) {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.FILE_DATA_STORE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.FILE_DATA_STORE))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(new ServiceRemoveStepHandler(TimerPersistence.SERVICE_NAME, ADD_HANDLER))
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setCapabilities(TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY));
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (pathManager != null) {
            final ResolvePathHandler resolvePathHandler = ResolvePathHandler.Builder.of(pathManager)
                    .setPathAttribute(PATH)
                    .setRelativeToAttribute(RELATIVE_TO)
                    .build();
            resourceRegistration.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
        }
    }
}
