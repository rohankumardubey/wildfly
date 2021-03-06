/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.dmr.ModelNode;

/**
 * Definition of a binary JDBC cache store resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class BinaryKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("binary-keyed-jdbc-store", "BINARY_KEYED_JDBC_STORE");
    static final PathElement PATH = pathElement("binary-jdbc");

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        TABLE("binary-keyed-table", EnumSet.allOf(BinaryTableResourceDefinition.Attribute.class), EnumSet.allOf(TableResourceDefinition.Attribute.class), EnumSet.allOf(TableResourceDefinition.DeprecatedAttribute.class), EnumSet.complementOf(EnumSet.of(TableResourceDefinition.ColumnAttribute.SEGMENT))),
        ;
        private final AttributeDefinition definition;

        @SafeVarargs
        DeprecatedAttribute(String name, Set<? extends org.jboss.as.clustering.controller.Attribute>... attributeSets) {
            int size = 0;
            for (Set<? extends org.jboss.as.clustering.controller.Attribute> attributes : attributeSets) {
                size += attributes.size();
            }
            List<AttributeDefinition> definitions = new ArrayList<>(size);
            for (Set<? extends org.jboss.as.clustering.controller.Attribute> attributes : attributeSets) {
                for (org.jboss.as.clustering.controller.Attribute attribute : attributes) {
                    definitions.add(attribute.getDefinition());
                }
            }
            this.definition = ObjectTypeAttributeDefinition.Builder.of(name, definitions.toArray(new AttributeDefinition[size]))
                    .setRequired(false)
                    .setDeprecated(InfinispanModel.VERSION_4_0_0.getVersion())
                    .setSuffix("table")
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addExtraParameters(DeprecatedAttribute.class)
                    .addRequiredChildren(BinaryTableResourceDefinition.PATH)
                    // Translate deprecated TABLE attribute into separate add table operation
                    .setAddOperationTransformation(new TableAttributeTransformation(DeprecatedAttribute.TABLE, BinaryTableResourceDefinition.PATH))
                    ;
        }
    }

    BinaryKeyedJDBCStoreResourceDefinition() {
        super(PATH, LEGACY_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, JDBCStoreResourceDefinition.PATH, WILDCARD_PATH), new ResourceDescriptorConfigurator());
        this.setDeprecated(InfinispanModel.VERSION_5_0_0.getVersion());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        registration.registerReadWriteAttribute(DeprecatedAttribute.TABLE.getDefinition(), LEGACY_READ_TABLE_HANDLER, LEGACY_WRITE_TABLE_HANDLER);

        new BinaryTableResourceDefinition().register(registration);

        return registration;
    }

    static final OperationStepHandler LEGACY_READ_TABLE_HANDLER = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress().append(BinaryTableResourceDefinition.PATH);
            ModelNode readResourceOperation = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
            operation.get(ModelDescriptionConstants.ATTRIBUTES_ONLY).set(true);
            context.addStep(readResourceOperation, new ReadResourceHandler(), context.getCurrentStage());
        }
    };

    static final OperationStepHandler LEGACY_WRITE_TABLE_HANDLER = new OperationStepHandler() {
        @SuppressWarnings("deprecation")
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress().append(BinaryTableResourceDefinition.PATH);
            ModelNode table = Operations.getAttributeValue(operation);
            for (Class<? extends org.jboss.as.clustering.controller.Attribute> attributeClass : Arrays.asList(BinaryTableResourceDefinition.Attribute.class, TableResourceDefinition.Attribute.class, TableResourceDefinition.DeprecatedAttribute.class)) {
                for (org.jboss.as.clustering.controller.Attribute attribute : attributeClass.getEnumConstants()) {
                    ModelNode writeAttributeOperation = Operations.createWriteAttributeOperation(address, attribute, table.get(attribute.getName()));
                    context.addStep(writeAttributeOperation, context.getResourceRegistration().getAttributeAccess(PathAddress.pathAddress(BinaryTableResourceDefinition.PATH), attribute.getName()).getWriteHandler(), context.getCurrentStage());
                }
            }
        }
    };
}
