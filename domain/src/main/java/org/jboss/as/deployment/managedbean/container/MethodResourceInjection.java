/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.managedbean.container;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import java.lang.reflect.Method;

/**
 * Resource injection capable of executing the resource injection using a Method instance.
 *
 * @author John E. Bailey
 */
public class MethodResourceInjection<T> extends ResourceInjection<T> {
    private final Value<Method> methodValue;

    /**
     * Construct an instance.
     *
     * @param methodValue The method value to use for injection
     * @param primitive Is the argument type primitive
     */
    public MethodResourceInjection(final Value<Method> methodValue, final boolean primitive) {
        super(primitive);
        this.methodValue = methodValue;
    }

    /** {@inheritDoc} */
    protected Injector<T> getInjector(final Object target) {
        return new SetMethodInjector<T>(Values.immediateValue(target), methodValue);
    }
}
