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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.WriteAttributeStepHandler;
import org.jboss.as.clustering.controller.WriteAttributeStepHandlerDescriptor;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for /subsystem=jgroups/stack=X/transport=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class TransportResourceDefinition extends AbstractProtocolResourceDefinition {

    static final PathElement LEGACY_PATH = pathElement("TRANSPORT");
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("transport", name);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        TRANSPORT("org.wildfly.clustering.jgroups.transport"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        @Deprecated SHARED("shared", ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE)
                        .setDeprecated(JGroupsModel.VERSION_4_0_0.getVersion())
                        ;
            }
        },
        SOCKET_BINDING("socket-binding", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(new CapabilityReference(Capability.TRANSPORT, CommonUnaryRequirement.SOCKET_BINDING));
            }
        },
        DIAGNOSTICS_SOCKET_BINDING("diagnostics-socket-binding", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(new CapabilityReference(Capability.TRANSPORT, CommonUnaryRequirement.SOCKET_BINDING));
            }
        },
        SITE("site", ModelType.STRING),
        RACK("rack", ModelType.STRING),
        MACHINE("machine", ModelType.STRING),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    @Deprecated
    enum ThreadingAttribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_EXECUTOR("default-executor"),
        OOB_EXECUTOR("oob-executor"),
        TIMER_EXECUTOR("timer-executor"),
        THREAD_FACTORY("thread-factory"),
        ;
        private final AttributeDefinition definition;

        ThreadingAttribute(String name) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING)
                    .setAllowExpression(false)
                    .setRequired(false)
                    .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    static class WriteThreadingAttributeStepHandlerDescriptor implements WriteAttributeStepHandlerDescriptor {
        @Override
        public Collection<AttributeDefinition> getAttributes() {
            Set<ThreadingAttribute> attributes = EnumSet.allOf(ThreadingAttribute.class);
            List<AttributeDefinition> result = new ArrayList<>(attributes.size());
            for (ThreadingAttribute attribute : attributes) {
                result.add(attribute.getDefinition());
            }
            return result;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .addCapabilities(Capability.class)
                    .addExtraParameters(ThreadingAttribute.class)
                    .addRequiredChildren(ThreadPoolResourceDefinition.class)
                    ;
        }
    }

    TransportResourceDefinition(ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(new Parameters(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), UnaryOperator.identity(), TransportConfigurationServiceConfigurator::new, parentServiceConfiguratorFactory);
    }

    TransportResourceDefinition(String name, ResourceServiceConfiguratorFactory serviceConfiguratorFactory, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(pathElement(name), UnaryOperator.identity(), serviceConfiguratorFactory, parentServiceConfiguratorFactory);
    }

    TransportResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(new Parameters(path, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), configurator, serviceConfiguratorFactory, parentServiceConfiguratorFactory);
    }

    private TransportResourceDefinition(Parameters parameters, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(parameters, new ResourceDescriptorConfigurator(configurator), serviceConfiguratorFactory, parentServiceConfiguratorFactory);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new WriteAttributeStepHandler(new WriteThreadingAttributeStepHandlerDescriptor()) {
            @Override
            protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
                // Add a new step to validate instead of doing it directly in this method.
                // This allows a composite op to change both attributes and then the
                // validation occurs after both have done their work.
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        ModelNode conf = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                        // TODO doesn't cover the admin-only modes
                        if (context.getProcessType().isServer()) {
                            for (ThreadingAttribute attribute : EnumSet.allOf(ThreadingAttribute.class)) {
                                if (conf.hasDefined(attribute.getName())) {
                                    // That is not supported.
                                    throw new OperationFailedException(JGroupsLogger.ROOT_LOGGER.threadsAttributesUsedInRuntime());
                                }
                            }
                        }
                    }
                }, OperationContext.Stage.MODEL);
            }
        }.register(registration);

        if (registration.getPathAddress().getLastElement().isWildcard()) {
            for (ThreadPoolResourceDefinition pool : EnumSet.allOf(ThreadPoolResourceDefinition.class)) {
                pool.register(registration);
            }

            parent.registerAlias(LEGACY_PATH, new AliasEntry(registration) {
                @Override
                public PathAddress convertToTargetAddress(PathAddress aliasAddress, AliasContext aliasContext) {
                    PathAddress target = this.getTargetAddress();
                    List<PathElement> result = new ArrayList<>(aliasAddress.size());
                    for (int i = 0; i < aliasAddress.size(); ++i) {
                        PathElement element = aliasAddress.getElement(i);
                        if (i == target.size() - 1) {
                            final ModelNode operation = aliasContext.getOperation();
                            final String stackName;
                            if (ModelDescriptionConstants.ADD.equals(Operations.getName(operation)) && operation.hasDefined("type")) {
                                stackName = operation.get("type").asString();
                            } else {
                                Resource root = null;
                                try {
                                    root = aliasContext.readResourceFromRoot(PathAddress.pathAddress(result));
                                } catch (Resource.NoSuchResourceException ignored) {
                                }
                                if (root == null) {
                                    stackName = "*";
                                } else {
                                    Set<String> names = root.getChildrenNames("transport");
                                    if (names.size() > 1) {
                                        throw new AssertionError("There should be at most one child");
                                    } else if (names.size() == 0) {
                                        stackName = "*";
                                    } else {
                                        stackName = names.iterator().next();
                                    }
                                }
                            }
                            result.add(PathElement.pathElement("transport", stackName));
                        } else if (i < target.size()) {
                            PathElement targetElement = target.getElement(i);
                            result.add(targetElement.isWildcard() ? PathElement.pathElement(targetElement.getKey(), element.getValue()) : targetElement);
                        } else {
                            result.add(element);
                        }
                    }
                    return PathAddress.pathAddress(result);
                }
            });
        }

        return registration;
    }
}
