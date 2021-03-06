/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-reactive-messaging-smallrye Extension.
 *
 * @author <a href="kkhan@redhat.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "WFLYRXMKAF", length = 4)
public interface MicroProfileReactiveMessagingKafkaLogger extends BasicLogger {

    MicroProfileReactiveMessagingKafkaLogger LOGGER = Logger.getMessageLogger(MicroProfileReactiveMessagingKafkaLogger.class, "org.wildfly.extension.microprofile.reactive.messaging");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Found property %s, will use the Elytron client-ssl-context: %s")
    void foundPropertyUsingElytronClientSSLContext(String prop, String ctx);

    @Message(id = 2, value = "Could not find an Elytron client-ssl-context called: %s")
    IllegalStateException noElytronClientSSLContext(String ctx);

}
